package io.github.davidalayachew;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RulesEngine
{

   private sealed interface Parseable
         permits
            Type, 
            Identifier, 
            IdentifierIsType, 
            IdentifierHasQuantityType, 
            Quantity, 
            QuantityType, 
            FrequencyType, 
            FrequencyTypeHasQuantityType, 
            FrequencyTypeIsType, 
            FrequencyTypeRelationship,
            IsIdentifierType
   {}
   
   private interface ToString
   {
   
      default public String cleanString()
      {
      
         try
         {
         
            String output = "";
         
            if (this.getClass().isRecord())
            {
            
               var components = this.getClass().getRecordComponents();
            
               for (var each : components)
               {
               
                  output += " " + each.getAccessor().invoke(this);
               
               }
            
            }
         
            return output;
         
         }
         
         catch (IllegalAccessException | InvocationTargetException e)
         {
         
            throw new IllegalStateException(e);
         
         }
      
      }
   
   }
   
   private enum Frequency
   {
   
      EVERY,         //every        xyz is an abc
      NOT_EVERY,     //not every    xyz is an abc
      NOT_A_SINGLE,  //not a single xyz is an abc
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = convertToPatternGroup(values());
   }

   private enum Relationship
   {
   
      HAS,  //Frequency Type has Quantity Type
      IS_A, //Instance is Type
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = convertToPatternGroup(values());
      
      public String pattern()
      {
      
         return switch (this)
            {
            
               case HAS    -> this.name();
               case IS_A   -> this.name().replace("_", " ").replace(" A", "(?: A|)");
            
            };
      
      }
      
   }

   private enum Response
   {
   
      OK,
      NOT_YET_IMPLEMENTED,
      CORRECT,
      POSSIBLY,
      INCORRECT,
      UNKNOWN_IDENTIFIER,
      UNKNOWN_TYPE,
      ;
   
   }

   private enum QueryModifier
   {
   
      MUST_EQUAL,
      CAN_EQUAL,
      CANNOT_EQUAL,
      ;
   
   }

   private final class ClassParser
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
                  
                     case "Type"                         -> List.of("A");
                     case "Quantity", "QuantityType"     -> List.of("1", "A");
                     case "Identifier"                   -> List.of("A");
                     case "IdentifierHasQuantityType"    -> List.of("A", "1", "A");
                     case "IdentifierIsType"             -> List.of("A", "A");
                     case "FrequencyType"                -> List.of("EVERY", "A", "A");
                     case "FrequencyTypeRelationship"    -> List.of("EVERY", "A", "IS A");
                     case "FrequencyTypeHasQuantityType" -> List.of("EVERY", "A", "1", "A");
                     case "FrequencyTypeIsType"          -> List.of("EVERY", "A", "A");
                     case "IsIdentifierType"             -> List.of("EVERY", "A");
                     default                             -> throw new IllegalArgumentException("Forgot to update this method!");
                  
                  };
            
               Constructor<? extends Parseable> constructor = temp.getConstructor(List.class);
               Parseable godForgiveMe = constructor.newInstance(strings);
               
               
               switch (godForgiveMe)
               {
               
                  case Type t                               -> map.put(Type.regex, Type::new);
                  case Quantity q                           -> map.put(Quantity.regex, Quantity::new);
                  case QuantityType qt                      -> map.put(QuantityType.regex, QuantityType::new);
                  case Identifier i                         -> map.put(Identifier.regex, Identifier::new);
                  case IdentifierHasQuantityType ihqt       -> map.put(IdentifierHasQuantityType.regex, IdentifierHasQuantityType::new);
                  case IdentifierIsType iit                 -> map.put(IdentifierIsType.regex, IdentifierIsType::new);
                  case FrequencyType ft                     -> map.put(FrequencyType.regex, FrequencyType::new);
                  case FrequencyTypeRelationship ftr        -> map.put(FrequencyTypeRelationship.regex, FrequencyTypeRelationship::new);
                  case FrequencyTypeHasQuantityType fthqt   -> map.put(FrequencyTypeHasQuantityType.regex, FrequencyTypeHasQuantityType::new);
                  case FrequencyTypeIsType ftit             -> map.put(FrequencyTypeIsType.regex, FrequencyTypeIsType::new);
                  case IsIdentifierType iit                 -> map.put(IsIdentifierType.regex, IsIdentifierType::new);
               
               }
            
               // throw new UnsupportedOperationException("Comment me out because jGRASP!");
            
            }
            
            catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception)
            {
            
               throw new IllegalStateException(exception);
            
            }
         
         }
      
         return Collections.unmodifiableMap(map);
      
      }
   
      private static final Optional<? extends Parseable> parse(String text)
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

   private record Type(String name) implements Parseable, ToString
   {
   
      public static final Pattern regex = Pattern.compile("([a-zA-Z]+[a-zA-Z0-9]*)");
   
      public Type(List<String> string)
      {
      
         this(string.get(0));
      
      }
   
      public String toString() {
         return cleanString(); }
    
   }

   private record Identifier(String name)  implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile("([a-zA-Z]+[a-zA-Z0-9]*)");
   
      public Identifier(List<String> strings)
      {
      
         this(strings.get(0));
      
      }
   
      public String toString() {
         return cleanString(); }
    
   }

   private record Quantity(long count) implements Parseable, ToString
   {
   
      public static final Pattern regex = Pattern.compile("(A|\\d{1,7})");
      
      public Quantity(List<String> strings)
      {
      
         this(Long.parseLong(strings.get(0).replace("A", "1")));
      
      }
   
      public String toString() {
         return cleanString(); }
    
   }

   private record QuantityType(Quantity quantity, Type type) implements Parseable, ToString
   {
   
      public static final Pattern regex = Pattern.compile(                          //If pattern is surrounded by () then it's a group
                                                               Quantity.regex       //group 1
                                                               + " " + Type.regex   //group 2
                                                               );
   
      public QuantityType(List<String> groups)
      {
      
         this(
               new Quantity(groups.subList(0, 1)),
               new Type(groups.subList(1, 2))
            );
      
      }
   
      public String toString() {
         return cleanString(); }
    
   }

   private record IdentifierHasQuantityType(Identifier identifier, QuantityType quantityType) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            Identifier.regex                    //group 1
                                                            + " " + Relationship.HAS.pattern()  //not a group because no (), so just a pattern
                                                            + " " + QuantityType.regex          //group 2 and 3
                                                            );
      
      public IdentifierHasQuantityType(List<String> strings)
      {
      
         this(new Identifier(strings.subList(0, 1)), new QuantityType(strings.subList(1, 3)));
      
      }
      
      public String toString() {
         return cleanString(); }
    
   }

   private record IdentifierIsType(Identifier identifier, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            Identifier.regex                    //group 1
                                                            + " " + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                            + " " + Type.regex                  //group 2
                                                            );
      
      public IdentifierIsType(List<String> strings)
      {
      
         this(new Identifier(strings.subList(0, 1)), new Type(strings.subList(1, 2)));
      
      }
      
      public String toString() {
         return cleanString(); }
    
   }

   private record FrequencyType(Frequency frequency, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            Frequency.regex               //group 1
                                                            + " " + Type.regex            //group 2
                                                            );
   
      public FrequencyType(List<String> strings)
      {
      
         this(Frequency.valueOf(strings.get(0).replace(" ", "_")), new Type(strings.subList(1, 2)));
      
      }
      
      public String toString() {
         return cleanString(); }
    
   }

   private record FrequencyTypeRelationship(FrequencyType frequencyType, Relationship relationship) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex           //group 1 and 2
                                                            + " " + Relationship.regex    //group 3
                                                            );
      
      public FrequencyTypeRelationship(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), Relationship.valueOf(strings.get(2).replace(" ", "_")));
      
      }
      
      public String toString() {
         return cleanString(); }
    
   }

   private record FrequencyTypeHasQuantityType(FrequencyType frequencyType, QuantityType quantityType) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex                 //group 1 and 2
                                                            + " " + Relationship.HAS.pattern()  //not a group because no (), so just a pattern
                                                            + " " + QuantityType.regex          //group 3 and 4
                                                            );
      
      public FrequencyTypeHasQuantityType(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), new QuantityType(strings.subList(2, 4)));
      
      }
    
      public String toString() {
         return cleanString(); }
    
   }

   private record FrequencyTypeIsType(FrequencyType frequencyType, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex                 //group 1 and 2
                                                            + " " + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                            + " " + Type.regex                  //group 3
                                                            );
      
      public FrequencyTypeIsType(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), new Type(strings.subList(2, 3)));
      
      }
    
      public String toString() {
         return cleanString(); }
    
   }

   private record IsIdentifierType(Identifier identifier, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex =   // Unfortunately, I don't (currently) see a way to make this easy or neat or clean
         Pattern.compile(
            "(?:"                                     // Beginning of non-capturing group
                  // Starting Option 1---------------------------------------------------
                  + "IS"                              // not a group             IS
                  + " " + Identifier.regex            // group 1                 Identifier (ex. DAVID)
                  + "(?: A|)"                         // not a capturing group   Optional A
                  + " " + Type.regex                  // group 2                 Type (ex. MAN)
                  + "(?:\\?|)"                        // not a capturing group   Optional question mark at the end of the sentence
      
                                                      //                         the full example = IS DAVID A MAN? --- A and ? are optional
                  // Finished Option 1---------------------------------------------------
            + "|"
                  // Starting Option 2---------------------------------------------------
                  + Identifier.regex                  // group 1                 Identifier (ex. DAVID)
                  + " " + Relationship.IS_A.pattern() // not a capturing group   (ex. IS A) --- A is optional
                  + " " + Type.regex                  // group 2                 Type (ex. MAN)
                  + "\\?"                             // not a group             REQUIRED question mark at the end of the sentence
                     
                                                      //                         the full example = DAVID IS A MAN? --- A is optional, BUT NOT ?
                  // Finished Option 2---------------------------------------------------
            + ")"                                     // End of non-capturing group
         );
      
      public IsIdentifierType(List<String> strings)
      {
      
         this(new Identifier(strings.subList(0, 1)), new Type(strings.subList(1, 2)));
      
      }
      
      public String toString() {
         return cleanString(); }
    
   }
   
   //private record
   
   private final Map<Identifier, Set<QuantityType>> hasInstances = new HashMap<>();
   private final Map<Identifier, Set<Type>> isInstances = new HashMap<>();
   private final Map<FrequencyType, Set<QuantityType>> hasRules = new HashMap<>();
   private final Map<FrequencyType, Set<Type>> isRules = new HashMap<>();
   
   public RulesEngine()
   {
   
      SwingUtilities.invokeLater(() -> constructJFrame());
      
   }

   private void constructJFrame()
   {
   
      JFrame frame = new JFrame("Rules Engine");
      
      frame.setSize(500, 500);
      frame.setLocation(500, 200);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      JPanel panel = new JPanel();
      
      constructJPanel(panel);
      
      frame.add(panel);
      
      frame.setVisible(true);
   
   }
   
   private void constructJPanel(JPanel panel)
   {
   
      JButton clear = new JButton("Clear");
      final JTextField typingArea = new JTextField(20);
      final JTextArea displayArea = new JTextArea();
      JScrollPane scrollPane = new JScrollPane(displayArea);
      
      displayArea.setEditable(false);
      scrollPane.setPreferredSize(new Dimension(400, 400));   
      
      clear.addActionListener(event -> setText(typingArea, "", displayArea, ""));
      typingArea.addActionListener(event -> processText(typingArea, "", displayArea, typingArea.getText()));
       
      panel.add(typingArea, BorderLayout.PAGE_START);
      panel.add(scrollPane, BorderLayout.CENTER);
      panel.add(clear, BorderLayout.PAGE_END);
   
   }
   
   private void setText(JTextField typingArea, String newTypingAreaText, JTextArea displayArea, String newDisplayAreaText)
   {
   
      typingArea.setText(newTypingAreaText);
      displayArea.setText(newDisplayAreaText);
      typingArea.requestFocusInWindow();
      
      displayArea.setCaretPosition(0);
      //displayArea.setCaretPosition(displayArea.getDocument().getLength());
   
   }
   
   private void processText(JTextField typingArea, String newTypingAreaText, JTextArea displayArea, String newDisplayAreaText)
   {
   
      Optional<? extends Parseable> parseable = convertToParseable(newDisplayAreaText);
      
      if (parseable.isPresent())
      {
      
         String response = processParseable(parseable.orElseThrow()).toString();
         
         newDisplayAreaText += "\n\t" + response;
      
      }
      
      else
      {
      
         newDisplayAreaText += "\n\tINVALID FORMAT";
      
      }
   
      newDisplayAreaText += "\n" + displayArea.getText();
      setText(typingArea, newTypingAreaText, displayArea, newDisplayAreaText);
         
   }
   
   private Optional<? extends Parseable> convertToParseable(final String input)
   {
   
      final String text = input.trim().toUpperCase().replaceAll("\s+", " ");
      
      return ClassParser.parse(text);
      
   }

   private Response processParseable(Parseable parseable)
   {
   
      System.out.println(parseable);
   
      return switch (parseable)
               {
               
                  case Type t                               -> Response.NOT_YET_IMPLEMENTED;//processType(t);
                  case Quantity q                           -> Response.NOT_YET_IMPLEMENTED;
                  case QuantityType qt                      -> Response.NOT_YET_IMPLEMENTED;
                  case Identifier i                         -> Response.NOT_YET_IMPLEMENTED;
                  case IdentifierHasQuantityType ihqt       -> processIdentifierHasQuantityType(ihqt);
                  case IdentifierIsType iit                 -> processIdentifierIsType(iit);
                  case FrequencyType ft                     -> Response.NOT_YET_IMPLEMENTED;
                  case FrequencyTypeRelationship ftr        -> Response.NOT_YET_IMPLEMENTED;
                  case FrequencyTypeHasQuantityType fthqt   -> processFrequencyTypeHasQuantityType(fthqt);
                  case FrequencyTypeIsType ftit             -> processFrequencyTypeIsType(ftit);
                  case IsIdentifierType iit                 -> processIsIdentifierType(iit);
               
               };
         
      //throw new UnsupportedOperationException("Comment me out because jGRASP!");
   
   
   }
   
   private Response processIsIdentifierType(final IsIdentifierType isQuery)
   {
   
      final Identifier givenIdentifier = isQuery.identifier();
      final Type givenType = isQuery.type();
   
      if (!this.isInstances.containsKey(givenIdentifier))
      {
      
         return Response.UNKNOWN_IDENTIFIER;
      
      }
   
      {
      
         final Set<Type> allIsTypeValues =
            Stream.of(
                  flatMap(this.isInstances, Map::values, Function.identity()),
                  flatMap(this.isRules, Map::values, Function.identity())
               )
               .flatMap(Set::stream)
               .collect(Collectors.toUnmodifiableSet())
               ;
      
         if (!allIsTypeValues.contains(givenType))
         {
         
            final Set<Type> allHasTypeValues =
               Stream.of(
                     flatMap(this.hasInstances, Map::values, quantityType -> quantityType.type()),
                        flatMap(this.hasRules, Map::values, quantityType -> quantityType.type())
                     )
                     .flatMap(Set::stream)
                     .collect(Collectors.toUnmodifiableSet())
                     ;
         
            if (!allHasTypeValues.contains(givenType))
            {
            
               final Set<Type> allTypeKeys = 
                  Stream.of(
                     this.hasRules.keySet(),
                     this.isRules.keySet()
                  )
                  .flatMap(Set::stream)
                  .map(FrequencyType::type)
                  .collect(Collectors.toUnmodifiableSet())
                  ;
                  
               if (!allTypeKeys.contains(givenType))
               {
               
                  return Response.UNKNOWN_TYPE;
               
               }
            
            }
         
         }
      
      }
      
      final Set<Type> possibleTypes = this.isInstances.get(givenIdentifier);
         
      if (possibleTypes.contains(givenType))
      {
         
         return Response.CORRECT;
         
      }
         
      else
      {
         
         final Map<FrequencyType, Set<Type>> allIsRules = findAllIsRules();
            
         for (Type eachPossibleType : possibleTypes)
         {
            
            final FrequencyType everyX = new FrequencyType(Frequency.EVERY, eachPossibleType);
            final FrequencyType nonePossibleX = new FrequencyType(Frequency.NOT_A_SINGLE, eachPossibleType);
            final FrequencyType noneGivenX = new FrequencyType(Frequency.NOT_A_SINGLE, givenType);
               
            if (allIsRules.containsKey(everyX) && allIsRules.get(everyX).contains(givenType))
            {
               
               return Response.CORRECT;
               
            }
            
            else if (allIsRules.containsKey(nonePossibleX) && allIsRules.get(nonePossibleX).contains(givenType))
            {
            
               return Response.INCORRECT;
            
            }
            
            else if (allIsRules.containsKey(noneGivenX) && allIsRules.get(noneGivenX).contains(eachPossibleType))
            {
            
               return Response.INCORRECT;
            
            }
            
            
         }
         
      }
      
      return Response.POSSIBLY;
   
   }
   
   private Response processFrequencyTypeHasQuantityType(FrequencyTypeHasQuantityType hasRule)
   {
   
      for (Map.Entry<FrequencyType, Set<QuantityType>> eachEntry : hasRules.entrySet())
      {
      
         final FrequencyType givenFrequencyType = hasRule.frequencyType();
         
         final Map<Identifier, Set<QuantityType>> allHasInstances = findAllHasInstances();
         
         final Map<FrequencyType, Set<QuantityType>> allHasRules = findAllHasRules();
      
      }
   
      this.hasRules.merge(
         hasRule.frequencyType(),
         new HashSet<>(Arrays.asList(hasRule.quantityType())),
         RulesEngine::merge
         );
         
      return Response.OK;
   
   }

   private Response processFrequencyTypeIsType(FrequencyTypeIsType isRule)
   {
   
      this.isRules.merge(
         isRule.frequencyType(),
         new HashSet<>(Arrays.asList(isRule.type())),
         RulesEngine::merge
         );
   
      return Response.OK;
   
   }

   private Response processIdentifierHasQuantityType(IdentifierHasQuantityType hasInstance)
   {
   
      Set<QuantityType> quantityTypes = new HashSet<>(Arrays.asList(hasInstance.quantityType()));
      
      FrequencyType everyX = new FrequencyType(Frequency.EVERY, hasInstance.quantityType().type());
      
      if (this.hasRules.containsKey(everyX))
      {
      
         quantityTypes.addAll(this.hasRules.get(everyX));
      
      }
   
      this.hasInstances.merge(
         hasInstance.identifier(),
         quantityTypes,
         RulesEngine::merge
         );
   
      return Response.OK;
   
   }

   private Response processIdentifierIsType(IdentifierIsType isInstance)
   {
   
      final Set<Type> types = new HashSet<>(Arrays.asList(isInstance.type()));
      
      final FrequencyType everyX = new FrequencyType(Frequency.EVERY, isInstance.type());
   
      this.isInstances.merge(
         isInstance.identifier(),
         types,
         RulesEngine::merge
         );
   
      return Response.OK;
   
   }

   private Map<FrequencyType, Set<Type>> findAllIsRules()
   {
   
      Map<FrequencyType, Set<Type>> allIsRules = copyOf(this.isRules);
   
      for (Map.Entry<FrequencyType, Set<Type>> eachEntry : this.isRules.entrySet())
      {
      
         if (Frequency.EVERY.equals(eachEntry.getKey().frequency()))
         {
         
            Set<Type> types = findAllChildTypesOf(eachEntry.getKey().type());
            allIsRules.merge(
                  eachEntry.getKey(), 
                  types, 
                  RulesEngine::merge
               );
         
         }
      
      }
      
      return allIsRules;
   
   }
   
   private Set<Type> findAllChildTypesOf(Type type)
   {
   
      Set<Type> childTypes = new HashSet<>();
   
      for (Map.Entry<FrequencyType, Set<Type>> eachEntry : this.isRules.entrySet())
      {
      
         FrequencyType everyX = new FrequencyType(Frequency.EVERY, type);
      
         if (eachEntry.getKey().equals(everyX))
         {
         
            for (Type eachType : eachEntry.getValue())
            {
            
               if (!childTypes.contains(eachType))
               {
               
                  childTypes.addAll(findAllChildTypesOf(eachType));
                  childTypes.add(eachType);
               
               }
            
            }
         
         }
      
      }
      
      return childTypes;
   
   }

   private Map<Identifier, Set<Type>> findAllIsInstances()
   {
   
      Map<Identifier, Set<Type>> allIsInstances = copyOf(this.isInstances);
      Map<FrequencyType, Set<Type>> allIsRules = findAllIsRules();
      
      for (Map.Entry<Identifier, Set<Type>> eachIsInstance : this.isInstances.entrySet())
      {
      
         for (Type eachType : eachIsInstance.getValue())
         {
         
            for (Map.Entry<FrequencyType, Set<Type>> eachIsRule : allIsRules.entrySet())
            {
            
               if (eachType.equals(eachIsRule.getKey().type()))
               {
               
                  allIsInstances.merge(
                        eachIsInstance.getKey(),
                        eachIsRule.getValue(),
                        RulesEngine::merge
                     );
               
               }
            
            }
         
         }
      
      }
      
      return allIsInstances;
   
   }

   private Map<FrequencyType, Set<QuantityType>> findAllHasRules()
   {
   
      Map<FrequencyType, Set<QuantityType>> allHasRules = copyOf(this.hasRules);
      Map<FrequencyType, Set<Type>> allIsRules = this.findAllIsRules();
      
      for (Map.Entry<FrequencyType, Set<QuantityType>> eachHasRule : this.hasRules.entrySet())
      {
      
         for (QuantityType eachQuantityType : eachHasRule.getValue())
         {
         
            for (Map.Entry<FrequencyType, Set<Type>> eachIsRule : allIsRules.entrySet())
            {
            
            //if ()
            
            }
         
         }
      
      }
   
      return allHasRules;
   
   }

   private Set<QuantityType> findAllOwnedTypesOf(QuantityType quantityType)
   {
   
      Set<QuantityType> ownedTypes = new HashSet<>();
   
      for (Map.Entry<FrequencyType, Set<QuantityType>> eachEntry : this.hasRules.entrySet())
      {
      
         FrequencyType everyX = new FrequencyType(Frequency.EVERY, quantityType.type());
         
         if (eachEntry.getKey().equals(everyX))
         {
         
            for (QuantityType eachQuantityType : eachEntry.getValue())
            {
            
               for (QuantityType eachResultQuantityType : findAllOwnedTypesOf(eachQuantityType))
               {
               
                  ownedTypes.add(
                        new QuantityType(
                           new Quantity(
                              eachResultQuantityType.quantity().count() * quantityType.quantity().count()
                           ),
                           eachResultQuantityType.type()
                        )
                     );
               
               }
            
            }
         
         }
      
      }
      
      return ownedTypes;
   
   }

   private Map<Identifier, Set<QuantityType>> findAllHasInstances()
   {
   
      Map<Identifier, Set<QuantityType>> allHasInstances = this.hasInstances;
      
      for (Map.Entry<Identifier, Set<QuantityType>> eachEntry : this.hasInstances.entrySet())
      {
      
         Map<Identifier, Set<Type>> allIsInstances = this.findAllIsInstances();
      
      }
   
      return allHasInstances;
   
   }

   private static <E> String convertToPatternGroup(E[] values)
   {
      
      return Arrays.toString(values)
                  .replace('[', '(')
                  .replace(']', ')')
                  .replace('_', ' ')
                  .replaceAll(", ", "|")
                  ;
      
   }

   private static <T extends Parseable> Optional<T> parse(Matcher matcher, Class<T> clazz)
   {
   
      if (matcher.matches())
      {
      
         List<String> matches = new ArrayList<>();
      
         for (int i = 1; i < matcher.groupCount(); i++)
         {
         
            matches.add(matcher.group(i));
         
         }
      
         try
         {
         
            return Optional.of(clazz.getConstructor(matches.getClass()).newInstance(matches));
         
         }
         
         catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception)
         {
         
            throw new IllegalStateException(exception);
         
         }
      
      }
      
      throw new IllegalStateException();
   
   }
   
   private static <K, V> Map<K, Set<V>> copyOf(Map<K, Set<V>> oldMap)
   {
   
      Map<K, Set<V>> newMap = new HashMap<>();
      
      for (Map.Entry<K, Set<V>> eachEntry : oldMap.entrySet())
      {
      
         newMap.put(eachEntry.getKey(), new HashSet<>(eachEntry.getValue()));
      
      }
      
      return newMap;
   
   }
   
   private static <T> Set<T> merge(Set<T> oldSet, Set<T> newSet)
   {
   
      oldSet.addAll(newSet);
      return oldSet;
   
   }

   private static <K, V, O, A> Set<V> flatMap(Map<K, Set<O>> map, Function<Map<K, Set<O>>, Collection<Set<O>>> mapPortion, Function<O, V> converter)
   {
   
      return 
         mapPortion.apply(map).stream()
            .flatMap(Set::stream)
            .map(converter::apply)
            .collect(Collectors.toCollection(HashSet::new))
            ;
   
   }

   public static void main(String[] args)
   {
   
      new RulesEngine();
   
   }

}
