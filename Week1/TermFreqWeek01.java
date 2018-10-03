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
import java.util.stream.Collectors;

/**
 * <p>Solution for week 1 programming assignment of UCI CS253 (Fall 2018).</p>
 * <p>
 * <b>NOTE:</b> Professor Lopes' ruleset used for splitting:
 * <ol>
 * <li>normalize to lowercase</li>
 * <li>replace all non-alphanumeric characters by a space</li>
 * <li>split by space</li>
 * <li>drop all words that are less than two characters long</li>
 * </ol>
 * </p>
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
        // Read the stop_words.txt file that should reside in the parent dir, and read the input file.
        // The stop_words.txt file is a csv file containing words that should be dropped when computing the term frequencies.
        // Note: use try-with-resource such that the files are automatically closed.
        try (Stream<String> stopWordsStream = Files.lines(Paths.get("../stop_words.txt")).map(line -> line.split(",")).flatMap(Arrays::stream);
             Stream<String> inputFileLines = Files.lines(Paths.get(args[0]))) {
            // Maps a word to its number of occurrences in the input file.
            final Map<String, Integer> wordCounts = new HashMap<>();
            // Turn the stopwords stream into a hash set for easy filtering of the input file's content later on.
            // Note: explicitly collect to HashSet to get the benefit of O(1)-complexity contains()!
            final Set<String> stopWords = stopWordsStream.collect(Collectors.toCollection(HashSet::new));
            inputFileLines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")). // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
                    map(line -> line.split("\\s+")).flatMap(Arrays::stream). // Separate into separete words; use flatMap to flatten Stream<String[]> to Stream<String>.
                    filter(word -> word.length() > 1). // Drop all 1-character words and empty strings.
                    filter(word -> !stopWords.contains(word)). // Drop all words in stop_words.txt.
                    forEach(word -> wordCounts.merge(word, 1, (oldVal, newVal) -> oldVal + newVal)); // Count the number of occurrences by filling in the map.
            // Note: the merge call above associates the entry with a value of 1 if the key is not found; otherwise the
            // current value is incremented by 1 (as newVal assumes a value of 1).

            // Sort map entry set by value (number of occurrences of the word that is the key) and print the result.
            // Note that the result of the built-in Integer comparator is negated so as to sort the values in descending order.
            // We also limit the output to the top-25 words.
            wordCounts.entrySet().stream().sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).limit(25).
                    forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue())));
        }
    }
}