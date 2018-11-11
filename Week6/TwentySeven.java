import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 27.1 and 27.2 in "Exercises in Programming Style" by Professor C. Lopes. Second part of the
 * homework of week 6 of UCI CS253 (Fall 2018 edition). In this style, we're to obey a "lazy rivers" programming style.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TwentySeven {


    public static void main(String[] args) throws IOException {
        java8StreamBasedSolution(args[0]);
    }

    /**
     * Solution using Java's built in {@link Stream} API.
     * @param filepath The path to the input file.
     * @throws IOException if any error occurs while accessing the input file or the stop words file.
     */
    public static void java8StreamBasedSolution(String filepath) throws IOException {
        // Load stop words into a set.
        Stream<String> stopWordsStream = Files.lines(Paths.get("../stop_words.txt")).map(line -> line.split(",")).
                flatMap(Arrays::stream); // Flatten from Stream<Sting[]> to Stream<String>
        // Add all one-character "words", then generate set.
        Set<String> stopWords = Stream.concat(stopWordsStream,
                Arrays.stream("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))
        ).collect(Collectors.toCollection(HashSet::new)); // Use HashSet for O(1) complexity contains().
        // Now let's read and process the input file, and produce the output.
        Files.lines(Paths.get(filepath)).
                map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ").split("\\s+")). // Normalize line to lower case and separate into words
                flatMap(Arrays::stream). // Flatten from Stream<Sting[]> to Stream<String>
                filter(word -> !stopWords.contains(word)). // Drop all words that are stop words.
                // TODO need to limit size of stream?
                collect(Collectors.toMap(w-> w, w -> 1, (current, one) -> current + one)). // Note: terminating operation, so no longer a stream. OK?
                entrySet().stream().sorted((e1,e2) -> -e1.getValue().compareTo(e2.getValue())). // Sort in descending order. Note: intermediate stateful operation.
                limit(25).forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue()))); // Print top 25 entries.
    }

}
