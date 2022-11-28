package io.github.davidalayachew;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

   public final class ClassParser
   {
   
      private ClassParser() {throw new UnsupportedOperationException();}
   
      private static final Map<Pattern, Function<List<String>, Parseable>> map = createMap();
   
      private static Map<Pattern, Function<List<String>, Parseable>> createMap()
      {
      
         Map<Pattern, Function<List<String>, Parseable>> map = new HashMap<>();
      
         for (Class<?> each : Parseable.class.getPermittedSubclasses())
         {
         
            @SuppressWarnings("unchecked")
               Class<? extends Parseable> temp = (Class<Parseable>)each;
         
            try
            {
            
               List<String> strings =
                  switch (each.getSimpleName())
                  {
                  
                     case "Identifier"                   -> List.of("A");
                     case "Type"                         -> List.of("A");
                     case "Quantity", "QuantityType"     -> List.of("1", "A");
                     case "IdentifierHasQuantityType"    -> List.of("A", "1", "A");
                     case "IdentifierIsAType"            -> List.of("A", "A");
                     case "FrequencyType"                -> List.of("EVERY", "A", "A");
                     case "FrequencyTypeRelationship"    -> List.of("EVERY", "A", "IS A");
                     case "FrequencyTypeHasQuantityType" -> List.of("EVERY", "A", "1", "A");
                     case "FrequencyTypeIsType"          -> List.of("EVERY", "A", "A");
                     case "IsIdentifierAType"            -> List.of("EVERY", "A");
                     default                             -> throw new IllegalArgumentException("Forgot to update this method!");
                  
                  };
            
               Constructor<? extends Parseable> constructor = temp.getConstructor(List.class);
               Parseable godForgiveMe = constructor.newInstance(strings);
            
            
               switch (godForgiveMe)
               {
               
                  case Identifier i                         -> map.put(Identifier.regex, Identifier::new);
                  case Type t                               -> map.put(Type.regex, Type::new);
                  case Quantity q                           -> map.put(Quantity.regex, Quantity::new);
                  case QuantityType qt                      -> map.put(QuantityType.regex, QuantityType::new);
                  case IdentifierHasQuantityType ihqt       -> map.put(IdentifierHasQuantityType.regex, IdentifierHasQuantityType::new);
                  case IdentifierIsAType iiat               -> map.put(IdentifierIsAType.regex, IdentifierIsAType::new);
                  case FrequencyType ft                     -> map.put(FrequencyType.regex, FrequencyType::new);
                  case FrequencyTypeRelationship ftr        -> map.put(FrequencyTypeRelationship.regex, FrequencyTypeRelationship::new);
                  case FrequencyTypeHasQuantityType fthqt   -> map.put(FrequencyTypeHasQuantityType.regex, FrequencyTypeHasQuantityType::new);
                  case FrequencyTypeIsType ftit             -> map.put(FrequencyTypeIsType.regex, FrequencyTypeIsType::new);
                  case IsIdentifierAType iiat               -> map.put(IsIdentifierAType.regex, IsIdentifierAType::new);
               
               }
            
            }
            
            catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception)
            {
            
               throw new IllegalStateException(exception);
            
            }
         
         }
      
         return Collections.unmodifiableMap(map);
      
      }
   
      public static final Optional<? extends Parseable> parse(String text)
      {
      
         for (Map.Entry<Pattern, Function<List<String>, Parseable>> each : map.entrySet())
         {
         
            Pattern regex = each.getKey();
         
            Matcher match = regex.matcher(text);
         
            if (match.matches())
            {
            
               return Optional.of(each.getValue().apply(fetchGroups(match)));
            
            }
         
         }
      
         return Optional.empty();
      
      }
   
   private static final List<String> fetchGroups(Matcher match)
   {
   
      List<String> groups = new ArrayList<>();
   
      for (int i = 1; i <= match.groupCount(); i++)
      {
      
         groups.add(match.group(i));
      
      }
   
      return groups;
   
   }

   }
