// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InternMapTest
{
    public static class Interner
        extends InternMap<String, StringSymbol>
    {
        public Interner()
        {
            super(StringSymbol::new);
        }
    }

    private Interner interner;

    @BeforeEach
    void setUp()
    {
        interner = new Interner();
    }

    @Test
    void nullKeyShouldThrowNPE()
    {
        assertThrows(NullPointerException.class, () -> {
            interner.intern(null);
        });
    }

    @Test
    void sameKeyProducesSameSymbol()
    {
        StringSymbol sym1 = interner.intern("test");
        StringSymbol sym2 = interner.intern("test");

        assertSame(sym1, sym2);
        assertEquals("test", sym1.getValue());
    }

    @Test
    void distinctKeysProduceDistinctSymbols()
    {
        StringSymbol sym1 = interner.intern("test1");
        StringSymbol sym2 = interner.intern("test2");

        assertNotSame(sym1, sym2);
        assertEquals("test1", sym1.getValue());
        assertEquals("test2", sym2.getValue());
    }

    @Test
    void internersProduceDistinctSymbols()
    {
        Interner     interner2 = new Interner();
        StringSymbol sym1      = interner.intern("test");
        StringSymbol sym2      = interner2.intern("test");

        // Different instances but same value should be equal
        assertNotSame(sym1, sym2);
        assertEquals(sym1, sym2);
        assertEquals(sym1.hashCode(), sym2.hashCode());
    }

    @Test
    void testConcurrentInterningOfOneKey()
        throws InterruptedException
    {
        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        Set<StringSymbol> results = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++)
        {
            executor.submit(() -> {
                try
                {
                    // Wait for all threads to be ready
                    startLatch.await();

                    for (int j = 0; j < iterationsPerThread; j++)
                    {
                        String       key = "concurrent";
                        StringSymbol sym = interner.intern(key);
                        results.add(sym);
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    finishLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously; wait for all to finish
        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // All threads should get the same instance
        assertEquals(1, results.size());
    }

    @Test
    void testConcurrentInterningOfManyKeys()
        throws InterruptedException
    {
        int             threadCount = 10;
        int             symbolCount = 500;
        ExecutorService executor    = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  startLatch  = new CountDownLatch(1);
        CountDownLatch  finishLatch = new CountDownLatch(threadCount);

        // Preallocate result sets and symbol strings
        @SuppressWarnings("unchecked")
        Set<StringSymbol>[] results = new Set[symbolCount];
        String[] symbolStrings = new String[symbolCount];
        for (int i = 0; i < symbolCount; i++)
        {
            results[i] = ConcurrentHashMap.newKeySet();
            symbolStrings[i] = "symbol" + i;
        }

        for (int i = 0; i < threadCount; i++)
        {
            executor.submit(() -> {
                try
                {
                    startLatch.await();
                    for (int j = 0; j < symbolCount; j++)
                    {
                        StringSymbol sym = interner.intern(symbolStrings[j]);
                        results[j].add(sym);
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Each symbol should have exactly one unique instance
        for (int i = 0; i < symbolCount; i++)
        {
            assertEquals(1,
                         results[i].size(),
                         "Symbol " + symbolStrings[i] + " should have only one instance");
        }
    }

    @Test
    void garbageCollectionReleasesEntries()
        throws InterruptedException
    {
        // Create symbols and keep strong references to prevent premature GC
        int            symbolCount = 100;
        StringSymbol[] symbols     = new StringSymbol[symbolCount];
        for (int i = 0; i < symbolCount; i++)
        {
            // Use UUID-like strings that won't be interned by JVM
            symbols[i] = interner.intern("gc_test_" + System.nanoTime() + "_" + i);
        }

        int sizeAfterCreation = interner.size();
        assertEquals(symbolCount, sizeAfterCreation,
                     "Should have created exactly " + symbolCount + " symbols");

        // Release strong references to make symbols eligible for GC
        symbols = null;

        // Force GC aggressively
        for (int i = 0; i < 10; i++)
        {
            System.gc();
            Thread.sleep(50);
        }

        // Create a new symbol to trigger cleanup during intern()
        interner.intern("trigger_" + System.nanoTime());

        // We expect some cleanup to have occurred
        int finalSize = interner.size();
        assertTrue(finalSize < sizeAfterCreation,
                   "Expected cleanup to reduce map size from " + sizeAfterCreation + ", but got " +
                   finalSize);
    }
}
