import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 29.1 and 29.2 in "Exercises in Programming Style" by Professor C. Lopes. First part of the
 * homework of week 9 of UCI CS253 (Fall 2018 edition). In this style, we're to obey a "data space" programming style.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TwentyNine {

    /**
     * The number of worker threads.
     */
    private static final int N = 5;

    public static void main(String[] args) throws InterruptedException {
        TwentyNine twentyNine = new TwentyNine();
        // Main thread fills the word space.
        twentyNine.fillWordSpace(args[0]);
        // Worker threads
        BusyLittleBee[] workers = new BusyLittleBee[N];
        // Use a count down latch to set workers off at the same time (simulate real concurrency for our small-scale
        // problem).
        CountDownLatch latch = new CountDownLatch(workers.length);
        // Init worker threads
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new BusyLittleBee(twentyNine::processWords, latch);
        }
        // Start the worker threads.
        for (Thread worker : workers) {
            worker.start();
        }
        // And wait for the worker threads to terminate.
        for (Thread worker : workers) {
            worker.join();
        }
        // Create new workers and new latch for the merging task.
        workers = new BusyLittleBee[N];
        latch = new CountDownLatch(workers.length);
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new BusyLittleBee(twentyNine::mergePartialFreqs, latch);
        }
        // Start the worker threads.
        for (Thread worker : workers) {
            worker.start();
        }
        // And wait for the worker threads to terminate.
        for (Thread worker : workers) {
            worker.join();
        }
        // Main thread prints the output.
        twentyNine.printTop25();
    }


    /**
     * The word data space.
     */
    private final ConcurrentLinkedQueue<String> mWordSpace = new ConcurrentLinkedQueue<>();

    /**
     * The partial frequencies data space.
     */
    private final ConcurrentLinkedQueue<Map<String, Integer>> mFreqSpace = new ConcurrentLinkedQueue();

    /**
     * The set of stop words.
     */
    private final Set<String> mStopWords;

    /**
     * The global word frequencies.
     * TODO: ok to use concurrent map as data space for 29.2?
     */
    private final ConcurrentHashMap<String, LongAdder> mWordFreqs = new ConcurrentHashMap<>();

    public TwentyNine() {
        try {
            Stream<String> stopWordsStream = Files.lines(Paths.get("../stop_words.txt")).map(line -> line.split(",")).
                    flatMap(Arrays::stream); // Flatten from Stream<Sting[]> to Stream<String>
            // Add all one-character "words", then generate set.
            mStopWords = Stream.concat(stopWordsStream,
                    Arrays.stream("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))
            ).collect(Collectors.toCollection(HashSet::new));
        } catch (IOException e) {
            // Rethrow wrapped in unchecked exception (fail early + avoid try-catch in client code)
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads and parses the file at {@code filepath}, adding all words to {@link #mWordSpace}.
     * @param filepath The filepath for the input file.
     */
    private void fillWordSpace(String filepath) {
        try {
            Files.lines(Paths.get(filepath)).
                    map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ").split("\\s+")).
                    flatMap(Arrays::stream).
                    forEach(word -> mWordSpace.add(word));
        } catch (IOException ioe) {
            // Rethrow wrapped in unchecked exception (fail early + avoid try-catch in client code).
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Polls words from {@link #mWordSpace} (until there are no more words available) and keeps count of the words seen.
     * The partial (or potentially global, if single threaded application) word counts are added to {@link #mFreqSpace}.
     */
    private void processWords() {
        Map<String, Integer> freqs = new HashMap<>();
        String word;
        while ((word = mWordSpace.poll()) != null) {
            if (!mStopWords.contains(word)) {
                freqs.merge(word, 1, (current, one) -> current + one);
            }
        }
        mFreqSpace.add(freqs);
    }

    /**
     * Merges {@code partialFreqs} into {@link #mWordFreqs}. Atomicity is handled behind the scenes by the {@link Map}
     * implementation chosen for {@link #mWordFreqs}.
     */
    private void mergePartialFreqs() {
        // The partial word frequencies to be merged into the global word frequencies.
        Map<String, Integer> partialFreqs;
        while ((partialFreqs = mFreqSpace.poll()) != null) {
            // computeIfAbsent uses the provided mapping function to compute and add an entry if no entry is present for
            // the provided key, and subsequently returns that entry. If an entry is already present, it is returned.
            // Hence we can simply call .increment() on the value returned by computeIfAbsent to increment its count.
            // Note: this particular style of stats-keeping is suggested in the docs for ConcurrentHashMap and LongAdder
            partialFreqs.entrySet().stream().
                    forEach(e -> mWordFreqs.computeIfAbsent(e.getKey(), k -> new LongAdder()).add(e.getValue()));
        }
    }

    /**
     * Prints the top 25 entries of {@link #mWordFreqs}. Note that this method assumes that there are no concurrent
     * modifications to {@link #mWordFreqs} during its execution, i.e., that the merging of partial maps has terminated.
     */
    private void printTop25() {
        mWordFreqs.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue().intValue())).
                sorted((p1, p2) -> -p1.second().compareTo(p2.second())).
                limit(25).
                forEach(p -> System.out.println(String.format("%s  -  %d", p.first(), p.second())));
    }

    /**
     * Worker thread with a {@link CountDownLatch} that makes the thread wait until all other workers are ready.
     */
    private static class BusyLittleBee extends Thread {

        /**
         * Use a countdown latch to set off workers at the same instant. Otherwise bootstrapping of later workers be so
         * delayed that they never really get to do any work as our problem size (Pride and Prejudice) is rather small.
         */
        private final CountDownLatch mLatch;

        public BusyLittleBee(Runnable r, CountDownLatch latch) {
            super(r);
            mLatch = latch;
        }

        @Override
        public void run() {
            try {
                // Signal to other waiting threads that this thread is ready.
                mLatch.countDown();
                // Wait for bootstrapping of other workers so all get to do a fair share of the work.
                mLatch.await();
                // Perform the actual work.
                super.run();
            } catch (InterruptedException e) {
                // Rethrow wrapped in unchecked exception (fail early + avoid try-catch in client code)
                throw new RuntimeException(e);
            }
        }

    }


    /**
     * Basic helper pair/tuple.
     *
     * @param <T1>
     *         Type of element 1 of the pair.
     * @param <T2>
     *         Type of element 2 of the pair.
     */
    private static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;

        private Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }

        private T1 first() {
            return mItem1;
        }

        private T2 second() {
            return mItem2;
        }
    }

}
