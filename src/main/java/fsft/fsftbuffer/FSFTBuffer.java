package fsft.fsftbuffer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A finite-space finite-time buffer that provides the following methods:
 * <ul>
 * <li>{@link #put(Bufferable)}: add an object to the buffer</li>
 * <li>{@link #get(String)}: retrieve an object from the buffer</li>
 * <li>{@link #touch(String)}: update the last refresh time for an object</li>
 * </ul>
 */
public class FSFTBuffer<B extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DEFAULT_CAPACITY = 32;

    /* the default timeout value is 180 seconds */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);

    private final int capacity;
    private final Duration timeoutDuration;

    private final ConcurrentSkipListSet<Item> buffer = new ConcurrentSkipListSet<>(
            (i1, i2) -> i1.timeused.compareTo(i2.timeused));

    /**
     * An item in the buffer that holds the object and metadata
     */
    private class Item {
        private final B value;
        private Instant timeout;
        private Instant timeused;

        public Item(B b) {
            this.value = b;
            this.timeout = Instant.now().plus(timeoutDuration);
            this.timeused = Instant.now();
        }

        /**
         * update the last access time for the object
         * @return {@code true} if update is successful, {@code false} otherwise
         */
        public boolean use() {
            if (Instant.now().isBefore(timeout)) {
                timeused = Instant.now();
                return true;
            }
            return false;
        }

        /**
         * refresh the timeout and the last access time for the object
         * @return {@code true} if refresh is successful, {@code false} otherwise
         */
        public boolean refresh() {
            if (Instant.now().isBefore(timeout)) {
                timeout = Instant.now().plus(timeoutDuration);
                timeused = Instant.now();
                return true;
            }
            return false;
        }
    }

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, Duration timeout) {
        this.capacity = capacity;
        this.timeoutDuration = timeout;
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DEFAULT_CAPACITY, DEFAULT_TIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     * This method can be used to replace an object in the buffer with
     * a newer instance. {@code b} is uniquely identified by its id,
     * {@code b.id()}.
     */
    public boolean put(B b) {
        if (b == null) {
            return false;
        }
        buffer.stream().findAny().ifPresent(i -> {
            if (i.value.id().equals(b.id())) {
                buffer.remove(i);
            }
        });
        while (buffer.size() >= capacity) {
            buffer.pollFirst();
        }
        buffer.add(new Item(b));
        return true;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     *         buffer
     */
    public B get(String id) throws InvalidIdentifierException {
        Item item = buffer.stream().filter(i -> i.value.id().equals(id)).findAny().orElse(null);
        if (item == null || !item.use()) {
            throw new InvalidIdentifierException("No object with the id: " + id + " found in the buffer");
        }
        return item.value;
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        Item item = buffer.stream().filter(i -> i.value.id().equals(id)).findAny().orElse(null);
        if (item == null || !item.refresh()) {
            return false;
        }
        return true;
    }
}
