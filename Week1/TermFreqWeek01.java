import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;


// NOTE - Professor's ruleset used for splitting:
// 1 normalize to lowercase
// 2 replace all non-alphanumeric characters by a space
// 3 split by space
// 4 drop all 1-character words

/**
 * Solution for week 1 programming assignment of UCI CS253 (Fall 2018).
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TermFreqWeek01 {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            String usageHint = String.format("usage: java %s some_text_file", TermFreqWeek01.class.getSimpleName());
            System.out.println(usageHint);
            return;
        }
        final String txtFilePath = args[0];
        // Read the stop_words.txt file that should reside in the parent dir.
        // The file is a csv file containing words that should be dropped when computing the term frequencies.
        List<String> stopWordsLines = Files.readAllLines(Paths.get("../stop_words.txt"));
        // Store stop words as a Set in order to enable easy exclusion using filters in later code.
        final Set<String> stopWords = new HashSet<>();
        for (String line : stopWordsLines) {
            for (String word : line.split(",")) {
                if (word.length() > 0) {
                    stopWords.add(word);
                }
            }
        }
        // Maps a word to its number of occurrences in the input file.
        final Map<String, Integer> wordCounts = new HashMap<>();
        // Note: use try-with-resource such that the file is automatically closed.
        try (Stream<String> txtFileLines = Files.lines(Paths.get(txtFilePath))) {
            // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
            txtFileLines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                    // Separate into separete words; use flatMap to flatten Stream<String[]> to Stream<String>
                    map(line -> line.split("\\s+")).flatMap(Arrays::stream).
                    // Drop all 1-character words and empty strings.
                    filter(word -> word.length() > 1).
                    // Drop all words in stop_words.txt, then count the number of occurrences by filling the map.
                    // The merge call associates the entry with a value of 1 if the key is not found; otherwise the
                    // current value is incremented by 1 (as newVal assumes a value of 1).
                    filter(word -> !stopWords.contains(word)).
                    forEach(word -> wordCounts.merge(word, 1, (oldVal, newVal) -> oldVal + newVal));
        }
        // Sort map entry set by value (number of occurrences of the word that is the key) and print the result.
        // Note that the result of the built-in Integer comparator is negated so as to sort the values in descending order.
        // We also limit the output to the top-25 words.
        wordCounts.entrySet().stream().
                sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).
                limit(25).
                forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue())));
    }
}