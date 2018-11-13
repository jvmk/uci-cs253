import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Option #02 for the implementation of the {@code extract_words(path_to_file)} function.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenWords2 implements Function<String, List<String>> {

    @Override
    public List<String> apply(String filepath) {
        try (BufferedReader stopWordsReader = new BufferedReader(new FileReader("../stop_words.txt"));
             BufferedReader inputReader = new BufferedReader(new FileReader(filepath))) {
            // Read all stop words into set
            Set<String> stopWords = new HashSet<>();
            String line;
            while ((line = stopWordsReader.readLine()) != null) {
                for (String word : line.split(",")) {
                    stopWords.add(word);
                }
            }
            // Add all one-character words to the set of stop words.
            String[] oneCharWords = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(",");
            for (String oneCharWord : oneCharWords) {
                stopWords.add(oneCharWord);
            }
            // Read all words in the input file, and drop all stop words.
            List<String> filteredWords = new ArrayList<>();
            while ((line = inputReader.readLine()) != null) {
                String[] normalizedWordsInLine = line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ").split("\\s+");
                for (String word : normalizedWordsInLine) {
                    if (!stopWords.contains(word)) {
                        filteredWords.add(word);
                    }
                }
            }
            return filteredWords;
        } catch (IOException ioe) {
            // Rethrow wrapped in checked exception.
            throw new RuntimeException(ioe);
        }
    }

}
