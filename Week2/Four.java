import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     Solution for exercise 1 of 2 of week 2 of UCI CS253 (a combination of exercises 4.1 and 4.2 in "Exercises in
 *     Programming Style" by Professor C. Lopes)
 * </p>
 *
 * In this exercise, we're to obey an imperative programming style in which the program functionality is split into
 * procedures that operate via side effects.
 *
 * The procedures may <em>not</em> return anything, but have to communicate results using side effects, hence all
 * methods have a return type of {@code void}. In addition, we are restricted from maintaining global state, so we must
 * resort to passing the same state object as an argument to each procedure.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Four {

    /**
     * Since we're not allowed to maintain global state, yet are still expected to make procedures work together by
     * making use of side effects (specifically, return values are not allowed in this style), we have to resort to
     * defining a wrapper class that can hold the shared state. The procedures will each receive a single (the same!)
     * instance of this class in order to give them access to the shared state.
     */
    private static class SharedState {
        /**
         * Path to the input file.
         */
        String mInputFilePath;
        /**
         * The input data (e.g., pride-and-prejudice.txt).
         */
        Stream<String> mData = Stream.empty();
        /**
         * The words found in {@link #mData} after normalization.
         */
        Stream<String> mWords = Stream.empty();
        /**
         * List of {@link Pair}s that each map a distinct word in {@link #mWords} to its number of occurrences in
         * {@link #mWords}.
         */
        List<Pair<String, Integer>> mWordFreqs = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        SharedState state = new SharedState();
        state.mInputFilePath = args[0];

        readFile(state);
        filterCharsAndNormalize(state);
        scan(state);
        removeStopWords(state);
        frequencies(state);
        sort(state);

        for (int i = 0; i < 25; i++) {
            Pair<String, Integer> p = state.mWordFreqs.get(i);
            System.out.println(String.format("%s  -  %d", p.mItem1, p.mItem2));
        }
    }

    /**
     * Updates {@code state.mData} to point to a the concatenation of its current value and a {@link Stream} containing
     * all the lines in the file at {@code state.mInputFilePath}.
     * @param state The shared state that is to be updated.
     * @throws IOException
     */
    public static void readFile(SharedState state) throws IOException {
        // Note: concat streams instead of simply overwriting mData with the new stream in order to mirror code in book
        // as closely as possible.
        state.mData = Stream.concat(state.mData, Files.lines(Paths.get(state.mInputFilePath)));
    }

    /**
     * Updates {@code state.mData} to point to a {@link Stream} based on the current value of {@code state.mData}, but
     * where all characters of the elements of {@code state.mData} have been normalized to lower case, and where all
     * non-alphanumeric characters have been removed.
     * @param state The shared state that is to be updated.
     */
    public static void filterCharsAndNormalize(SharedState state) {
        // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space and update
        // the shared state to point to the result of the operation.
        state.mData = state.mData.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " "));
    }

    /**
     * Concatenate the results of scanning {@code state.mData} for words to the current value of {@code state.mWords}.
     * @param state The shared state that is to be updated.
     */
    public static void scan(SharedState state) {
        // Separate into separate words by splitting for each whitespace.
        // Use flatMap to flatten Stream<String[]> to Stream<String>.
        // Drop empty strings and one-character "words".
        // Note that like in readFile, we concat the Stream generated from state.mData with the current value of
        // state.mWords in order to mirror the code in the book as closely as possible.
        state.mWords = Stream.concat(state.mWords,
                state.mData.map(line -> line.split("\\s+")).flatMap(Arrays::stream).filter(w -> w.length() > 1));
    }

    /**
     * Updates {@code state.mWords} based on its current value such that it only retains those elements that are not in
     * {@code ../stop_words.txt}.
     * @param state The shared state that is to be updated.
     * @throws IOException
     */
    public static void removeStopWords(SharedState state) throws IOException {
        // Read the contents of the stop words file. Use try-with-resource to ensure resources are freed.
        try(Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
            // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
            Set<String> stopWords = swStream.map(line -> line.split(",")).
                    flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
            // Update state.mWords to result of filtering its current elements.
            state.mWords = state.mWords.filter(word -> !stopWords.contains(word));
        }
    }

    /**
     * For each word {@code w} in {@code state.mWords}, updates the corresponding {@code Pair p} in
     * {@code state.mWordFreqs} (i.e., the {@code Pair p} for which {@code p.mItem1.equals(w)}) by incrementing
     * {@code p.mItem2}.
     *
     * If no corresponding {@code Pair} is found, a new {@code Pair pNew} with values {@code pNew.mItem1 = w} and
     * {@code pNew.mItem2 = 1} is appended to {@code state.mWordFreqs}.
     *
     * @param state The shared state that is to be updated.
     */
    public static void frequencies(SharedState state) {
        // Note that this is a super inefficient implementation (each indexOf call is O(n))!
        // We could reduce this to O(1) by using a HashMap instead of a List for mWordFreqs.
        // However, the code in the book explicitly uses lists for this purpose, most likely
        // because it wants to demonstrate how the list is sorted in place in a subsequent
        // procedure call.
        final List<String> keys = state.mWordFreqs.stream().map(pair -> pair.mItem1).collect(Collectors.toList());
        state.mWords.forEach(word -> {
            int index = keys.indexOf(word);
            if (index > -1) {
                state.mWordFreqs.get(index).mItem2 = state.mWordFreqs.get(index).mItem2 + 1;
            } else {
                state.mWordFreqs.add(new Pair<>(word, 1));
                keys.add(word);
            }
        });
    }

    /**
     * Performs an in-place sort of {@code state.mWordFreqs} by frequency.
     * @param state The shared state that is to be updated.
     */
    public static void sort(SharedState state) {
        Collections.sort(state.mWordFreqs, (p1, p2) -> -p1.mItem2.compareTo(p2.mItem2));
    }

    // Basic helper tuple
    private static class Pair<T1, T2> {
        private T1 mItem1;
        private T2 mItem2;
        Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }
    }
}