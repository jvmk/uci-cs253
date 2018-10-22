import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercise 11.1 in "Exercises in Programming Style" by Professor C. Lopes. First part of the homework of
 * week 4 of UCI CS253 (Fall 2018 edition).
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Eleven {

    public static void main(String[] args) {
        WordFrequencyController wfc = new WordFrequencyController();
        wfc.receive(new String[]{StringConstants.INIT, args[0]}, Void.class);
        wfc.receive(new String[]{StringConstants.RUN}, Void.class);
    }

    /**
     * Interface that classes will implement to signal that they operate in letterbox style.
     */
    interface Letterbox {
        /**
         * Receives a message and invokes the operation with id equal to {@code msg[0]}.
         * <p>
         * This method takes a class literal, which allows the caller to indicate that it expects the result of the
         * invocation of this method to be of a certain type. This allows implementations of this abstract method to
         * cast the return type to the desired type internally (without having to deal with compiler warnings), which
         * results in cleaner client code.
         *
         * @param msg
         *         The message to be delivered in the letterbox.
         * @param expectedReturnType
         *         A class literal that the caller supplies to indicate the expected return type.
         * @param <T>
         *         The type of the result.
         * @return The result of performing the operation identified by {@code msg[0]} cast to the type {@code T}.
         */
        <T> T receive(String[] msg, Class<T> expectedReturnType);
    }

    /**
     * Models the contents of the file.
     */
    static class DataStorageManager implements Letterbox {

        /**
         * Complete contents of file.
         */
        private String mFileContents;

        @Override
        public <T> T receive(String[] msg, Class<T> expectedReturnType) {
            switch (msg[0]) {
                case StringConstants.INIT:
                    return expectedReturnType.cast(init(msg[1]));
                case StringConstants.WORDS:
                    return expectedReturnType.cast(getWords());
                default:
                    throw new IllegalArgumentException(StringConstants.msgNotUnderstood(msg[0]));
            }
        }

        /**
         * Initializes this {@link DataStorageManager}: reads the contents of the file at path {@code filepath} into a
         * local variable. The file contents are normalized to lower case, and all non-alphanumeric characters are
         * replaced by a space.
         *
         * @param filepath
         *         The path to the file that should be used to initialize this {@link DataStorageManager}.
         * @return {@code null}; the method is declared with return type {@link Void} as it is called from {@link
         * #receive(String[], Class)} which must return a value, hence this method cannot be a defined as a {@code void}
         * (no return value) method.
         * @throws IOException
         *         If an exception occurs while accessing the file at {@code filepath}.
         */
        private Void init(String filepath) {
            // Freeing resources: use try-with-resource such that file handles are automatically closed.
            try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
                // Then join strings using the system's line separator in order to preserve line breaks in the single
                // resulting string (Files.lines(Path) throws away line separators when reading the file).
                mFileContents = lines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ioe) {
                // Rethrow as unchecked to simplify calling code.
                throw new RuntimeException(ioe);
            }
            return null;
        }

        /**
         * Returns a processed copy of the local copy of the file contents, namely one in which the string has been
         * split into separate words.
         *
         * @return An array containing the words in storage.
         */
        private String[] getWords() {
            if (mFileContents == null) {
                throw new IllegalStateException(StringConstants.notInitialized());
            }
            return mFileContents.split("\\s+");
        }

    }

    /**
     * Models the stop words filter.
     */
    static class StopWordManager implements Letterbox {

        /**
         * The set of stop words.
         */
        private Set<String> mStopWords;

        @Override
        public <T> T receive(String[] msg, Class<T> expectedReturnType) {
            switch (msg[0]) {
                case StringConstants.INIT:
                    return expectedReturnType.cast(init());
                case StringConstants.IS_STOP_WORD:
                    return expectedReturnType.cast(isStopWord(msg[1]));
                default:
                    throw new IllegalArgumentException(StringConstants.msgNotUnderstood(msg[0]));
            }
        }

        /**
         * Initializes this {@link StopWordManager}: reads and parses the contents of the stop words file, forming a set
         * of stop words.
         *
         * @return {@code null}; the method is declared with return type {@link Void} as it is called from {@link
         * #receive(String[], Class)} which must return a value, hence this method cannot be a defined as a {@code void}
         * (no return value) method.
         */
        private Void init() {
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
                mStopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                        collect(Collectors.toCollection(HashSet::new));
            } catch (IOException ioe) {
                // Rethrow as unchecked to simplify calling code.
                throw new RuntimeException(ioe);
            }
            return null;
        }

        /**
         * Checks if {@code word} is a stop word (all <= 1 character words are also stop words).
         *
         * @param word
         *         The word to be compared against the set of stop words.
         * @return A boxed {@code true} if {@code word} is a stop word, otherwise a boxed {@code false}.
         */
        private Boolean isStopWord(String word) {
            if (mStopWords == null) {
                throw new IllegalStateException(StringConstants.notInitialized());
            }
            return mStopWords.contains(word) || word.length() <= 1;
        }
    }

    /**
     * In charge of keeping track of word frequencies.
     */
    static class WordFrequencyManager implements Letterbox {

        /**
         * Maps a word to its current count (frequency).
         */
        private final Map<String, Integer> mWordFreqs = new HashMap<>();

        @Override
        public <T> T receive(String[] msg, Class<T> expectedReturnType) {
            switch (msg[0]) {
                case StringConstants.INCREMENT_COUNT:
                    return expectedReturnType.cast(incrementCount(msg[1]));
                case StringConstants.SORTED:
                    return expectedReturnType.cast(sorted());
                default:
                    throw new IllegalArgumentException(StringConstants.msgNotUnderstood(msg[0]));
            }
        }

        /**
         * Increments the frequency of {@code word} by one, or sets its frequency to 1 if it is not currently in {@link
         * #mWordFreqs}.
         *
         * @param word
         *         The word that is to have its frequency incremented by 1.
         * @return The updated frequency of {@code word}.
         */
        private Integer incrementCount(String word) {
            return mWordFreqs.merge(word, 1, (current, one) -> current + one);
        }

        private List<Pair<String, Integer>> sorted() {
            return mWordFreqs.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).
                    sorted((p1, p2) -> -p1.second().compareTo(p2.second())).collect(Collectors.toList());
        }

    }

    /**
     * Combines the functionality provided by the other classes (or "things" as in the book's terminology).
     */
    static class WordFrequencyController implements Letterbox {

        private DataStorageManager mDataStorageManager;
        private StopWordManager mStopWordManager;
        private WordFrequencyManager mWordFreqManager;

        @Override
        public <T> T receive(String[] msg, Class<T> expectedReturnType) {
            switch (msg[0]) {
                case StringConstants.INIT:
                    return expectedReturnType.cast(init(msg[1]));
                case StringConstants.RUN:
                    return expectedReturnType.cast(run());
                default:
                    throw new IllegalArgumentException(StringConstants.msgNotUnderstood(msg[0]));
            }
        }

        /**
         * Initializes this {@link WordFrequencyManager}: initializes instances of the other classes that this {@link
         * WordFrequencyManager} uses to provide its functionality.
         *
         * @param filepath
         *         The path to the file for which word frequencies are to be computed
         * @return {@code null}; the method is declared with return type {@link Void} as it is called from {@link
         * #receive(String[], Class)} which must return a value, hence this method cannot be a defined as a {@code void}
         * (no return value) method.
         */
        private Void init(String filepath) {
            mDataStorageManager = new DataStorageManager();
            mStopWordManager = new StopWordManager();
            mWordFreqManager = new WordFrequencyManager();
            mDataStorageManager.receive(new String[]{StringConstants.INIT, filepath}, Void.class);
            mStopWordManager.receive(new String[]{StringConstants.INIT}, Void.class);
            return null;
        }

        /**
         * Performs the word frequency count and prints the to 25 word frequencies to standard output.
         *
         * @return {@code null}; the method is declared with return type {@link Void} as it is called from {@link
         * #receive(String[], Class)} which must return a value, hence this method cannot be a defined as a {@code void}
         * (no return value) method.
         */
        private Void run() {
            for (String word : mDataStorageManager.receive(new String[]{StringConstants.WORDS}, String[].class)) {
                if (!mStopWordManager.receive(new String[]{StringConstants.IS_STOP_WORD, word}, Boolean.class)) {
                    // Although the call returns an Integer, we don't care about its value here.
                    mWordFreqManager.receive(new String[]{StringConstants.INCREMENT_COUNT, word}, Integer.class);
                }
            }
            List<Pair<String, Integer>> wordFreqs =
                    mWordFreqManager.receive(new String[]{StringConstants.SORTED}, List.class);
            for (int i = 0; i < 25; i++) {
                Pair<String, Integer> p = wordFreqs.get(i);
                System.out.println(String.format("%s  -  %d", p.first(), p.second()));
            }
            return null;
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
    static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;

        Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }

        public T1 first() {
            return mItem1;
        }

        public T2 second() {
            return mItem2;
        }
    }

    /**
     * Simple helper class that holds string literals.
     */
    static class StringConstants {
        // Operation identifiers
        public static final String INIT = "init";
        public static final String WORDS = "words";
        public static final String IS_STOP_WORD = "is_stop_word";
        public static final String INCREMENT_COUNT = "increment_count";
        public static final String SORTED = "sorted";
        public static final String RUN = "run";

        // Exception message generators.
        public static String msgNotUnderstood(String msgId) {
            return "Message not understood: " + msgId;
        }

        public static String notInitialized() {
            return "Instance has not been initialized";
        }
    }

}
