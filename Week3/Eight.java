import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     Solution for exercise 2 of 2 of week 3 of UCI CS253 (exercise 8.1 in "Exercises in Programming Style" by
 *     Professor C. Lopes)
 * </p>
 *
 * In this exercise, we're to obey the "Kick Forward" programming style. This style is essentially a variation of the
 * pipeline style in which functions don't return values, but instead take a second argument that is the next function
 * in the pipeline. The function provided as argument is invoked with what would have been the output of the current
 * function if it was to return a value. Note that this style results in some extremely ugly types for the parameters as
 * we're nesting {@link BiConsumer}s in {@link BiConsumer}s. To circumvent this issue, I implemented
 * {@link #simpleType(BiConsumer)} which converts the nested {@code BiConsumer} into its raw type (i.e., it drops all
 * generic type parameters).
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Eight {

    public static void main(String[] args) {
        readFile(args[0], Eight::filterChars);
    }

    /**
     * Given a filepath, reads the textual content of the file into a {@link String} and invokes {@code nextFunction}
     * with that string.
     * Line breaks in the file are preserved (and translated to the system's line separator). Note: if an
     * {@link IOException} is thrown by the read operation, it will be wrapped in a {@link RuntimeException} that is
     * then rethrown to inform the caller of this method of the error. This is to allow the caller to invoke this method
     * from within a lambda's body without being forced to wrap the call in a try-catch block.
     *
     * @param filepath The path to the file to read from.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the contents
     *                     of the file.
     */
    public static void readFile(String filepath, BiConsumer<String, BiConsumer> nextFunction) {
        // Freeing resources: use try-with-resource such that file handles are automatically closed.
        try(Stream<String> lines = Files.lines(Paths.get(filepath))) {
            // Join strings using the system's line separator in order to preserve line breaks in the single resulting
            // string (Files.lines(Path) throws away line separators when reading the file).
            nextFunction.accept(lines.collect(Collectors.joining(System.lineSeparator())), simpleType(Eight::normalize));
        } catch (IOException ioe) {
            // Hide the fact that method is throwing a checked exception by rethrowing it wrapped in an unchecked
            // exception in order to allow this method to be referred to using method references.
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Given a string, produces a copy of where all non-alphanumeric characters have been replaced by a space and
     * invokes {@code nextFunction}, passing in the resulting string as an argument.
     *
     * @param data The string that is to be filtered.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting string.
     */
    public static void filterChars(String data, BiConsumer<String, BiConsumer> nextFunction) {
        // Convert all non-alphanumeric characters (except whitespace) to a space, then invoke 'scan' with the result.
        String alphanumeric = data.replaceAll("[^a-zA-Z\\d\\s]", " ");
        nextFunction.accept(alphanumeric, simpleType(Eight::scan));
    }

    /**
     * Converts a given string to lowercase and invokes {@code nextFunction} with that string.
     *
     * @param data The string that is to be normalized (converted to lowercase).
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting string.
     */
    public static void normalize(String data, BiConsumer<String, BiConsumer> nextFunction) {
        nextFunction.accept(data.toLowerCase(), simpleType(Eight::removeStopWords));
    }

    /**
     * Splits a string into separate words and invokes {@code nextFunction} with the resulting list of words.
     *
     * @param data The string that is to be split into separate words.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting list of words.
     */
    public static void scan(String data, BiConsumer<List<String>, BiConsumer> nextFunction) {
        // Separate into separate words by splitting for each whitespace.
        List<String> words = Arrays.asList(data.split("\\s+"));
        nextFunction.accept(words, simpleType(Eight::frequencies));
    }

    /**
     * Given list of words, produces a new list containing only those words in the original list that do not appear in
     * {@code ../stop_words.txt} and are also more than one character long. The resulting list is passed to
     * {@code nextFunction}.
     *
     * @param words The list of words that is to be filtered.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting list of words.
     */
    public static void removeStopWords(List<String> words, BiConsumer<List<String>, BiConsumer> nextFunction) {
        // Read the contents of the stop words file. Use try-with-resource to ensure resources are freed.
        try(Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
            // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
            Set<String> stopWords = swStream.map(line -> line.split(",")).
                    flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
            // Drop all one character words and stop words.
            words = words.stream().filter(w -> w.length() > 1 && !stopWords.contains(w)).collect(Collectors.toList());
            nextFunction.accept(words, simpleType(Eight::sort));
        } catch (IOException ioe) {
            // Hide the fact that method is throwing a checked exception by rethrowing it wrapped in an unchecked
            // exception in order to allow this method to be referred to using method references.
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Given a list of words, constructs a {@link Map} that associates each distinct word with its frequency in the
     * input list. The resulting map is passed to {@code nextFunction}.
     *
     * @param words The list of words for which word frequencies are desired.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting word frequency map.
     */
    public static void frequencies(List<String> words, BiConsumer<Map<String, Integer>, BiConsumer> nextFunction) {
        final Map<String, Integer> wordFreqs = new HashMap<>();
        words.forEach(word -> wordFreqs.merge(word, 1, (oldVal, newVal) -> oldVal + newVal));
        nextFunction.accept(wordFreqs, simpleType(Eight::printText));
    }

    /**
     * Given a {@link Map} associating words with their frequencies, constructs a list of corresponding
     * {@code Pair<String, Integer>} instances sorted by word frequency (descending order). The resulting list is passed
     * to {@code nextFunction}.
     *
     * @param wordFreqs The word frequency map.
     * @param nextFunction The next function in the pipeline, i.e., the function that is to be invoked with the
     *                     resulting sorted list of pairs of words and their frequencies.
     */
    public static void sort(Map<String, Integer> wordFreqs, BiConsumer<List<Pair<String, Integer>>, Consumer<?>> nextFunction) {
        List<Pair<String, Integer>> sorted = wordFreqs.entrySet().stream().
                sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).
                map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        nextFunction.accept(sorted, Eight::noOp);
    }

    /**
     * Prints the first 25 entries of the sorted word frequencies in the prescribed format.
     * @param wordFreqs The sorted word frequencies.
     * @param terminationFunction The function that is to be invoked after printing the word frequencies and serves to
     *                            conclude the pipeline.
     */
    public static void printText(List<Pair<String, Integer>> wordFreqs, Consumer<?> terminationFunction) {
        wordFreqs.stream().limit(25).forEach(p -> System.out.println(String.format("%s  -  %d", p.mItem1, p.mItem2)));
        terminationFunction.accept(null);
    }

    /**
     * The no-operator function that takes a {@code T} and does nothing.
     * @param t
     * @param <T>
     */
    public static <T> void noOp(T t) {
        return;
    }

    /**
     * Basic, immutable helper pair/tuple.
     *
     * @param <T1> Type of element 1 of the pair.
     * @param <T2> Type of element 2 of the pair.
     */
    private static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;
        Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }
    }

    /**
     * Casts a {@link BiConsumer} with a nested {@code BiConsumer} to a {@code BiConsumer<T, BiConsumer>}, i.e.,
     * converts the nested {@code BiConsumer} to its raw type.
     * @param f A {@code BiConsumer} that takes another {@code BiConsumer} as its second argument.
     * @param <T> The the type of the first argument of the outer {@code BiConsumer}.
     * @param <B> The full type of the nested {@code BiConsumer}.
     * @return A reference to {@code f} of type {@code BiConsumer<T, BiConsumer>}.
     */
    private static <T, B> BiConsumer<T, BiConsumer> simpleType(BiConsumer<T, B> f) {
        return (BiConsumer<T, BiConsumer>) f;
    }

}