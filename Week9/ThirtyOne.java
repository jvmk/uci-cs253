import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 31.1 and 31.3 (and 30.3) in "Exercises in Programming Style" by Professor C. Lopes.
 * Second part of the homework of week 9 of UCI CS253 (Fall 2018 edition). In this style, we're to obey a "Double Map
 * Reduce" programming style.
 *
 * <p>
 *     Description of exercise 30.3 from Canvas (differs from book's description):
 *     "EXTRA CREDIT (10 pts): Integrate [exercise] 30.3 (multithreaded map) into your solution to 31. Depending on the
 *     language you are using, you may or may not need to write a concurrent map function; there may already exist one
 *     ready to be used."
 * </p>
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class ThirtyOne {


    public static void main(String[] args) {
        List<List<Pair<String, Integer>>> splits = partition(readFile(args[0]), 200)
                .parallel() // Exercise 30.3: integrate concurrent map
                .map(ThirtyOne::splitWords).collect(Collectors.toList());

        // Note: sequential bottleneck here.
        Map<String, List<Pair<String, Integer>>> letterGroups = regroup(splits);

        List<Pair<String, Integer>> wordFreqs = letterGroups.entrySet().stream()
                .parallel() // Exercise 30.3: integrate concurrent map
                .map(e -> countWords(e.getValue()))
                .collect(ArrayList::new, List::addAll, List::addAll);

        wordFreqs.sort((p1, p2) -> -p1.second().compareTo(p2.second()));
        for (int i = 0; i < 25; i++) {
            Pair<String, Integer> freq = wordFreqs.get(i);
            System.out.println(String.format("%s  -  %d", freq.first(), freq.second()));
        }
    }

    /**
     * Partitions {@code fileContents} into chunks.
     * @param fileContents The contents of the input file.
     * @param chunkLineCount The number of lines in each chunk.
     * @return A stream of chunks.
     */
    public static Stream<String> partition(String fileContents, int chunkLineCount) {
        List<String> backingList = new ArrayList<>();
        int offset = 0;
        String[] lines = fileContents.split(System.lineSeparator());
        while (offset < lines.length) {
            int end = Integer.min(offset + chunkLineCount, lines.length);
            StringBuilder chunk = new StringBuilder();
            for (int i = offset; i < end; i++) {
                // Separate lines in chunk with a space to simplify splitting
                chunk.append(lines[i] + " ");
            }
            backingList.add(chunk.toString());
            offset = end;
        }
        return backingList.stream();
    }

    /**
     * Splits a line (chunk) into words, and drops all stop words.
     * @param line A line (chunk) from the input file.
     * @return The words in the line (chunk).
     */
    private static List<Pair<String, Integer>> splitWords(String line) {
        // This is kinda silly: we read and parse the stop words file once for each chunk, but this is the way the book
        // does it, so better stay in line with that.
        Set<String> stopWords = Arrays.stream(readFile("../stop_words.txt").split(",")).
                collect(Collectors.toCollection(HashSet::new));
        return Arrays.stream(line.replaceAll("[^a-zA-Z\\d\\s]", " ").toLowerCase().split(" ")).
                filter(w -> w.length() >= 2 && !stopWords.contains(w)).
                map(w -> new Pair<>(w, 1)).
                collect(Collectors.toList());
    }

    /**
     * Regroups the words into 5 groups (for exercise 31.3)
     * @param pairsList The words.
     * @return A map where each entry is a word group.
     */
    private static Map<String, List<Pair<String, Integer>>> regroup(List<List<Pair<String, Integer>>> pairsList) {
        // Exercise 31.3 groups
        char g1 = 'a';
        char g2 = 'f';
        char g3 = 'k';
        char g4 = 'p';
        char g5 = 'u';
        Map<String, List<Pair<String, Integer>>> result = new HashMap<>();
        for (List<Pair<String, Integer>> pairs : pairsList) {
            for (Pair<String, Integer> p : pairs) {
                char group;
                char firstLetter = p.first().charAt(0);
                if (firstLetter >= g1 && firstLetter < g2) {
                    group = g1;
                } else if (firstLetter >= g2 && firstLetter < g3) {
                    group = g2;
                } else if (firstLetter >= g3 && firstLetter < g4) {
                    group = g3;
                } else if (firstLetter >= g4 && firstLetter < g5) {
                    group = g4;
                } else {
                    // Everything else (including numeric "words", e.g., "2008") goes in group 5.
                    group = g5;
                }
                result.computeIfAbsent(String.valueOf(group), k -> new ArrayList<>()).add(p);
            }
        }
        return result;
    }

    /**
     * Counts the words in a word group.
     * @param wordGroup The word group.
     * @return A list of pairs where each pair contains is a word and its frequency.
     */
    private static List<Pair<String, Integer>> countWords(List<Pair<String, Integer>> wordGroup) {
        return wordGroup.stream().
                collect(
                        Collectors.toMap(
                                Pair::first,
                                p -> p,
                                (p1, p2) -> new Pair<>(p1.first(), p1.second() + p2.second())
                        )
                ).
                entrySet().stream().
                map(Map.Entry::getValue).
                collect(Collectors.toList());
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

        private T1 first() {
            return mItem1;
        }

        private T2 second() {
            return mItem2;
        }
    }
}


