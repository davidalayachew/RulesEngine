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
import java.util.stream.IntStream;

public class RulesEngine
{

   private sealed interface Parseable
   permits Type, Identifier, IdentifierIsType, IdentifierHasQuantityType, Quantity, QuantityType, FrequencyType, FrequencyTypeHasQuantityType, FrequencyTypeIsType, FrequencyTypeRelationship {}
   
   private interface Regex {}
   
   private enum Frequency
   {
   
      EVERY,
      NOT_EVERY,
      NONE,
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = convertToPatternGroup(values());
   }

   private enum Relationship
   {
   
      HAS,  //Frequency Type has Quantity Type
      IS,   //Instance is Type
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = convertToPatternGroup(values());
      
      public String pattern()
      {
      
         return switch (this)
            {
            
               case HAS    -> this.name();
               case IS     -> "(?:" + this.name() + "|" + this.name() + " A)";
            
            };
      
      }
      
   }

   private enum Response
   {
   
      OK,
      ALREADY_EXISTS,
      OVERWROTE_PREVIOUS_VALUE,
      NOT_YET_IMPLEMENTED,
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
                     case "FrequencyTypeRelationship"    -> List.of("EVERY", "A", "IS");
                     case "FrequencyTypeHasQuantityType" -> List.of("EVERY", "A", "1", "A");
                     case "FrequencyTypeIsType"          -> List.of("EVERY", "A", "A");
                     default                             -> Collections.emptyList();
                  
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
               
               }
            
            
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

   private record Type(String name) implements Parseable
   {
   
      public static final Pattern regex = Pattern.compile("([a-zA-Z]+[a-zA-Z0-9]*)");
   
      public Type(List<String> string)
      {
      
         this(string.get(0));
      
      }
      
   }

   private record Identifier(String name) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile("([a-zA-Z]+[a-zA-Z0-9]*)");
   
      public Identifier(List<String> strings)
      {
      
         this(strings.get(0));
      
      }
   
   }

   private record Quantity(long count) implements Parseable
   {
   
      public static final Pattern regex = Pattern.compile("(\\d{1,7})");
      
      public Quantity(List<String> strings)
      {
      
         this(Long.parseLong(strings.get(0)));
      
      }
   
   }

   private record QuantityType(Quantity quantity, Type type) implements Parseable
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
   
   }

   private record IdentifierHasQuantityType(Identifier identifier, QuantityType quantityType) implements Parseable
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
      
   }

   private record IdentifierIsType(Identifier identifier, Type type) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            Identifier.regex                    //group 1
                                                            + " " + Relationship.IS.pattern()   //not a group because no (), so just a pattern
                                                            + " " + Type.regex                  //group 2
                                                            );
      
      public IdentifierIsType(List<String> strings)
      {
      
         this(new Identifier(strings.subList(0, 1)), new Type(strings.subList(1, 2)));
      
      }
      
   }

   private record FrequencyType(Frequency frequency, Type type) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            Frequency.regex               //group 1
                                                            + " " + Type.regex            //group 2
                                                            );
      
      public FrequencyType(List<String> strings)
      {
      
         this(Frequency.valueOf(strings.get(0)), new Type(strings.subList(1, 2)));
      
      }
      
   }

   private record FrequencyTypeRelationship(FrequencyType frequencyType, Relationship relationship) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex           //group 1 and 2
                                                            + " " + Relationship.regex    //group 3
                                                            );
      
      public FrequencyTypeRelationship(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), Relationship.valueOf(strings.get(2)));
      
      }
      
   }

   private record FrequencyTypeHasQuantityType(FrequencyType frequencyType, QuantityType quantityType) implements Parseable
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
    
   }

   private record FrequencyTypeIsType(FrequencyType frequencyType, Type type) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex                 //group 1 and 2
                                                            + " " + Relationship.IS.pattern()   //not a group because no (), so just a pattern
                                                            + " " + Type.regex                  //group 3
                                                            );
      
      public FrequencyTypeIsType(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), new Type(strings.subList(2, 3)));
      
      }
    
   }

   private record Query() {}
   
   private final Collection<Type> types = new HashSet<>();
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
      
         Response response = processParseable(parseable.orElseThrow());
         
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
   
      if (parseable instanceof Type t)
      {
      
      return Response.NOT_YET_IMPLEMENTED;
      
      }
      
      else if (parseable instanceof Identifier i)
      {
      
      return Response.NOT_YET_IMPLEMENTED;
      
      }
      
      else if (parseable instanceof FrequencyTypeHasQuantityType hr)
      { 
         
         return processFrequencyTypeHasQuantityType(hr);
      
      }
      
      else if (parseable instanceof FrequencyTypeIsType hr)
      { 
         
         return processFrequencyTypeIsType(hr);
      
      }
      
      else if (parseable instanceof IdentifierHasQuantityType hi)
      {
      
         return processIdentifierHasQuantityType(hi);
      
      }
      
      else if (parseable instanceof IdentifierIsType ii)
      {
      
         return processIdentifierIsType(ii);
      
      }
      
      else if (parseable instanceof Type t)
      {
         
         return processType(t);
         
      }
      
      else
      {
      
         throw new IllegalArgumentException("Invalid type");
      
      }
   
   }
   
   private Response processFrequencyTypeHasQuantityType(FrequencyTypeHasQuantityType hasRule)
   {
   
      this.hasRules.merge(
         hasRule.frequencyType(),
         new HashSet<>(Arrays.asList(hasRule.quantityType())),
         (oldSet, newSet) -> {oldSet.addAll(newSet);return oldSet;}
         );
         
      return Response.OK;
   
   }

   private Response processFrequencyTypeIsType(FrequencyTypeIsType isRule)
   {
   
      this.isRules.merge(
         isRule.frequencyType(),
         new HashSet<>(Arrays.asList(isRule.type())),
         (oldSet, newSet) -> {oldSet.addAll(newSet);return oldSet;}
         );
   
      return Response.OK;
   
   }

   private Response processIdentifierHasQuantityType(IdentifierHasQuantityType hasInstance)
   {
   
      processType(hasInstance.quantityType().type());
      
      Set<QuantityType> quantityTypes = new HashSet<>(Arrays.asList(hasInstance.quantityType()));
      
      FrequencyType everyX = new FrequencyType(Frequency.EVERY, hasInstance.quantityType().type());
      
      if (this.hasRules.containsKey(everyX))
      {
      
         quantityTypes.addAll(this.hasRules.get(everyX));
      
      }
   
      this.hasInstances.merge(
         hasInstance.identifier(),
         quantityTypes,
         (oldSet, newSet) -> {oldSet.addAll(newSet);return oldSet;}
         );
   
      return Response.OK;
   
   }

   private Response processIdentifierIsType(IdentifierIsType isInstance)
   {
   
      processType(isInstance.type());
      
      Set<Type> types = new HashSet<>(Arrays.asList(isInstance.type()));
      
      FrequencyType everyX = new FrequencyType(Frequency.EVERY, isInstance.type());
      
      if (this.isRules.containsKey(everyX))
      {
      
         types.addAll(this.isRules.get(everyX));
      
      }
   
      this.isInstances.merge(
         isInstance.identifier(),
         types,
         (oldSet, newSet) -> {oldSet.addAll(newSet);return oldSet;}
         );
   
      return Response.OK;
   
   }

   private Response processType(Type type)
   {
   
      if (this.types.contains(type))
      {
      
         return Response.ALREADY_EXISTS;
      
      }
      
      else
      {
      
         this.types.add(type);
         
         return Response.OK;
      
      }
   
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
   
   public static void main(String[] args)
   {
   
      new RulesEngine();
   
   }

}