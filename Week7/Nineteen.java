import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Solution for exercises 19.1 and 19.4 in "Exercises in Programming Style" by Professor C. Lopes. Second part of the
 * homework of week 7 of UCI CS253 (Fall 2018 edition). In these exercises, we're to obey a "plugins" style of
 * programming in which most of the functionality is loaded dynamically.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Nineteen {

    /**
     * The function that extracts words. To be loaded dynamically.
     */
    private static Function<String, List<String>> EXTRACT_WORDS_FUNC;

    /**
     * The function that counts the top 25 word frequencies. To be loaded dynamically.
     */
    private static Function<List<String>, List<Map.Entry<String, Integer>>> FREQUENCIES_FUNC;

    /**
     * For exercise 19.4: Dynamically loaded print function.
     */
    private static Consumer<List<Map.Entry<String, Integer>>> PRINT_FUNC;

    public static void main(String[] args)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        loadPlugins();
        PRINT_FUNC.accept(FREQUENCIES_FUNC.apply(EXTRACT_WORDS_FUNC.apply(args[0])));
    }

    /**
     * Dynamically loads the "plugins".
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void loadPlugins() throws ClassNotFoundException, IOException, IllegalAccessException, InstantiationException {
        // Load the configuration file
        Properties config = new Properties();
        config.load(new BufferedReader(new FileReader("./config.properties")));
        // Read the directory from which to read code dynamically (plugins) and convert it to a URL
        URL pluginsDirUrl = Paths.get(config.getProperty("pluginsDir")).toUri().toURL();
        ClassLoader classLoader = new URLClassLoader(new URL[] { pluginsDirUrl }, Nineteen.class.getClassLoader());
        // Read the names of classes to be dynamically loaded.
        String wordsProviderClassName = config.getProperty("words_provider");
        String frequenciesProviderClassName = config.getProperty("frequencies_provider");
        // Drop ".class" from class names if present.
        wordsProviderClassName = removeDotClass(wordsProviderClassName);
        frequenciesProviderClassName = removeDotClass(frequenciesProviderClassName);
        // Load the classes dynamically.
        Class<?> wordsProviderClass = classLoader.loadClass(wordsProviderClassName);
        Class<?> frequenciesProviderClass = classLoader.loadClass(frequenciesProviderClassName);
        // Create instances; casting types to expected base types
        Function<String, List<String>> wordsProvider =
                (Function<String, List<String>>) wordsProviderClass.newInstance();
        Function<List<String>, List<Map.Entry<String, Integer>>> frequenciesProvider =
                (Function<List<String>, List<Map.Entry<String, Integer>>>) frequenciesProviderClass.newInstance();
        EXTRACT_WORDS_FUNC = wordsProvider;
        FREQUENCIES_FUNC = frequenciesProvider;

        // EXERCISE 19.4
        String printerClassName = config.getProperty("printer");
        printerClassName = removeDotClass(printerClassName);
        Class<?> printerClass = classLoader.loadClass(printerClassName);
        Consumer<List<Map.Entry<String, Integer>>> printer =
                (Consumer<List<Map.Entry<String, Integer>>>) printerClass.newInstance();
        PRINT_FUNC = printer;
    }


    /**
     * Produces a string that is identical to the input string, except that ".class" has been removed from the end of
     * the input string.
     * @param s The string for which ".class" is to be removed from the end of the string.
     * @return A string that is identical to {@code s}, except that ".class" has been removed from the end of the
     *         string. If {@code s} does not end with ".class", {@code s} itself is returned.
     */
    private static String removeDotClass(String s) {
        final String clazz = ".class";
        if (s.endsWith(clazz)) {
            return s.substring(0, s.length() - clazz.length());
        }
        return s;
    }
}
