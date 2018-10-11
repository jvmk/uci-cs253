/**
 * TODO add class documentation.
 *
 * @author Janus Varmarken
 */
public class Five {


    /* ORIGINAL BODY OF Four.java

    public static void main(String[] args) throws IOException {
        // Use try-with-resource to make sure that the file handles underlying the Streams are closed at end of block.
        try(Stream<String> linesStream = readFile(args[0]);
            Stream<String> stopWordsStream = readFile("../stop_words.txt")) {
            Stream<String> lines = filterCharsAndNormalize(linesStream);
            Stream<String> words = scan(lines);
            words = removeStopWords(words, stopWordsStream);
            Map<String, Integer> freqs = frequencies(words);
            Stream<Pair<String, Integer>> sorted = sort(freqs);
            sorted.limit(25).forEach(p -> System.out.println(String.format("%s  -  %d", p.mItem1, p.mItem2)));
        }
    }

    public static Stream<String> readFile(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath));
    }

    public static Stream<String> filterCharsAndNormalize(Stream<String> lineStream) {
        // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
        return lineStream.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " "));
    }

    public static Stream<String> scan(Stream<String> lineStream) {
        // Separate into separate words by splitting for each whitespace.
        // Use flatMap to flatten Stream<String[]> to Stream<String>.
        // Drop empty strings and one-character "words"
        return lineStream.map(line -> line.split("\\s+")).flatMap(Arrays::stream).filter(w -> w.length() > 1);
    }

    public static Stream<String> removeStopWords(Stream<String> wordStream, Stream<String> stopWordsStream) {
        // Collect stopwords to set. Note: explicitly use HashSet for O(1) complexity contains().
        Set<String> stopWords = stopWordsStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                collect(Collectors.toCollection(HashSet::new));
        // Drop all words in stop_words.txt.
        return wordStream.filter(word -> !stopWords.contains(word));
    }

    public static Map<String, Integer> frequencies(Stream<String> wordStream) {
        // Count the number of occurrences by filling in a map.
        Map<String, Integer> wordCounts = new HashMap<>();
        // Note: the merge call associates the entry with a value of 1 if the key is not found; otherwise the
        // current value is incremented by 1 (as newVal assumes a value of 1).
        wordStream.forEach(word -> wordCounts.merge(word, 1, (oldVal, newVal) -> oldVal + newVal));
        return wordCounts;
    }

    public static Stream<Pair<String, Integer>> sort(Map<String, Integer> wordCounts) {
        return wordCounts.entrySet().stream().sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).
                map(entry -> new Pair<>(entry.getKey(), entry.getValue()));
    }

    // Basic helper tuple
    private static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;
        Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }
    }

    */
}
