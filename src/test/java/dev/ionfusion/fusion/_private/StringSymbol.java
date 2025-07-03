import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;

public class StringSymbol {
    private final String value;
    
    private StringSymbol(String value) {
        this.value = value;
    }
    
    public String getValue() { return value; }
    
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof StringSymbol && 
               Objects.equals(value, ((StringSymbol) o).value));
    }
    
    @Override
    public int hashCode() { return Objects.hashCode(value); }
    
    public static class Interner {
        private final ConcurrentHashMap<String, WeakReference<StringSymbol>> 
            internMap = new ConcurrentHashMap<>();
        private final ReferenceQueue<StringSymbol> queue = new ReferenceQueue<>();
        
        private class CleanupRef extends WeakReference<StringSymbol> {
            final String key;
            CleanupRef(String key, StringSymbol symbol) {
                super(symbol, queue);
                this.key = key;
            }
        }
        
        public StringSymbol intern(String value) {
            cleanup();
            
            StringSymbol[] holder = new StringSymbol[1];
            internMap.compute(value, (k, ref) -> {
                StringSymbol symbol = (ref != null) ? ref.get() : null;
                if (symbol == null) {
                    symbol = new StringSymbol(value);
                    ref = new CleanupRef(value, symbol);
                }
                holder[0] = symbol;
                return ref;
            });
            return holder[0];
        }
        
        private void cleanup() {
            CleanupRef ref;
            while ((ref = (CleanupRef) queue.poll()) != null) {
                // CRITICAL: Use two-argument remove to avoid race condition.
                // The map entry may have been replaced with a new reference
                // for the same key after this reference was queued for cleanup.
                // Only remove if the exact reference still matches.
                internMap.remove(ref.key, ref);
            }
        }
        
        // Package-access method for testing
        int getInternMapSize() {
            return internMap.size();
        }
    }
}