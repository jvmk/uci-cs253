import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 * Solution for a customized version of exercise 17.1 in "Exercises in Programming Style" by Professor C. Lopes.
 * First part of the homework of week 7 of UCI CS253 (Fall 2018 edition).
 *
 * The description of the customization to exercise 17.1 is as follows:
 * "17.1, but done in the following way. Let's go back to Week 2. Start with the solution you provided for either 4 or
 * 5, but rewrite the main block/method so that all calls are made using reflection. It's ok if you get rid of the .2
 * constraint, but also ok if you don't."
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Seventeen {

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        // Get the declared methods.
        Method[] methods = Seventeen.class.getDeclaredMethods();
        // Build a map from method name to Method instance.
        Map<String, Method> methodMap = Arrays.stream(methods).collect(Collectors.toMap(m -> m.getName(), m -> m));
        // Fetch readFile method and invoke it on the provided filepath.
        Object fileContents = methodMap.get("readFile").invoke(null, args[0]);
        // Fetch filterCharsAndNormalize method and invoke it on the output of readFile.
        Object normalizedFileContents = methodMap.get("filterCharsAndNormalize").invoke(null, fileContents);
        // Fetch scan method and invoke it on the output of filterCharsAndNormalize.
        Object words = methodMap.get("scan").invoke(null, normalizedFileContents);
        // removeStopWords returns a function that takes a string (the path to the stop words file).
        Object filteredWordsFunc = methodMap.get("removeStopWords").invoke(null, words);
        // Use reflection to get the apply method of the Function instance returned by removeStopWords.
        Method filteredWordsFuncApply = Arrays.stream(filteredWordsFunc.getClass().getMethods()).
                filter(m -> m.getName().equals("apply")).findFirst().get();
        // Invoke apply method, specifying the second command line argument as the path to the stop words file.
        Object filteredWords = filteredWordsFuncApply.invoke(filteredWordsFunc, args[1]);
        // Fetch frequencies method and invoke it on the output of the apply call.
        Object wordFreqs = methodMap.get("frequencies").invoke(null, filteredWords);
        // Fetch the sort method and invoke it on the output of the frequencies call.
        Object sortedWordFreqs = methodMap.get("sort").invoke(null, wordFreqs);
        // Get the sublist method so that we can avoid printing everything.
        Method subListMethod = Arrays.stream(sortedWordFreqs.getClass().getMethods()).
                filter(m -> m.getName().equals("subList")).findFirst().get();
        // Invoke printAll on the top25 entries.
        methodMap.get("printAll").invoke(null, subListMethod.invoke(sortedWordFreqs, 0, 25));
    }

    /**
     * Given a filepath, reads the textual content of the file and returns it as a string. Line breaks in the file are
     * preserved (and translated to the system's line separator) in the output string. Note: if an {@link IOException}
     * is thrown by the read operation, it will be wrapped in a {@link RuntimeException} that is then rethrown to inform
     * the caller of this method of the error. This is to allow the caller to invoke this method from within a lambda's
     * body without being forced to wrap the call in a try-catch block.
     *
     * @param filepath The path to the file to read from.
     * @return The textual content of the file identified by {@code filepath}.
     */
    public static String readFile(String filepath) {
        // Freeing resources: use try-with-resource such that file handles are automatically closed.
        try(Stream<String> lines = Files.lines(Paths.get(filepath))) {
            // Join strings using the system's line separator in order to preserve line breaks in the single resulting
            // string (Files.lines(Path) throws away line separators when reading the file).
            return lines.collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException ioe) {
            // Hide the fact that method is throwing a checked exception by rethrowing it wrapped in an unchecked
            // exception in order to allow this method to be called from within a lambda's body.
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Takes a string and returns a lower case copy where all non-alphanumeric characters have been removed.
     *
     * @param data The string that is to be normalized.
     * @return A lower case copy of {@code data} where all non-alphanumeric characters have been removed.
     */
    public static String filterCharsAndNormalize(String data) {
        // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
        return data.replaceAll("[^a-zA-Z\\d\\s]", " ").toLowerCase();
    }

    /**
     * Given a string, return a list of words in that string.
     * @param data The string that is to be split into separate words.
     * @return A list of words in {@code data}.
     */
    public static List<String> scan(String data) {
        // Separate into separate words by splitting for each whitespace.
        return Arrays.asList(data.split("\\s+"));
    }


    /**
     * Given a list of words, returns a function that, when invoked with a path to a stop words file, will return a new
     * list of words based on the input, but with all stop words and one-character words removed.
     *
     * @param words A list of words.
     * @return A function that, when invoked with a path to a stop words file, will return a new list of words based on
     *         the input, but with all stop words and one-character words removed.
     */
    public static Function<String, List<String>> removeStopWords(List<String> words) {
        // Make use of "Currying": Return a function that takes a path to the stop words file and returns a copy
        // of the list of words provided as argument to this outer function with all stop words and one-character words
        // removed.
        return stopWordsFilepath -> {
            // Read stop words into a set. Explicitly collect to HashSet to get O(1) contains.
            Set<String> stopWords = Arrays.stream(readFile(stopWordsFilepath).split(",")).
                    collect(Collectors.toCollection(HashSet::new));
            // Return a copy of the input with all stop words and one-character words removed.
            return words.stream().filter(w -> !stopWords.contains(w) && w.length() > 1).collect(Collectors.toList());
        };
    }

    /**
     * Given a list of words, constructs a {@link Map} that associates each distinct word with its frequency in the
     * input list.
     *
     * @param words A list of words.
     * @return A {@link Map} that associates each distinct word with its frequency in the input list.
     */
    public static Map<String, Integer> frequencies(List<String> words) {
        final Map<String, Integer> wordFreqs = new HashMap<>();
        words.forEach(word -> wordFreqs.merge(word, 1, (oldVal, newVal) -> oldVal + newVal));
        return wordFreqs;
    }

    /**
     * Given a {@link Map} associating words with their frequencies (as returned by {@link #frequencies(List)}),
     * constructs a list of corresponding {@code Pair<String, Integer>} instances sorted by word frequency (descending
     * order).
     *
     * @param wordFreqs A {@link Map} associating words with their frequencies.
     * @return A list of corresponding {@code Pair<String, Integer>} instances sorted by word frequency (descending
     *         order).
     */
    public static List<Pair<String, Integer>> sort(Map<String, Integer> wordFreqs) {
        return wordFreqs.entrySet().stream().sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).
                map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    /**
     * Takes a list of pairs of words and their associated frequencies and prints them recursively. While it'd be more
     * efficient (and memory friendly) to avoid the recursion here, we do so in order to mirror the example code in the
     * book.
     *
     * @param wordFreqs The word frequencies that are to be printed.
     */
    public static void printAll(List<Pair<String, Integer>> wordFreqs) {
        if (wordFreqs.size() > 0) {
            Pair<String, Integer> head = wordFreqs.get(0);
            System.out.println(String.format("%s  -  %d", head.mItem1, head.mItem2));
            // Proceed to the remainder of the list if there's an element at index 1, otherwise supply the termination
            // condition argument (the empty list) to the recursive call.
            printAll(wordFreqs.size() > 1 ? wordFreqs.subList(1, wordFreqs.size()) : new ArrayList<>());
        }
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


}
