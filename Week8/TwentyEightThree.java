import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Solution for exercise 28.3 in "Exercises in Programming Style" by Professor C. Lopes. Second part of the homework of
 * week 8 of UCI CS253 (Fall 2018 edition). In this exercise, we're to combine the "actors" programming style with the
 * "lazy rivers" programming style.
 *
 * @author Janus Varmarken {@literal <jvarmark@uci.edu>}
 */
@SuppressWarnings("Duplicates") // Suppress warnings for duplicated code from previous and later exercises.
public class TwentyEightThree {

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

        // Register for outbound messages, forming a pipeline.
        wordExtractor.acceptMessage(createRegistrationMessage(wordFilter));
        wordFilter.acceptMessage(createRegistrationMessage(wordFreqTracker));
        // This registration is additional (when compared to 28.1 and 28.2) as we need to allow the end of the pipeline
        // to send messages to the front of the pipeline in order for the end to initiate a pull (see lazy rivers).
        wordFreqTracker.acceptMessage(createRegistrationMessage(wordExtractor));

        // Initiate things by sending the print top 25 message.
        wordFreqTracker.acceptMessage(new StringMessage(StringConstants.PRINT_TOP_25));
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

        /**
         * Dispatches a received message, providing subclasses of {@code Actor} a chance to handle the received message.
         * @param msg The received message.
         */
        abstract protected void dispatch(Message<?> msg);

    }

    /**
     * Actor that receives a filepath and extracts the words from the file at that path.
     */
    private static class WordExtractor extends Actor {

        /**
         * Reader that reads from the specified file.
         */
        private BufferedReader mReader;

        /**
         * Currently buffered words (originating from a single line read by {@link #mReader}).
         */
        private String[] mBufferedWords;

        /**
         * Next index to use when returning a word from {@link #mBufferedWords}.
         */
        private int mBufferedWordsIndex = 0;

        @Override
        protected void dispatch(Message<?> msg) {
            if (msg.getOperationId().equals(StringConstants.INIT) && msg instanceof StringMessage) {
                this.handleInitMessage((StringMessage) msg);
            } else if (msg.getOperationId().equals(StringConstants.NEXT_WORD)) {
                // We don't care about the message type as we do not make use of the message's arguments.
                this.handleNextWordMessage();
            } else {
                // Forward unknown messages of unknown types to later actors
                this.forwardMessage(msg);
            }
        }

        /**
         * Invoked upon reception of an init message. Initializes a {@link Stream} member field that contains each word
         * of the file at the path specified by the first argument in {@code msg}.
         * @param msg The init message.
         */
        private void handleInitMessage(StringMessage msg) {
            String filepath = msg.getArgs()[0];
            try {
                // Initialize the reader.
                mReader = new BufferedReader(new FileReader(filepath));
            } catch (FileNotFoundException e) {
                // Rethrow as unchecked (fail early + avoid try-catch in caller)
                throw new RuntimeException(e);
            }
        }

        /**
         * Handler for {@link StringConstants#NEXT_WORD} messages.
         */
        private void handleNextWordMessage() {
            try {
                if (mBufferedWords != null && mBufferedWordsIndex < mBufferedWords.length) {
                    // Return next buffered word if unprocessed words in buffer.
                    String w = mBufferedWords[mBufferedWordsIndex++];
                    mRecipients.forEach(r -> r.acceptMessage(new StringMessage(StringConstants.UNFILTERED_WORD, w)));
                } else {
                    // Otherwise read and buffer the next line.
                    String line = mReader.readLine();
                    if (line == null) {
                        // Reached end of file, so send stream empty message.
                        mRecipients.forEach(r -> r.acceptMessage(new StringMessage(StringConstants.STREAM_EMPTY)));
                        // And close file stream
                        mReader.close();
                    } else {
                        // Prepare word buffer
                        mBufferedWords = line.toLowerCase().replaceAll("[^a-zA-Z\\d\\s]", " ").split("\\s+");
                        mBufferedWordsIndex = 0;
                        // Recurse in order to trigger sending of next word.
                        handleNextWordMessage();
                    }
                }
            } catch (IOException e) {
                // Rethrow as unchecked (fail early + avoid try-catch in caller).
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Actor that filters words based on a stop words filter.
     */
    private static class WordFilter extends Actor {

        /**
         * The stop words.
         */
        private Set<String> mStopWords;

        @Override
        protected void dispatch(Message<?> msg) {
            if (msg.getOperationId().equals(StringConstants.INIT)) {
                // We don't care about the message type as we do not make use of the message's arguments.
                handleInitMessage();
            } else if (msg.getOperationId().equals(StringConstants.UNFILTERED_WORD) && msg instanceof StringMessage) {
                handleUnfilteredWordMessage((StringMessage) msg);
            } else {
                // Forward unknown message types to later actors
                this.forwardMessage(msg);
            }
        }

        /**
         * Handles {@link StringConstants#INIT} messages. Reads and parses the stop words file, putting its content
         * in {@link #mStopWords}.
         */
        private void handleInitMessage() {
            try (Stream<String> swStream = Files.lines(Paths.get("../stop_words.txt"))) {
                // Add single-char words to stop words stream, and collect stop words stream to set.
                // Note: explicitly use HashSet for O(1) complexity contains().
                mStopWords = Stream.concat(swStream.map(line -> line.split(",")).flatMap(Arrays::stream),
                        Arrays.stream("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(","))).
                        collect(Collectors.toCollection(HashSet::new));
            } catch (IOException ioe) {
                // Rethrow as unchecked (fail early + avoid try-catch in caller).
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Handles {@link StringConstants#UNFILTERED_WORD} messages.
         * @param msg A {@link StringConstants#UNFILTERED_WORD} message.
         */
        private void handleUnfilteredWordMessage(StringMessage msg) {
            for (String w : msg.getArgs()) {
                if (!mStopWords.contains(w)) {
                    // Not a stop word, so forward a filtered words message.
                    mRecipients.forEach(r -> r.acceptMessage(new StringMessage(StringConstants.FILTERED_WORD, w)));
                } else {
                    // A stop word. Forward message to later actors to allowed them a chance to request the next word.
                    this.forwardMessage(msg);
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
                this.handleFilteredWordMessage((StringMessage) msg);
            } else if (msg.getOperationId().equals(StringConstants.PRINT_TOP_25)) {
                // We don't care about the type of message as we do not make use of its arguments.
                this.handlePrintTop25Message();
            } else if (msg.getOperationId().equals(StringConstants.UNFILTERED_WORD)) {
                // We don't care about the type of message as we do not make use of its arguments.
                this.handleUnfilteredWordMessage();
            } else if (msg.getOperationId().equals(StringConstants.STREAM_EMPTY)) {
                // We don't care about the type of message as we do not make use of its arguments.
                this.handleStreamEmptyMessage();
            } else {
                // Forward unknown message types to later actors
                this.forwardMessage(msg);
            }
        }

        /**
         * Handles {@link StringConstants#FILTERED_WORD} messages.
         * @param msg A {@link StringConstants#FILTERED_WORD} message.
         */
        private void handleFilteredWordMessage(StringMessage msg) {
            for (String w : msg.getArgs()) {
                // Update word count.
                mWordFreqs.merge(w, 1, (current, one) -> current + one);
            }
            // Request the next word
            this.requestNextWord();
        }

        /**
         * Handles {@link StringConstants#UNFILTERED_WORD} messages.
         */
        private void handleUnfilteredWordMessage() {
            // Current word was a stop word, so ignore and request next word.
            this.requestNextWord();
        }

        /**
         * Handles {@link StringConstants#PRINT_TOP_25} messages.
         */
        private void handlePrintTop25Message() {
            // As we are initiating things from the end of the pipeline in this style, what we need to do here is to
            // pull the first word to set off the pipeline processing.
            requestNextWord();
        }

        /**
         * Handles {@link StringConstants#STREAM_EMPTY} messages.
         */
        private void handleStreamEmptyMessage() {
            // We've cleaned out the data source, hence now ready to print the top 25 entries.
            mWordFreqs.entrySet().stream().sorted((e1,e2) -> -e1.getValue().compareTo(e2.getValue())).
                    limit(25).forEach(e -> System.out.println(String.format("%s  -  %d", e.getKey(), e.getValue())));
            // Terminate the pipeline by sending termination message to self (and forward it to all Actors registered
            // for outbound messages).
            this.acceptMessage(new StringMessage(StringConstants.TERMINATE));
        }

        /**
         * Requests the next word by sending a {@link StringConstants#NEXT_WORD} to all recipients.
         */
        private void requestNextWord() {
            mRecipients.forEach(r -> r.acceptMessage(new StringMessage(StringConstants.NEXT_WORD)));
        }

    }

    /**
     * Holds string literals (operation IDs).
     */
    private static class StringConstants {
        private static final String TERMINATE = "terminate";
        private static final String INIT = "init";
        private static final String NEXT_WORD = "next_word";
        private static final String REGISTER_FOR_UPDATES = "register_for_updates";
        private static final String UNFILTERED_WORD = "unfiltered_word";
        private static final String FILTERED_WORD = "filtered_word";
        private static final String RESET_COUNT = "reset_count";
        private static final String PRINT_TOP_25 = "print_top_25";
        private static final String STREAM_EMPTY = "stream_empty";
    }

}
