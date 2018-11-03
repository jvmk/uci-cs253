import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 24.1 and 24.3 in "Exercises in Programming Style" by Professor C. Lopes. Third part of the
 * homework of week 5 of UCI CS253 (Fall 2018 edition). In this exercise, we're to obey the "Quarantine" style. In this
 * style, core program functions must be pure, and hence all IO actions must be contained in computation sequences that
 * are clearly separated from the pure functions. In order to achieve this, we wrap IO sequences in functions that can
 * be lazily evaluated so that we can defer execution until necessary.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TwentyFour {

    public static void main(String[] args) {
        // Java 8 already has method references and functional interfaces, which provides exactly the functionality
        // implemented by the book's TFQuarantine class, so we can simply skip that class.
        Function<String, Supplier<List<String>>> extractWords = TwentyFour::extractWords;
        Function<String, Runnable> callChain = extractWords.
                andThen(TwentyFour::removeStopWords).
                andThen(TwentyFour::getFrequencies).
                andThen(TwentyFour::sort).
                andThen(TwentyFour::top25Frequencies);
        // Skip implementing an analog of the book's 'get_input' function as one cannot get a hold of the standard
        // command line arguments outside of the main function in Java (can only use System.getProperty(...) to get
        // -Dkeyword=value style arguments, but these are system properties and hence strictly not arguments). Professor
        // Lopes said that it was OK to skip this function. So we simply execute the call chain from here, passing in
        // the filepath argument directly. Note that the run() call is *not* analogous to the book's .execute() call
        // (in fact, the analog is the apply call); the run call simply executes the final result of the call chain,
        // i.e., a Runnable that prints the top25 frequencies to std.out.
        callChain.apply(args[0]).run();
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

    /**
     * Given a path to a file, return a function that reads the file and returns its contents as a list of words.
     * @param filepath A path to a file.
     * @return A function that reads the file and returns its contents as a list of words.
     */
    private static Supplier<List<String>> extractWords(String filepath) {
        return () -> {
            try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                // Make all lowercase, convert non-alphanumeric chars to spaces, and split into separate words.
                // Note as we stream on each line of the file, we just first join all lines (using a space as a
                // delimiter) before we invoke split() on the resulting joined string.
                String[] words = lines.map(l -> l.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(" ")).split("\\s+");
                return Arrays.asList(words);
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception.
                throw new RuntimeException(ioe);
            }
        };
    }

    /**
     * Given a function that, when invoked, returns a list of words, return a function that filters the results of the
     * invocation of the function provided as argument, removing all stop words.
     * @param wordsSupplier A function that, when invoked, returns a list of words.
     * @return A function that filters the results of the invocation of {@code wordsSupplier}, removing all stop words.
     */
    private static Supplier<List<String>> removeStopWords(Supplier<List<String>> wordsSupplier) {
        return () -> {
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
                Set<String> stopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                        collect(Collectors.toCollection(HashSet::new));
                // Add all 1-character words to stop words set.
                // Source: 'alphabet' courtesy of https://stackoverflow.com/a/17575926/1214974
                char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                for (char c : alphabet) {
                    stopWords.add(Character.toString(c));
                }
                return wordsSupplier.get().stream().filter(w -> !stopWords.contains(w)).collect(Collectors.toList());
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception
                throw new RuntimeException(ioe);
            }
        };

    }

    /**
     * Given a function that returns a list of words, return the word frequencies for the output of that function.
     * @param filteredWordsSupplier A function that returns a list of words.
     * @return The word frequencies for the output of {@code filteredWordsSupplier}.
     */
    private static Map<String, Integer> getFrequencies(Supplier<List<String>> filteredWordsSupplier) {
        Map<String, Integer> wordFreqs = new HashMap<>();
        filteredWordsSupplier.get().forEach(w -> wordFreqs.merge(w, 1, (currentVal, one) -> currentVal + one));
        return wordFreqs;
    }

    /**
     * Sorts a map of word frequencies by frequency, descending order.
     * @param wordFreqs A map of word frequencies.
     * @return A list of word frequencies, sorted by descending frequency, descending order.
     */
    private static List<Pair<String, Integer>> sort(Map<String, Integer> wordFreqs) {
        return wordFreqs.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).
                sorted((p1, p2) -> -p1.second().compareTo(p2.second())).collect(Collectors.toList());
    }

    /**
     * Return a function that prints the 25 first entries in {@code sortedFreqs}.
     * @param sortedFreqs A sorted list of word frequencies.
     * @return A function that prints the 25 first entries in {@code sortedFreqs}.
     */
    private static Runnable top25Frequencies(List<Pair<String, Integer>> sortedFreqs) {
        // Exercise 24.3: return function that prints frequencies to the screen (instead of accumulating all in a
        // string), one pair at a time.
        return () -> {
            for(Pair<String, Integer> p : sortedFreqs.subList(0, 25)) {
                System.out.println(String.format("%s  -  %d", p.first(), p.second()));
            }
        };
    }
}
