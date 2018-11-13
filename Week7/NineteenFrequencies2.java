import java.util.*;
import java.util.function.Function;

/**
 * Option #02 for the implementation of the {@code top25(word_list)} function.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenFrequencies2 implements Function<List<String>, List<Map.Entry<String, Integer>>> {

    @Override
    public List<Map.Entry<String, Integer>> apply(List<String> words) {
        Map<String, Integer> wordFreqs = new HashMap<>();
        for (String word : words) {
            wordFreqs.merge(word, 1, (current, one) -> current + one);
        }
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        result.addAll(wordFreqs.entrySet());
        Collections.sort(result, (e1, e2) -> -e1.getValue().compareTo(e2.getValue()));
        return result.subList(0, 25);
    }

}
