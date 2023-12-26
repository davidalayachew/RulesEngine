
package io.github.davidalayachew;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public sealed interface ValueChain<V>
{

   public record ConsCell<V> (V head, ValueChain<V> valueChain) implements ValueChain<V>
   {
   
      public ConsCell
      {
      
         Objects.requireNonNull(head);
         Objects.requireNonNull(valueChain);
      
      }
      
      public Set<V> flatten()
      {
      
         final Set<V> output = new HashSet<>();
         
         output.add(this.head);
         output.addAll(this.valueChain.flatten());
         
         return Set.copyOf(output);
      
      }
   
   }

   public record ConsEmpty<V> () implements ValueChain<V>
   {
   
      public Set<V> flatten()
      {
      
         return Set.of();
      
      }
   
   }

   public Set<V> flatten();

}
