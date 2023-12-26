
package io.github.davidalayachew;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class MultiMap<K, V>
{

   public sealed interface PutResponse
   {
   
      public record NewMappingCreated() implements PutResponse {}
      public record RequestedMappingAlreadyExists() implements PutResponse {}
      public record ReplacedOldMapping <V> (V oldValue) implements PutResponse {}
   
   }

   private final Map<K, Set<V>> map = new HashMap<>();

   public MultiMap.PutResponse add(final K key, final V value)
   {
   
      final Set<V> previousValue = this.map.merge(key, this.singletonSet(value), this::combineSets);
   
      return
         switch (previousValue)
         {
         
            case null   -> new MultiMap.PutResponse.NewMappingCreated();
            default     ->
               previousValue == value
                  ?  new MultiMap.PutResponse.RequestedMappingAlreadyExists()
                  :  new MultiMap.PutResponse.ReplacedOldMapping<Set<V>>(previousValue)
                  ;
         
         }
         ;
   
   }

   public Optional<Set<V>> get(final K key)
   {
   
      final Set<V> value = this.map.get(key);
   
      return
         Optional
            .ofNullable(value)
            .map(Set::copyOf)
            ;
   
   }

   public boolean containsKey(final K key)
   {
   
      return this.map.containsKey(key);
   
   }

   public boolean containsValue(final V value)
   {
   
      return
         this
            .map
            .values()
            .stream()
            .flatMap(Set::stream)
            .anyMatch(value::equals)
            ;
   
   }

   public boolean containsMapping(final K key, final V value)
   {
   
      return
         this
            .get(key)
            .map(set -> set.contains(value))
            .orElse(false)
            ;
   
   }

   private Set<V> singletonSet(final V value)
   {
   
      final Set<V> singletonSet = new HashSet<>();
   
      singletonSet.add(value);
   
      return singletonSet;
   
   }

   private Set<V> combineSets(final Set<V> original, final Set<V> incoming)
   {
   
      original.addAll(incoming);
   
      return original;
   
   }

}
