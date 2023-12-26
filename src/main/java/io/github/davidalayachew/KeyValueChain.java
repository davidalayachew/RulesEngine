
package io.github.davidalayachew;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record KeyValueChain<K, V> (K key, ValueChain<V> valueChain)
{

   public KeyValueChain
   {
   
      Objects.requireNonNull(key);
      Objects.requireNonNull(valueChain);
   
   }

   public Map.Entry<K, Set<V>> flatten()
   {
   
      return Map.entry(this.key, this.valueChain.flatten());
   
   }

}
