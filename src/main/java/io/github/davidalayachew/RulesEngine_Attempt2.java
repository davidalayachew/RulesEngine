
package io.github.davidalayachew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RulesEngine_Attempt2
{

   public sealed interface PutResponse
   {
   
      public record DirectMappingAlreadyExists() implements PutResponse {}
      public record IndirectMappingAlreadyExists(KeyValueChain<Identifier, Type> keyValueChain) implements PutResponse {}
      public record NewDirectMappingCreated() implements PutResponse {}
   
   }

   private final MultiMap<Identifier,     Type>          isInstances    = new MultiMap<>();
   private final MultiMap<FrequencyType,  Type>          isRules        = new MultiMap<>();
   private final MultiMap<Identifier,     QuantityType>  hasInstances   = new MultiMap<>();
   private final MultiMap<FrequencyType,  QuantityType>  hasRules       = new MultiMap<>();

   public RulesEngine_Attempt2.PutResponse put(final IdentifierIsAType identifierIsAType)
   {
   
      Objects.requireNonNull(identifierIsAType);
   
      final Identifier identifier   = identifierIsAType.identifier();
      final Type type               = identifierIsAType.type();
   
      DIRECT_MAPPING:
      {
      
         if (this.containsDirectIsMapping(identifier, type))
         {
         
            return new PutResponse.DirectMappingAlreadyExists();
         
         }
      
      }
   
      INDIRECT_MAPPING:
      {
      
         if (this.containsIndirectIsMapping(identifier, type))
         {
         
            return new PutResponse.IndirectMappingAlreadyExists(null);
         
         }
      
      }
   
      this.isInstances.add(identifier, type);
   
      return new PutResponse.NewDirectMappingCreated();
   
   }

   private boolean containsDirectIsMapping(final Identifier identifier, final Type type)
   {
   
      return this.isInstances.containsMapping(identifier, type);
   
   }

   private boolean containsIndirectIsMapping(final Identifier identifier, final Type goal)
   {
   
      final List<Type> mappingsToAttempt = new ArrayList<>();
   
      final int mappingsToAttemptIndex = 0;
   
      final Set<Type> directMappings = this.isInstances.get(identifier).orElse(Set.of());
   
      mappingsToAttempt.addAll(directMappings);
   
      return this.recursiveIndirectIsMapping(identifier, goal, mappingsToAttempt, mappingsToAttemptIndex);
   
   }

   private boolean recursiveIndirectIsMapping
   (
      final Identifier identifier,
      final Type goal,
      final List<Type> mappingsToAttempt,
      final int mappingsToAttemptIndex
   )
   {
   
      if (mappingsToAttemptIndex >= mappingsToAttempt.size())
      {
      
         return false;
      
      }
   
      final Type currentType = mappingsToAttempt.get(mappingsToAttemptIndex);
   
      if (goal.equals(currentType))
      {
      
         return true;
      
      }
   
      final FrequencyType potentialIsMappingKey = new FrequencyType(Frequency.EVERY, currentType);
   
      final Set<Type> currentTypeIsMappings =
         this
            .isRules
            .get(potentialIsMappingKey)
            .orElse(Set.of())
            ;
   
      for (final Type eachCurrentTypeIsMapping : currentTypeIsMappings)
      {
      
         if (!mappingsToAttempt.contains(eachCurrentTypeIsMapping))
         {
         
            mappingsToAttempt.add(eachCurrentTypeIsMapping);
         
         }
      
      }
   
      return this.recursiveIndirectIsMapping(identifier, goal, mappingsToAttempt, mappingsToAttemptIndex + 1);
   
   }

}
