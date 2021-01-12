// Copyright (c) 2018-2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.util.hamt.HashArrayMappedTrie;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.Changes;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A functional hash table using a {@link HashArrayMappedTrie}.
 * <p>
 * This version is an internal implementation detail of FusionJava, is not intended
 * for reuse, and does not support Java nulls as keys or values. Attempts to use null
 * on operations such as {@link #get(Object)} or {@link #with(Object, Object)} will throw a
 * {@link NullPointerException}.
 * </p>
 * Iteration order of this data structure is undefined and not guaranteed to be stable.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FunctionalHashTrie<K, V>
    implements Iterable<Entry<K, V>>
{
    private static final String NULL_ERROR_MESSAGE =
        "FunctionalHashTrie does not support null keys or values";

    private static final FunctionalHashTrie EMPTY =
        new FunctionalHashTrie<>(HashArrayMappedTrie.empty(), 0);
    private final TrieNode<K, V> root;
    private final int size;


    // Temporary adaptor; will remove BiFunction soon.
    private static class RemappingChanges
        extends Changes
    {
        private final BiFunction remapping;

        RemappingChanges(BiFunction remapping)
        {
            this.remapping = remapping;
        }

        @Override
        protected Object replacing(Object storedValue, Object givenValue)
        {
            return remapping.apply(storedValue, givenValue);
        }
    }


    static <K, V> FunctionalHashTrie<K, V> empty()
    {
        return EMPTY;
    }

    static <K, V> FunctionalHashTrie<K, V> create(Map<K, V> other)
    {
        if (other.isEmpty()) return EMPTY;

        Changes changes = new Changes();
        TrieNode<K, V> trie = HashArrayMappedTrie.fromMap(other, changes);

        return new FunctionalHashTrie<>(trie, changes.keyCountDelta());
    }


    static <K, V> FunctionalHashTrie<K, V> merge(Iterator<Entry<K, V>> items,
                                                 BiFunction<V, V, V> remapping)
    {
        if (!items.hasNext()) return EMPTY;

        Changes changes = new RemappingChanges(remapping);
        TrieNode<K, V> trie = HashArrayMappedTrie.fromEntries(items, changes);
        return EMPTY.resultFrom(trie, changes);
    }

    static <K, V> FunctionalHashTrie<K, V> merge(Entry<K, V>[] items,
                                                 BiFunction<V, V, V> remapping)
    {
        return merge(Arrays.asList(items).iterator(), remapping);
    }


    /**
     * Creates a trie by copying entries for the {@code keys} from the
     * {@code origin} trie.
     */
    static <K, V> FunctionalHashTrie<K, V>
    fromSelectedKeys(FunctionalHashTrie<K, V> origin, K[] keys)
    {
        Changes changes = new Changes();
        TrieNode<K, V> newTrie =
                HashArrayMappedTrie.fromSelectedKeys(origin.root, keys, changes);
        return EMPTY.resultFrom(newTrie, changes);
    }


    FunctionalHashTrie(TrieNode<K, V> root, int size)
    {
        root.getClass(); // Null check

        this.root = root;
        this.size = size;
    }


    /**
     * @param key to examine the map for.
     * @return true if the key is in the map, false otherwise.
     */
    public boolean containsKey(K key)
    {
        return get(key) != null;
    }


    /**
     * @param key the key to search for.
     * @return the value associated with key, null it if is not in the map.
     */
    public V get(K key)
    {
        if (key == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }

        if (isEmpty())
        {
            return null;
        }
        else
        {
            return root.get((K) key);
        }
    }


    // TODO: Add a variation of with[out] that returns the previous value (if any).

    private FunctionalHashTrie<K, V> resultFrom(TrieNode<K, V> newRoot, Changes changes)
    {
        if (changes.changeCount() != 0)
        {
            int newSize = size + changes.keyCountDelta();
            if (newSize == 0) return EMPTY;
            return new FunctionalHashTrie<>(newRoot, newSize);
        }

        assert root == newRoot;
        return this;
    }

    /**
     * This method functionally modifies the {@link FunctionalHashTrie} and returns
     * a new {@link FunctionalHashTrie} if a modification was made, otherwise returns itself.
     *
     * The equivalent of Clojure's PersistentHashMap's assoc().
     */
    public FunctionalHashTrie<K, V> with(K key, V value)
    {
        if (key == null || value == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }

        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.with(key, value, changes);
        return resultFrom(newRoot, changes);
    }


    /**
     * This method functional removes a key from the {@link FunctionalHashTrie} and returns
     * a new {@link FunctionalHashTrie} if a modification was made, otherwise returns itself.
     */
    public FunctionalHashTrie<K, V> without(K key)
    {
        if (isEmpty())
        {
            return this;
        }
        else if (key == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }
        else
        {
            Changes changes = new Changes();
            TrieNode<K, V> newRoot = root.without(key, changes);
            return resultFrom(newRoot, changes);
        }
    }


    /**
     * Functionally removes multiple keys from this trie.
     *
     * @param keys must not be null.
     *
     * @return the resulting trie.
     */
    public FunctionalHashTrie<K, V> withoutKeys(K[] keys)
    {
        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.withoutKeys(keys, changes);
        return resultFrom(newRoot, changes);
    }


    @Override
    public Iterator<Entry<K, V>> iterator()
    {
        return root.iterator();
    }


    public Set<K> keySet()
    {
        // FIXME This is an extremely expensive implementation.
        return new AbstractSet<K>()
        {
            @Override
            public Iterator<K> iterator()
            {
                final Iterator<Entry<K, V>> entryIter = FunctionalHashTrie.this.iterator();
                return new Iterator<K>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return entryIter.hasNext();
                    }

                    @Override
                    public K next()
                    {
                        return entryIter.next().getKey();
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return size;
            }
        };
    }


    public int size()
    {
        return size;
    }


    public boolean isEmpty()
    {
        return size == 0;
    }


    // TODO: Add method that accepts a function to modify each element in the trie.
}
