import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 12.1, 12.2, and 12.3 in "Exercises in Programming Style" by Professor C. Lopes. Second part of
 * the homework of week 4 of UCI CS253 (Fall 2018 edition). In these exercises, we explore the "Closed Maps" programming
 * style. All functionality (variables as well as methods/functions) is stored as values in maps. As the values are of
 * different types, we must resort to {@code Map<String, Object>} to fit them all in the map. As a result, we loose all
 * static checking of types.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Twelve {

    /**
     * A {@link HashMap} that creates an alias for "this" that is "me".
     * Defined in a base class as we want to use "me" in all three maps, so we might as well just declare the alias
     * once and for all.
     * @param <K> The type of the keys in the map.
     * @param <V> The type of the values in the map.
     */
    static class MeMap<K, V> extends HashMap<K, V> {
        protected final MeMap<K, V> me = this;
    }

    static final Map<String, Object> DATA_STORAGE_OBJ = new MeMap<String, Object>() {
        // Use initializer block to pre-fill the map. Bad practice since it's inserted into the constructor, so we
        // effectively end up calling instance methods before the object has been fully initialized... but oh well...
        {
            // The array of words is empty before init has been called.
            me.put(StringConstants.DATA, new String[0]);
            // Add init function that will initialize the map with the words read from the input file.
            me.put(StringConstants.INIT, (Consumer<String>) (filepath) -> {
                try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                    // Make all lowercase, convert non-alphanumeric chars to spaces, and split into separate words.
                    // Note as we stream on each line of the file, we just first join all lines (using a space as a
                    // delimiter) before we invoke split() on the resulting joined string.
                    String[] words = lines.map(l -> l.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                            collect(Collectors.joining(" ")).split("\\s+");
                    // Replace the empty array with the words array.
                    me.put(StringConstants.DATA, words);
                } catch (IOException ioe) {
                    // Rethrow wrapped in unchecked exception.
                    throw new RuntimeException(ioe);
                }
            });
            // Put a getter function in the map that extracts what is stored under 'data'.
            // Technically the client code may also just access 'data' directly because there is no encapsulation, i.e.,
            // client code has access to all values stored in the map, although we intend for it to make use of the
            // getter defined here.
            me.put(StringConstants.WORDS, (Supplier<String[]>) () -> (String[]) me.get(StringConstants.DATA));
        }
    };

    @SuppressWarnings("unchecked")
    static final Map<String, Object> STOP_WORDS_OBJ = new MeMap<String, Object>() {
        // Initializer block. See caveats listed above (in DATA_STORAGE_OBJ).
        {
            // The set of stop words is empty before init has been called.
            me.put(StringConstants.STOP_WORDS, new HashSet<String>());
            // Add init function that will load the stop words.
            me.put(StringConstants.INIT, (Runnable) () -> {
                try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                    // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
                    Set<String> stopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                            collect(Collectors.toCollection(HashSet::new));
                    me.put(StringConstants.STOP_WORDS, stopWords);
                } catch (IOException ioe) {
                    // Rethrow as unchecked exception
                    throw new RuntimeException(ioe);
                }
            });
            // Add function that determines if a given word is a stop word.
            me.put(StringConstants.IS_STOP_WORD, (Function<String, Boolean>) w -> w.length() <= 1 || ((Set<String>)me.get(StringConstants.STOP_WORDS)).contains(w));
        }
    };

    @SuppressWarnings("unchecked")
    static final Map<String, Object> WORD_FREQS_OBJ = new MeMap<String, Object>() {
        // Initializer block. See caveats listed above (in DATA_STORAGE_OBJ).
        {
            // The (nested) map holding the word frequencies is initially empty.
            me.put(StringConstants.FREQS, new HashMap<String, Integer>());
            // Add function that increments the word count in the nested map for a given word.
            me.put(StringConstants.INCREMENT_COUNT, (Consumer<String>) w -> ((Map<String, Integer>)me.get(StringConstants.FREQS)).merge(w, 1, (old, one) -> old + one));
            // Add function that produces a sorted list of word frequencies (descending order).
            me.put(StringConstants.SORTED, (Supplier<List<Pair<String, Integer>>>) () -> ((Map<String, Integer>)me.get(StringConstants.FREQS)).entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).sorted((p1, p2) -> -p1.second().compareTo(p2.second())).collect(Collectors.toList()));
        }
    };

    /**
     * Basic, immutable helper pair/tuple.
     *
     * @param <T1>
     *         Type of element 1 of the pair.
     * @param <T2>
     *         Type of element 2 of the pair.
     */
    static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;

        Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }

        public T1 first() {
            return mItem1;
        }

        public T2 second() {
            return mItem2;
        }
    }

    /**
     * Simple helper class that holds string literals.
     */
    static class StringConstants {
        public static final String DATA = "data";
        public static final String INIT = "init";
        public static final String WORDS = "words";
        public static final String STOP_WORDS = "stop_words";
        public static final String IS_STOP_WORD = "is_stop_word";
        public static final String FREQS = "freqs";
        public static final String INCREMENT_COUNT = "increment_count";
        public static final String SORTED = "sorted";
        public static final String TOP25 = "top25";
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        // Initialize the two maps that need to load files.
        ((Consumer<String>) DATA_STORAGE_OBJ.get(StringConstants.INIT)).accept(args[0]);
        ((Runnable) STOP_WORDS_OBJ.get(StringConstants.INIT)).run();
        // Get and invoke the function that returns the words in the input file
        // (returned as an array that we then start iterating over).
        for (String word : ((Supplier<String[]>) DATA_STORAGE_OBJ.get(StringConstants.WORDS)).get()) {
            // Get and invoke the function that returns if a word is a stop word, providing it with the current word.
            if (!((Function<String, Boolean>) STOP_WORDS_OBJ.get(StringConstants.IS_STOP_WORD)).apply(word)) {
                // Not a stop word, so get and invoke the function that increments the word frequency.
                ((Consumer<String>) WORD_FREQS_OBJ.get(StringConstants.INCREMENT_COUNT)).accept(word);
            }
        }

        // EXERCISE 12.2: "Dynamically" add new function to WORD_FREQS_OBJ that prints the top25 words.
        // Note: can't use the alias we created for "me" here as we are not working within the class' own scope.
        // As a result, we can only achieve the "me" functionality here by making the function accept an argument that
        // is the object instance on which it is to be invoked, just like how Python implements instance methods by
        // considering the first argument as the object on which the method is to be invoked (hence that argument is
        // named self by convention).
        WORD_FREQS_OBJ.put(StringConstants.TOP25, (Consumer<Map<String, Object>>) (me) -> {
            // Get and invoke the sorted function, then create a view of the 25 first entries.
            List<Pair<String, Integer>> top25 = ((Supplier<List<Pair<String, Integer>>>) me.get(StringConstants.SORTED)).get().subList(0, 25);
            for (Pair<String, Integer> p : top25) {
                System.out.println(String.format("%s  -  %d", p.first(), p.second()));
            }
        });
        // Get and invoke the new function, passing in the object itself as the argument (the "me" parameter).
        ((Consumer<Map<String, Object>>) WORD_FREQS_OBJ.get(StringConstants.TOP25)).accept(WORD_FREQS_OBJ);
    }

}
