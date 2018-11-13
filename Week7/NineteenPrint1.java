import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Option #01 for the print functionality implementation for exercise 19.4.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenPrint1 implements Consumer<List<Map.Entry<String, Integer>>> {

    @Override
    public void accept(List<Map.Entry<String, Integer>> entries) {
        entries.stream().forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue())));
    }

}
