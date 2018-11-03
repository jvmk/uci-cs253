import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 14.1 and 14.2 in "Exercises in Programming Style" by Professor C. Lopes. First part of the
 * homework of week 5 of UCI CS253 (Fall 2018 edition). In these exercises, we are to obey to the "Hollywood"
 * programming style. This style is essentially event-driven programming / observer pattern. The static inner classes
 * {@link WordFrequencyFramework}, {@link DataStorage}, {@link StopWordFilter}, and {@link WordFrequencyCounter} are
 * direct Java translations of the book's Python code.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Fourteen {

    public static void main(String[] args) {
        WordFrequencyFramework wordFreqFramework = new WordFrequencyFramework();
        StopWordFilter stopWordFilter = new StopWordFilter(wordFreqFramework);
        DataStorage dataStorage = new DataStorage(wordFreqFramework, stopWordFilter);
        // No need to retain reference here as wordFreqFramework and dataStorage will both obtain a reference to created
        // object once it registers itself as observer of the two, thereby preventing garbage collection of the new
        // WordFrequencyCounter object.
        new WordFrequencyCounter(wordFreqFramework, dataStorage);
        // Exercise 14.2: instantiate the additional observer.
        ZWordsCounter zWordsCounter = new ZWordsCounter(stopWordFilter);
        // Exercise 14.2: register the additional observer as observer of word events.
        dataStorage.registerWordEventHandler(zWordsCounter::onWordEncountered);
        // Exercise 14.2: register the additional observer as observer of termination events.
        wordFreqFramework.registerEndEventHandler(zWordsCounter::printZWords);
        // Fire chain of events.
        wordFreqFramework.run(args[0]);
    }


    public static class WordFrequencyFramework {

        private final List<Consumer<String>> mLoadEventHandlers = new ArrayList<>();
        private final List<Runnable> mDoWorkEventHandlers = new ArrayList<>();
        private final List<Runnable> mEndEventHandlers = new ArrayList<>();

        public void registerLoadEventHandler(Consumer<String> handler) {
            mLoadEventHandlers.add(handler);
        }

        public void registerDoWorkEventHandler(Runnable handler) {
            mDoWorkEventHandlers.add(handler);
        }

        public void registerEndEventHandler(Runnable handler) {
            mEndEventHandlers.add(handler);
        }

        // Alternatively, one could make this class implement Consumer<String> for this purpose.
        public void run(String filepath) {
            mLoadEventHandlers.forEach(h -> h.accept(filepath));
            mDoWorkEventHandlers.forEach(r -> r.run());
            mEndEventHandlers.forEach(r -> r.run());
        }

    }

    /**
     * Models the contents of the file.
     */
    private static class DataStorage {

        private final StopWordFilter mStopWordFilter;
        private String mFileContents;
        private final List<Consumer<String>> mWordEventHandlers = new ArrayList<>();

        private DataStorage(WordFrequencyFramework wff, StopWordFilter stopWordFilter) {
            wff.registerLoadEventHandler(this::load);
            wff.registerDoWorkEventHandler(this::produceWords);
            mStopWordFilter = stopWordFilter;
        }

        private void load(String filepath) {
            try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
                // Then join strings using the system's line separator in order to preserve line breaks in the single
                // resulting string (Files.lines(Path) throws away line separators when reading the file).
                mFileContents = lines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception.
                throw new RuntimeException(ioe);
            }
        }

        private void produceWords() {
            if (mFileContents == null) {
                throw new IllegalStateException("not yet initialized; must load file contents via load(String) first");
            }
            for (String word : mFileContents.split("\\s+")) {
                if (!mStopWordFilter.isStopWord(word)) {
                    mWordEventHandlers.forEach(h -> h.accept(word));
                }
            }
        }

        private void registerWordEventHandler(Consumer<String> handler) {
            mWordEventHandlers.add(handler);
        }

    }

    /**
     * Models the stop word filter.
     */
    private static class StopWordFilter {

        private Set<String> mStopWords;

        private StopWordFilter(WordFrequencyFramework wff) {
            wff.registerLoadEventHandler(this::load);
        }

        @SuppressWarnings("Duplicates") // ignore duplicated in other files in project (solutions to other exercises)
        private void load(String ignoredFilepath) {
            // The stop_words.txt file path is hardcoded, so ignore argument (which is the filepath of the file for
            // which word frequencies are to be computed).
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
                mStopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                        collect(Collectors.toCollection(HashSet::new));
                // Add all 1-character words to stop words set.
                // Source: 'alphabet' courtesy of https://stackoverflow.com/a/17575926/1214974
                char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                for (char c : alphabet) {
                    mStopWords.add(Character.toString(c));
                }
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception
                throw new RuntimeException(ioe);
            }
        }

        private boolean isStopWord(String word) {
            if (mStopWords == null) {
                throw new IllegalStateException("not yet initialized; must load stop words via load(String) first");
            }
            return mStopWords.contains(word);
        }
    }

    /**
     * Manages the word frequency data.
     */
    private static class WordFrequencyCounter {

        private final Map<String, Integer> mWordFreqs = new HashMap<>();

        private WordFrequencyCounter(WordFrequencyFramework wff, DataStorage dataStorage) {
            dataStorage.registerWordEventHandler(this::incrementWordCount);
            wff.registerEndEventHandler(this::printFrequencies);
        }

        private void incrementWordCount(String word) {
            mWordFreqs.merge(word, 1, (currentVal, one) -> currentVal + one);
        }

        private void printFrequencies() {
            mWordFreqs.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).
                    sorted((p1, p2) -> -p1.second().compareTo(p2.second())).limit(25).collect(Collectors.toList()).
                    forEach(p -> System.out.println(String.format("%s  -  %d", p.first(), p.second())));
        }

    }

    /**
     * Provides the functionality required for exercise 14.2.
     */
    private static class ZWordsCounter {
        private final Set<String> mZWords = new HashSet<>();
        private final StopWordFilter mStopWordFilter;

        private ZWordsCounter(StopWordFilter stopWordFilter) {
            mStopWordFilter = stopWordFilter;
        }

        /**
         * Adds the given {@code word} to {@link #mZWords} <em>iff</em> {@code word} is not a stop word and {@code word}
         * contains the letter 'z'.
         *
         * @param word A word (from the input file).
         */
        private void onWordEncountered(String word) {
            if (!mStopWordFilter.isStopWord(word) && word.contains("z")) {
                // Only add word if it is not a stop word and it contains the letter 'z'.
                mZWords.add(word);
            }
        }

        /**
         * Prints the number of unique non stop word words that contains the letter 'z'.
         */
        private void printZWords() {
            // Print the number of unique words containing the letter 'z'.
            // Note: Prof. Lopes said on canvas that it was up to me if I print the number of unique words containing
            // the letter 'z', or the total number of words containing the letter 'z' (counting duplicates). I opted for
            // the former.
            System.out.println(mZWords.size());
        }
    }

    /**
     * Basic, immutable helper pair/tuple.
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
