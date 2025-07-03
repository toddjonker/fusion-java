import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StringSymbolTest {
    private StringSymbol.Interner interner;
    
    @BeforeEach
    void setUp() {
        interner = new StringSymbol.Interner();
    }
    
    @Test
    void testBasicInterning() {
        StringSymbol sym1 = interner.intern("test");
        StringSymbol sym2 = interner.intern("test");
        
        assertSame(sym1, sym2);
        assertEquals("test", sym1.getValue());
    }
    
    @Test
    void testDifferentStrings() {
        StringSymbol sym1 = interner.intern("test1");
        StringSymbol sym2 = interner.intern("test2");
        
        assertNotSame(sym1, sym2);
        assertEquals("test1", sym1.getValue());
        assertEquals("test2", sym2.getValue());
    }
    
    @Test
    void testEqualsAndHashCode() {
        StringSymbol.Interner interner2 = new StringSymbol.Interner();
        StringSymbol sym1 = interner.intern("test");
        StringSymbol sym2 = interner2.intern("test");
        
        // Different instances but same value should be equal
        assertNotSame(sym1, sym2);
        assertEquals(sym1, sym2);
        assertEquals(sym1.hashCode(), sym2.hashCode());
    }
    
    @Test
    void testConcurrentInterning() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        Set<StringSymbol> results = ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < iterationsPerThread; j++) {
                        StringSymbol sym = interner.intern("concurrent");
                        results.add(sym);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Release all threads simultaneously
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        assertEquals(1, results.size()); // All threads should get same instance
        executor.shutdown();
    }
    
    @Test
    void testGarbageCollection() throws InterruptedException {
        // Create symbols and keep strong references to prevent premature GC
        int symbolCount = 100;
        StringSymbol[] symbols = new StringSymbol[symbolCount];
        for (int i = 0; i < symbolCount; i++) {
            // Use UUID-like strings that won't be interned by JVM
            symbols[i] = interner.intern("gc_test_" + System.nanoTime() + "_" + i);
        }
        
        int sizeAfterCreation = interner.getInternMapSize();
        assertEquals(symbolCount, sizeAfterCreation, "Should have created exactly " + symbolCount + " symbols");
        
        // Release strong references to make symbols eligible for GC
        symbols = null;
        
        // Force GC aggressively
        for (int i = 0; i < 10; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(50);
        }
        
        // Create a new symbol to trigger cleanup during intern()
        interner.intern("trigger_" + System.nanoTime());
        
        int finalSize = interner.getInternMapSize();
        
        // We expect some cleanup to have occurred
        assertTrue(finalSize < sizeAfterCreation, 
            "Expected cleanup to reduce map size from " + sizeAfterCreation + " to less, but got " + finalSize);
    }
    
    @Test
    void testNullValue() {
        assertThrows(NullPointerException.class, () -> {
            interner.intern(null);
        });
    }
    
    @Test
    void testConcurrentMultipleSymbols() throws InterruptedException {
        int threadCount = 10;
        int symbolCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        
        // Preallocate result sets and symbol strings
        @SuppressWarnings("unchecked")
        Set<StringSymbol>[] results = new Set[symbolCount];
        String[] symbolStrings = new String[symbolCount];
        for (int i = 0; i < symbolCount; i++) {
            results[i] = ConcurrentHashMap.newKeySet();
            symbolStrings[i] = "symbol" + i;
        }
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < symbolCount; j++) {
                        StringSymbol sym = interner.intern(symbolStrings[j]);
                        results[j].add(sym);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        
        // Each symbol should have exactly one unique instance
        for (int i = 0; i < symbolCount; i++) {
            assertEquals(1, results[i].size(), "Symbol " + symbolStrings[i] + " should have only one instance");
        }
        
        executor.shutdown();
    }
    
    @Test
    void testEmptyString() {
        String empty1 = new String("");
        String empty2 = new String("");
        
        StringSymbol sym1 = interner.intern(empty1);
        StringSymbol sym2 = interner.intern(empty2);
        
        assertSame(sym1, sym2);
        assertEquals("", sym1.getValue());
    }
    
    @Test
    void testLongStrings() {
        // Test with long strings to ensure no issues with large content
        String long1 = "a".repeat(1000) + "1";
        String long2 = "a".repeat(1000) + "2";
        
        StringSymbol sym1a = interner.intern(long1);
        StringSymbol sym1b = interner.intern(long1);
        StringSymbol sym2 = interner.intern(long2);
        
        assertSame(sym1a, sym1b);
        assertNotSame(sym1a, sym2);
        assertEquals(long1, sym1a.getValue());
        assertEquals(long2, sym2.getValue());
    }
    
    @Test
    void testMultipleInterners() {
        StringSymbol.Interner interner1 = new StringSymbol.Interner();
        StringSymbol.Interner interner2 = new StringSymbol.Interner();
        
        StringSymbol sym1 = interner1.intern("test");
        StringSymbol sym2 = interner2.intern("test");
        
        // Different interners should produce different instances
        assertNotSame(sym1, sym2);
        assertEquals(sym1.getValue(), sym2.getValue());
    }
}