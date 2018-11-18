import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercise 18.1 in "Exercises in Programming Style" by Professor C. Lopes. Third part of the homework of
 * week 7 of UCI CS253 (Fall 2018 edition). In this exercise, we're to obey an "Aspects" programming style.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Eighteen {

    public Eighteen() {
        // Constructor in the "original" code.
        // Only included here for illustrative purposes (mimic how the code would look initially, i.e., before aspects).
        // Code wishing to augment the original code should use the proxy.
    }

    @SuppressWarnings("Duplicates") // ignore duplicated in other files in project (solutions to other exercises)
    protected List<String> extractWords(String filepath) {
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

    protected Map<String, Integer> frequencies(List<String> words) {
        return words.stream().collect(Collectors.toMap(w -> w, w -> 1, (current, one) -> current + one));
    }

    protected List<Map.Entry<String, Integer>> sort(Map<String, Integer> wordFreqs) {
        return wordFreqs.entrySet().stream().
                sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).
                collect(Collectors.toList());
    }

    private static BiFunction<Object, Object[], Object> profile(final Method m) {
        return (instance, args) -> {
            try {
                long startTime = System.nanoTime();
                // Invoke method provided as argument.
                Object methodReturnValue = m.invoke(instance, args);
                long elapsed = System.nanoTime() - startTime;
                System.out.println(
                        String.format("%s.%s(...) took %d nanoseconds",
                                m.getDeclaringClass().getSimpleName(), m.getName(), elapsed)
                );
                return methodReturnValue;
            } catch (ReflectiveOperationException roe) {
                roe.printStackTrace();
                // Rethrow as unchecked to make caller aware that we failed.
                throw new RuntimeException(roe);
            }
        };
    }

    /**
     * Since Java does not allow for runtime modifications to the symbol table, we must resort to a proxy design pattern
     * in order to achieve the goals of the exercise.
     */
    public static class EighteenProxy extends Eighteen {

        /**
         * Factory for creating {@code Eighteen} proxies.
         * Clients wishing to profile {@code Eighteen} code should instantiate proxies using this factory.
         * The factory returns a proxy that forwards calls to a backing {@code Eighteen} instance.
         * The proxy's interface is the same as {@code Eighteen}'s interface as it is a subclass of {@code Eighteen}.
         * @param trackedMethods Methods of {@code Eighteen} which are to be tracked. Set to {@code null} or an empty
         *                       array if you do not wish to track any methods.
         * @return A proxy that forwards all calls to a new {@code Eighteen} instance.
         */
        public static Eighteen newEighteenProxy(Method[] trackedMethods) {
            trackedMethods = trackedMethods == null ? new Method[0] : trackedMethods;
            return new EighteenProxy(new Eighteen(), trackedMethods);
        }

        private final Eighteen mProxied;
        private final Method[] mTrackedMethods;

        private EighteenProxy(Eighteen proxied, Method[] trackedMethods) {
            mProxied = proxied;
            mTrackedMethods = trackedMethods;
        }

        /**
         * Locate the computation that is to be executed. If there is a tracked version of the invoked method present,
         * then that version will be returned as the invocation target. If not, the returned invocation target will
         * simply be the standard version of the method as defined in {@code Eighteen}.
         * @param methodOfCurrentClass The method that is being invoked on the proxy instance.
         * @return The computation that is determined to be the one to execute.
         */
        private BiFunction<Object, Object[], Object> findInvocationTarget(Method methodOfCurrentClass) {
            Optional<Method> trackedCounterpart = getTrackedCounterpart(methodOfCurrentClass);
            if (trackedCounterpart.isPresent()) {
                return Eighteen.profile(trackedCounterpart.get());
            } else {
                return (instance, args) -> {
                    try {
                        Method eighteenMethod = Eighteen.class.getDeclaredMethod(methodOfCurrentClass.getName(),
                                methodOfCurrentClass.getParameterTypes());
                        return eighteenMethod.invoke(instance, args);
                    } catch (ReflectiveOperationException roe) {
                        // rethrow as unchecked.
                        throw new RuntimeException(roe);
                    }
                };
            }
        }

        @Override
        protected List<String> extractWords(String filepath) {
            // Hack to get enclosing method courtesy of https://stackoverflow.com/a/5891326/1214974
            Method thisMethod = new Object(){}.getClass().getEnclosingMethod();
            return (List<String>) findInvocationTarget(thisMethod).apply(mProxied, new Object[] { filepath });
        }

        @Override
        protected Map<String, Integer> frequencies(List<String> words) {
            // Hack to get enclosing method courtesy of https://stackoverflow.com/a/5891326/1214974
            Method thisMethod = new Object(){}.getClass().getEnclosingMethod();
            return (Map<String, Integer>) findInvocationTarget(thisMethod).apply(mProxied, new Object[] { words });
        }

        @Override
        protected List<Map.Entry<String, Integer>> sort(Map<String, Integer> wordFreqs) {
            // Hack to get enclosing method courtesy of https://stackoverflow.com/a/5891326/1214974
            Method thisMethod = new Object(){}.getClass().getEnclosingMethod();
            return (List<Map.Entry<String, Integer>>)
                    findInvocationTarget(thisMethod).apply(mProxied, new Object[] { wordFreqs });
        }

        private Optional<Method> getTrackedCounterpart(Method method) {
            return Arrays.stream(mTrackedMethods).
                    filter(m -> m.getName().equals(method.getName()) &&
                            Arrays.equals(m.getParameterTypes(), method.getParameterTypes()))
                    .findFirst();
        }

    }

    public static void main(String[] args) throws NoSuchMethodException {
        Method[] trackedMethods = new Method[] {
                Eighteen.class.getDeclaredMethod("extractWords", String.class),
                Eighteen.class.getDeclaredMethod("frequencies", List.class),
                Eighteen.class.getDeclaredMethod("sort", Map.class)
        };
        // Cannot overwrite symbol table, so must resort to proxy pattern.
        Eighteen eighteen = EighteenProxy.newEighteenProxy(trackedMethods);
        List<Map.Entry<String, Integer>> wordFreqs =
                eighteen.sort(eighteen.frequencies(eighteen.extractWords(args[0])));
        wordFreqs.stream().limit(25).
                forEach(wf -> System.out.println(String.format("%s  -  %d", wf.getKey(), wf.getValue())));
    }

}
