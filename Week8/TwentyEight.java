import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercises 28.1 and 28.2 in "Exercises in Programming Style" by Professor C. Lopes. First part of the
 * homework of week 8 of UCI CS253 (Fall 2018 edition). In this style, we're to obey an "actors" programming style.
 * This style is basically concurrent message-passing.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
public class TwentyEight {

    public static void main(String[] args) throws InterruptedException {
        WordExtractor wordExtractor = new WordExtractor();
        WordFilter wordFilter = new WordFilter();
        WordFrequencyTracker wordFreqTracker = new WordFrequencyTracker();
        Actor[] allActors = new Actor[] { wordExtractor, wordFilter, wordFreqTracker };
        // Start the threads.
        for (Actor a : allActors) {
            a.start();
        }
        // Send the init message to all actors.
        wordExtractor.acceptMessage(createInitMessage(args[0]));
        wordFilter.acceptMessage(createInitMessage());
        wordFreqTracker.acceptMessage(createInitMessage());
        // Register for outbound messages.
        wordExtractor.acceptMessage(createRegistrationMessage(wordFilter));
        wordFilter.acceptMessage(createRegistrationMessage(wordFreqTracker));
        // Fire the processing chain by sending the process words message.
        wordExtractor.acceptMessage(new StringMessage(StringConstants.PROCESS_WORDS));
        // Wait for actors to terminate
        for (Actor a : allActors) {
            a.join();
        }
    }

    /**
     * Factory for creating an {@code ActorMessage} with operation ID = {@link StringConstants#REGISTER_FOR_UPDATES},
     * i.e., a message indicating that the {@code Actor} contained in the message's arguments wishes to receive messages
     * sent by the receiver of the {@code ActorMessage}.
     * @param a The {@code Actor} that wishes to register as a receiver of messages from some other {@code Actor} that
     *          the returned {@code ActorMessage} is sent to.
     * @return An {@code ActorMessage} that indicates to the receiver that the {@code Actor} contained in the
     *         {@code ActorMessage}'s arguments wishes to register for outbound messages of the {@code Actor} that
     *         is the receiver of the {@code ActorMessage}.
     */
    private static ActorMessage createRegistrationMessage(Actor a) {
        return new ActorMessage(StringConstants.REGISTER_FOR_UPDATES, a);
    }

    /**
     * Factory for creating {@code StringMessage}s with operation ID = {@link StringConstants#INIT}.
     * @param args The args to be bundled in the returned message.
     * @return A {@code StringMessage} that indicates to the receiver that it should initialize itself.
     */
    private static StringMessage createInitMessage(String... args) {
        return new StringMessage(StringConstants.INIT, args);
    }

    /**
     * Base class for messages. Made abstract in order to encourage subclassing. Subclasses should concretize the
     * generic type parameter. This will allow code that handles message reception to perform checked casts in contrast
     * to resorting to unchecked casts in case {@code Message} was to be used directly.
     *
     * @param <T> The type of the arguments carried in this message.
     */
    abstract private static class Message<T> {

        private final String mOperationId;
        private final T[] mArgs;

        private Message(String operationId, T... args) {
            mOperationId = operationId;
            mArgs = args;
        }

        /**
         * Get the operation identifier.
         * @return The operation identifier.
         */
        public String getOperationId() {
            return mOperationId;
        }

        /**
         * Get the arguments bundled with this message.
         * @return The arguments bundled with this message.
         */
        public T[] getArgs() {
            return mArgs;
        }

    }

    /**
     * Convenience subclass of {@link Message} for messages that carry {@link String}s as arguments.
     */
    private static class StringMessage extends Message<String> {

        private StringMessage(String operationId, String... args) {
            super(operationId, args);
        }

    }

    /**
     * Convenience subclass of {@link Message} for messages that carry {@link Actor}s as arguments.
     */
    private static class ActorMessage extends Message<Actor> {

        private ActorMessage(String operationId, Actor... args) {
            super(operationId, args);
        }

    }

    /**
     * Base class for classes that are 'actors'.
     */
    abstract private static class Actor extends Thread {

        /**
         * Termination flag.
         */
        private boolean mStop = false;

        /**
         * This {@code Actor}'s message queue.
         */
        private final LinkedBlockingQueue<Message<?>> mMsgQueue = new LinkedBlockingQueue<>();

        /**
         * Recipients registered for receiving messages sent by this {@code Actor}.
         */
        protected final List<Actor> mRecipients = new ArrayList<>();

        @Override
        public void run() {
            while (!mStop) {
                Message<?> msg;
                try {
                    // Fetch next message, or block if queue is empty.
                    msg = mMsgQueue.take();
                } catch (InterruptedException e) {
                    // Just fail immediately if we get interrupted.
                    throw new RuntimeException(e);
                }
                // Handle messages common to all actors directly in the base class (termination and registration).
                if (msg.getOperationId().equals(StringConstants.TERMINATE)) {
                    mStop = true;
                    // Forward termination message in order to kill the entire actor chain that follows this actor.
                    forwardMessage(msg);
                } else if (msg.getOperationId().equals(StringConstants.REGISTER_FOR_UPDATES) &&
                        msg instanceof ActorMessage) {
                    for (Actor a : ((ActorMessage)msg).getArgs()) {
                        mRecipients.add(a);
                    }
                } else {
                    // Dispatch subclass specific messages to the subclass.
                    dispatch(msg);
                }
            }
        }

        /**
         * Places a message in this {@code Actor}'s message queue.
         * @param msg The message to be placed in the queue.
         */
        protected void acceptMessage(Message<?> msg) {
            mMsgQueue.add(msg);
        }

        /**
         * Forwards a message to all {@code Actor}s in {@link #mRecipients}.
         * @param msg The message that is to be forwarded.
         */
        protected void forwardMessage(Message<?> msg) {
            for (Actor a : mRecipients) {
                a.acceptMessage(msg);
            }
        }

        abstract protected void dispatch(Message<?> msg);

    }

    /**
     * Actor that receives a filepath and extracts the words from the file at that path.
     */
    private static class WordExtractor extends Actor {

        private String mFileContents;

        @Override
        protected void dispatch(Message<?> msg) {
            if (msg.getOperationId().equals(StringConstants.INIT) && msg instanceof StringMessage) {
                this.init((StringMessage) msg);
            } else if (msg.getOperationId().equals(StringConstants.PROCESS_WORDS)) {
                // We don't care about the message type in this case as we do not expect
                // the message to carry any arguments.
                this.processWords();
            } else {
                // Message not understood, forward it.
                forwardMessage(msg);
            }
        }

        /**
         * Invoked upon reception of an init message. Loads the contents of the file at the path specified by the
         * first argument in {@code msg} into a member field.
         * @param msg The init message.
         */
        private void init(StringMessage msg) {
            String filepath = msg.getArgs()[0];
            try (Stream<String> lines = Files.lines(Paths.get(filepath))) {
                // Normalize to lower case and convert all non-alphanumeric characters (except whitespace) to a space.
                // Then join strings using the system's line separator in order to preserve line breaks in the single
                // resulting string (Files.lines(Path) throws away line separators when reading the file).
                mFileContents = lines.map(line -> line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ")).
                        collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ioe) {
                // Rethrow as unchecked (fail early).
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Invoked upon reception of a "process words" message.
         */
        private void processWords() {
            if (mFileContents == null) {
                // Do nothing if init has not yet been called.
                return;
            }
            String[] words = mFileContents.split("\\s+");
            for (String w : words) {
                // Inform all registered actors of the new word.
                for (Actor a : mRecipients) {
                    a.acceptMessage(new StringMessage(StringConstants.UNFILTERED_WORD, w));
                }
            }
            // Trigger printing by sending the print message. Note that the print message will be forwarded down the
            // Actor chain until it reaches the WordFrequencyTracker.
            mRecipients.stream().forEach(r -> r.acceptMessage(new StringMessage(StringConstants.PRINT_TOP_25)));
            // Kill self and in turn (due to forwarding) all subsequent actors by sending a termination message to this.
            this.acceptMessage(new StringMessage(StringConstants.TERMINATE));
        }
    }

    /**
     * Actor that filters words based on a stop words filter.
     */
    private static class WordFilter extends Actor {

        private Set<String> mStopWords;

        @Override
        protected void dispatch(Message<?> msg) {
            if (msg.getOperationId().equals(StringConstants.INIT)) {
                // We don't care about the message type as we do not make use of the message's arguments.
                init();
            } else if (msg.getOperationId().equals(StringConstants.UNFILTERED_WORD) && msg instanceof StringMessage) {
                filterWord((StringMessage) msg);
            } else {
                // Message not understood, forward it.
                forwardMessage(msg);
            }
        }

        private void init() {
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Add single-char words to stop words stream, and collect stop words stream to set.
                // Note: explicitly use HashSet for O(1) complexity contains().
                mStopWords = Stream.concat(swStream.map(line -> line.split(",")).flatMap(Arrays::stream),
                        Arrays.stream("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))).
                        collect(Collectors.toCollection(HashSet::new));
            } catch (IOException ioe) {
                // Rethrow as unchecked (fail early).
                throw new RuntimeException(ioe);
            }
        }

        private void filterWord(StringMessage msg) {
            for (String w : msg.getArgs()) {
                if (!mStopWords.contains(w)) {
                    for (Actor a : mRecipients) {
                        a.acceptMessage(new StringMessage(StringConstants.FILTERED_WORD, w));
                    }
                }
            }
        }

    }

    /**
     * Actor that tracks word counts and provides capabilities to output word counts.
     */
    private static class WordFrequencyTracker extends Actor {

        private Map<String, Integer> mWordFreqs;

        @Override
        protected void dispatch(Message<?> msg) {
            if (msg.getOperationId().equals(StringConstants.INIT) ||
                    msg.getOperationId().equals(StringConstants.RESET_COUNT)) {
                // Init and reset mean the same to this Actor. We only include the reset for completeness. It may be
                // used by client code to reset the count, e.g., if the client code wishes to reuse the same instance
                // for tracking word counts for another file.
                // We don't care about the type of message as we do not make use of its arguments.
                mWordFreqs = new HashMap<>();
            } else if (msg.getOperationId().equals(StringConstants.FILTERED_WORD) && msg instanceof StringMessage) {
                incrementWordCount((StringMessage) msg);
            } else if (msg.getOperationId().equals(StringConstants.PRINT_TOP_25)) {
                // We don't care about the type of message as we do not make use of its arguments.
                printTop25();
            } else {
                // Message not understood, forward it.
                forwardMessage(msg);
            }
        }

        private void incrementWordCount(StringMessage msg) {
            for (String w : msg.getArgs()) {
                mWordFreqs.merge(w, 1, (current, one) -> current + one);
            }
        }

        private void printTop25() {
            mWordFreqs.entrySet().stream().sorted((e1,e2) -> -e1.getValue().compareTo(e2.getValue())).
                    limit(25).forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue())));
        }

    }

    /**
     * Holds string literals (operation IDs).
     */
    private static class StringConstants {
        private static final String TERMINATE = "terminate";
        private static final String INIT = "init";
        private static final String PROCESS_WORDS = "process_words";
        private static final String REGISTER_FOR_UPDATES = "register_for_updates";
        private static final String UNFILTERED_WORD = "unfiltered_word";
        private static final String FILTERED_WORD = "filtered_word";
        private static final String RESET_COUNT = "reset_count";
        private static final String PRINT_TOP_25 = "print_top_25";
    }


}
