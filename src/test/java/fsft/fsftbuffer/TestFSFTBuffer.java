package fsft.fsftbuffer;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestFSFTBuffer {

    private class Thing implements Bufferable {
        private final String id;

        public Thing(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    @Test
    public void testPut() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        Thing t1 = new Thing("1");
        Thing t2 = new Thing("2");
        Thing t3 = new Thing("3");
        buffer.put(t1);
        buffer.put(t2);
        assertTrue(buffer.put(t3));
    }

    @Test
    public void testGet() throws InvalidIdentifierException {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        Thing t1 = new Thing("1");
        buffer.put(t1);
        assertEquals(t1, buffer.get("1"));
    }

    @Test
    public void testGetInvalidIdentifier() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        assertThrows(InvalidIdentifierException.class, () -> buffer.get("invalid"));
    }

    @Test
    public void testTouch() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        Thing t1 = new Thing("1");
        buffer.put(t1);
        assertTrue(buffer.touch("1"));
    }

    @Test
    public void testTouchInvalidIdentifier() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        assertFalse(buffer.touch("invalid"));
    }

    @Test
    public void testPutNull() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        assertFalse(buffer.put(null));
    }

    @Test
    public void testBufferCapacity() {
        FSFTBuffer<Thing> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));
        Thing t1 = new Thing("1");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        Thing t2 = new Thing("2");
        Thing t3 = new Thing("3");
        buffer.put(t1);
        buffer.put(t2);
        buffer.put(t3);
        assertThrows(InvalidIdentifierException.class, () -> buffer.get("1"));
    }
}
