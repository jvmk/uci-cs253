import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 15.1 and 15.2 in "Exercises in Programming Style" by Professor C. Lopes. Second (optional)
 * part of the homework of week 5 of UCI CS253 (Fall 2018 edition). In this exercise, we're to obey a publish-subscribe
 * style. The static inner classes {@link EventManager}, {@link DataStorage}, {@link StopWordFilter},
 * {@link WordFrequencyCounter}, and {@link WordFrequencyApp} are essentially direct translations of the book's Python
 * code. The functionality required for exercise 15.2 is implemented in {@link ZWordsTracker}.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class Fifteen {

    public static void main(String[] args) {
        EventManager eventManager = new EventManager();
        // No need to retain references here as we will not call these objects directly from here.
        // Garbage collection is prevented as EventManager holds a reference to the objects since the objects register
        // themselves as subscribers of some set of events.
        new DataStorage(eventManager);
        new StopWordFilter(eventManager);
        new WordFrequencyCounter(eventManager);
        new ZWordsTracker(eventManager); // <--- For exercise 15.2.
        new WordFrequencyApp(eventManager);
        // Fire the run event to start the event chain
        eventManager.publish(new Event(EventType.RUN, args[0]));
    }

    /**
     * Basic, immutable helper pair/tuple.
     *
     * @param <T1>
     *         Type of element 1 of the pair.
     * @param <T2>
     *         Type of element 2 of the pair.
     */
    private static class Pair<T1, T2> {
        private final T1 mItem1;
        private final T2 mItem2;

        private Pair(T1 item1, T2 item2) {
            mItem1 = item1;
            mItem2 = item2;
        }

        private T1 first() {
            return mItem1;
        }

        private T2 second() {
            return mItem2;
        }
    }

    /**
     * Models an event.
     */
    private static class Event {

        private final EventType mType;
        private final List<Object> mArgs = new ArrayList<>();

        private Event(EventType eventType, Object... eventArgs) {
            mType = eventType;
            for (Object o : eventArgs) {
                mArgs.add(o);
            }
        }

        private EventType getType() {
            return mType;
        }

        private List<Object> getArgs() {
            return mArgs;
        }

    }

    /**
     * For indicating the type of an {@link Event}.
     */
    private enum EventType {
        LOAD, START, WORD,  RUN, VALID_WORD, EOF, PRINT
    }

    /**
     * Provides the publish/subscribe infrastructure.
     */
    private static class EventManager {

        private final Map<EventType, List<Consumer<Event>>> mSubscriptions = new HashMap<>();

        private void subscribe(EventType eventType, Consumer<Event> consumer) {
            List<Consumer<Event>> consumers = mSubscriptions.getOrDefault(eventType, new ArrayList<>());
            consumers.add(consumer);
            mSubscriptions.put(eventType, consumers);
        }

        private void publish(Event event) {
            List<Consumer<Event>> consumers = mSubscriptions.get(event.getType());
            if (consumers != null) {
                consumers.forEach(c -> c.accept(event));
            }
        }
    }

    /**
     * Models the contents of the file.
     */
    private static class DataStorage {

        private final EventManager mEventMgr;
        private String mFileContents;

        private DataStorage(EventManager eventMgr) {
            mEventMgr = eventMgr;
            mEventMgr.subscribe(EventType.LOAD, this::onLoadEvent);
            mEventMgr.subscribe(EventType.START, this::onStartEvent);
        }

        /**
         * Handler for 'load' events. In the context of this class, 'load' is interpreted as 'load the contents from the
         * file whose path is specified as the first argument of the event'.
         * @param event A 'load' event.
         */
        private void onLoadEvent(Event event) {
            // Assume the filepath is at index 0 of event's args and is a string.
            String filepath = (String) event.getArgs().get(0);
            try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
                // Then join strings using the system's line separator in order to preserve line breaks in the single
                // resulting string (Files.lines(Path) throws away line separators when reading the file).
                mFileContents = lines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception.
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Handler for 'start' events. In the context of this class, 'start' is interpreted as 'separate the file
         * contents into individual words, and fire an event for each such word'.
         * @param event A start event.
         */
        private void onStartEvent(Event event) {
            for (String word : mFileContents.split("\\s+")) {
                mEventMgr.publish(new Event(EventType.WORD, word));
            }
            // Done processing file contents, so fire EOF event.
            mEventMgr.publish(new Event(EventType.EOF));
        }
    }

    /**
     * Models the stop word filter.
     */
    private static class StopWordFilter {

        private final EventManager mEventMgr;
        private Set<String> mStopWords;

        private StopWordFilter(EventManager eventMgr) {
            mEventMgr = eventMgr;
            mEventMgr.subscribe(EventType.LOAD, this::onLoadEvent);
            mEventMgr.subscribe(EventType.WORD, this::onWordEvent);
        }

        /**
         * Handler for 'load' events. In the context of this class, 'load' is interpreted as 'load the stop words from
         * the hardcoded stop words file, ignoring filepath argument of the event'.
         * @param event A 'load' event.
         */
        @SuppressWarnings("Duplicates") // ignore duplicated in other files in project (solutions to other exercises)
        private void onLoadEvent(Event event) {
            // The stop_words.txt file path is hardcoded, so ignore filepath argument embedded in the event (which is
            // the filepath of the file for which word frequencies are to be computed).
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Collect stop words to set. Note: explicitly use HashSet for O(1) complexity contains().
                mStopWords = swStream.map(line -> line.split(",")).flatMap(Arrays::stream).
                        collect(Collectors.toCollection(HashSet::new));
                // Add all 1-character words to stop words set.
                // Source: 'alphabet' courtesy of https://stackoverflow.com/a/17575926/1214974
                char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                for (char c : alphabet) {
                    mStopWords.add(Character.toString(c));
                }
            } catch (IOException ioe) {
                // Rethrow wrapped in unchecked exception
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Handler for 'word' events. In the context of this class, 'word' is interpreted as "inspect the word that is
         * part of the event's arguments, checking if it is a stop word, and if it is not, fire a 'valid word' event".
         * @param event A 'word' event.
         */
        private void onWordEvent(Event event) {
            // Assume that the word is the first argument of the event.
            String word = (String) event.getArgs().get(0);
            if (!mStopWords.contains(word)) {
                // Not a stop word, so should be counted. Fire 'valid word' event in order to trigger word counter.
                mEventMgr.publish(new Event(EventType.VALID_WORD, word));
            }
        }
    }

    /**
     * Keeps track of word frequencies.
     */
    private static class WordFrequencyCounter {

        private final EventManager mEventMgr;
        private final Map<String, Integer> mWordFreqs = new HashMap<>();

        private WordFrequencyCounter(EventManager eventMgr) {
            mEventMgr = eventMgr;
            mEventMgr.subscribe(EventType.VALID_WORD, this::onValidWordEvent);
            mEventMgr.subscribe(EventType.PRINT, this::onPrintEvent);
        }

        /**
         * Handler for 'valid word' events. In the context of this class, 'valid word' is interpreted as "increment the
         * frequency for the word that is at the front of the event's arguments".
         * @param event A 'valid word' event.
         */
        private void onValidWordEvent(Event event) {
            // Assume that the word is at index 0 of the event's arguments.
            String validWord = (String) event.getArgs().get(0);
            // Update count for word.
            mWordFreqs.merge(validWord, 1, (currentVal, one) -> currentVal + one);
        }

        /**
         * Handler for 'print' events. In the context of this class, 'print' is interpreted as "print the words with the
         * top 25 frequencies".
         * @param event A 'print' event.
         */
        private void onPrintEvent(Event event) {
            mWordFreqs.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).
                    sorted((p1, p2) -> -p1.second().compareTo(p2.second())).limit(25).collect(Collectors.toList()).
                    forEach(p -> System.out.println(String.format("%s  -  %d", p.first(), p.second())));
        }
    }

    /**
     * Provides the functionality required for exercise 15.2.
     */
    private static class ZWordsTracker {

        private final EventManager mEventMgr;
        private final Set<String> mZWords = new HashSet<>();

        private ZWordsTracker(EventManager eventMgr) {
            mEventMgr = eventMgr;
            mEventMgr.subscribe(EventType.VALID_WORD, this::onValidWordEvent);
            mEventMgr.subscribe(EventType.PRINT, this::onPrintEvent);
        }

        /**
         * Handler for 'valid word' events. In the context of this class, 'valid word' is interpreted as "if the word
         * contains the letter 'z', make note of it".
         * @param event A 'valid word' event.
         */
        private void onValidWordEvent(Event event) {
            // Assume that the word is at index 0 of the event's arguments.
            String word = (String) event.getArgs().get(0);
            if (word.contains("z")) {
                mZWords.add(word);
            }
        }


        /**
         * Handler for 'print' events. In the context of this class, 'print' is interpreted as "print the words received
         * through 'valid word' events that contain the letter 'z'".
         * @param event A 'print' event.
         */
        private void onPrintEvent(Event event) {
            mZWords.stream().sorted().forEach(s -> System.out.println(s));
        }

    }

    private static class WordFrequencyApp {

        private final EventManager mEventMgr;

        private WordFrequencyApp(EventManager eventMgr) {
            mEventMgr = eventMgr;
            mEventMgr.subscribe(EventType.RUN, this::onRunEvent);
            mEventMgr.subscribe(EventType.EOF, this::onEofEvent);
        }

        /**
         * Handler for 'run' events. In the context of this class, 'run' is interpreted as "publish the load and start
         * events to set off the event chain".
         * @param event A 'run' event.
         */
        private void onRunEvent(Event event) {
            String filepath = (String) event.getArgs().get(0);
            mEventMgr.publish(new Event(EventType.LOAD, filepath));
            mEventMgr.publish(new Event(EventType.START));
        }

        /**
         * Handler for 'EOF' events. In the context of this class, 'EOF' is interpreted as "terminate the event chain
         * by firing the print event that results in the results of the computations being output to standard out".
         * @param event An 'EOF' event.
         */
        private void onEofEvent(Event event) {
            mEventMgr.publish(new Event(EventType.PRINT));
        }
    }

}
