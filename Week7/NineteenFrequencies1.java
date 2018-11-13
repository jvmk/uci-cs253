import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Option #01 for the implementation of the {@code top25(word_list)} function.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenFrequencies1 implements Function<List<String>, List<Map.Entry<String, Integer>>> {

    @Override
    public List<Map.Entry<String, Integer>> apply(List<String> words) {
        return words.stream().
                collect(Collectors.toMap(w -> w, w -> 1, (current, one) -> current + one)).
                entrySet().stream().sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).limit(25).
                collect(Collectors.toList());
    }

}
