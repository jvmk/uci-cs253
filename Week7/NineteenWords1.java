import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Option #01 for the implementation of the {@code extract_words(path_to_file)} function.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenWords1 implements Function<String, List<String>> {

    @SuppressWarnings("Duplicates") // ignore duplicated in other files in project (solutions to other exercises)
    @Override
    public List<String> apply(String filepath) {
        // Load stop words and input file
        try (Stream<String> stopWordsFile = Files.lines(Paths.get("../stop_words.txt"));
             Stream<String> inputFile = Files.lines(Paths.get(filepath))) {
            // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
            Set<String> stopWords = stopWordsFile.map(line -> line.split(",")).
                    flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
            // Add all one-character words to set of stop words.
            List<String> oneCharWords = Arrays.stream("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(",")).
                    collect(Collectors.toList());
            stopWords.addAll(oneCharWords);
            // First normalize input file's lines to lower case and separate into words
            return inputFile.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ").split("\\s+")).
                    flatMap(Arrays::stream). // Flatten from Stream<Sting[]> to Stream<String>
                    filter(word -> !stopWords.contains(word)). // Drop all words that are stop words.
                    collect(Collectors.toList());
        } catch (IOException ioe) {
            // Rethrow wrapped in unchecked exception
            throw new RuntimeException(ioe);
        }
    }

}
