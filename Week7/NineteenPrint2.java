import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Option #02 for the print functionality implementation for exercise 19.4.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class NineteenPrint2 implements Consumer<List<Map.Entry<String, Integer>>> {

    @Override
    public void accept(List<Map.Entry<String, Integer>> entries) {
        for (Map.Entry<String, Integer> e : entries) {
            // Add some sleep time to mimic a slower implementation that provides the same functionality.
            try {
                Thread.sleep(250);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue()));
        }
    }

}
