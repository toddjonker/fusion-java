// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A map that stores unique interned values for given keys.
 * Values are weakly referenced, so they may be garbage collected when they
 * become unreachable from other parts of the application.
 */
public class InternMap <K, V>
{
    private final Map<K, WeakReference<V>> myWeakMap = new ConcurrentHashMap<>();
    private final ReferenceQueue<V>        myQueue   = new ReferenceQueue<>();
    private final Function<K, V>           myValueFactory;


    public InternMap(Function<K, V> valueFactory)
    {
        myValueFactory = valueFactory;
    }


    /**
     * Tracks the key that maps to an interned value, so when a value is
     * collected, we can remove its map entry.
     */
    private static class CleanupRef <K, V>
        extends WeakReference<V>
    {
        private final K myKey;

        CleanupRef(V value, ReferenceQueue<V> queue, K key)
        {
            super(value, queue);
            myKey = key;
        }
    }

    /**
     * Produces an interned value for the given key.
     * <p>
     * If the key has no associated value, one is produced using the factory
     * provided to this map's constructor.
     */
    public V intern(K key)
    {
        cleanup();

        // This "box" ensures there is always a strong reference to the value.
        Object[] box = new Object[1];
        myWeakMap.compute(key, (k, ref) -> {
            V value = (ref != null) ? ref.get() : null;
            if (value == null)
            {
                value = myValueFactory.apply(key);
                ref = new CleanupRef(value, myQueue, key);
            }
            box[0] = value;
            return ref;
        });
        return (V) box[0];
    }

    /**
     * Remove any entries with garbage-collected values.
     */
    public void cleanup()
    {
        CleanupRef ref;
        while ((ref = (CleanupRef) myQueue.poll()) != null)
        {
            // CRITICAL: Use two-argument remove to avoid race condition.
            // The map entry may have been replaced with a new reference
            // for the same key after this reference was queued for cleanup.
            // Only remove if the exact reference still matches.
            myWeakMap.remove(ref.myKey, ref);
        }
    }

    public int size()
    {
        cleanup();
        return myWeakMap.size();
    }
}
