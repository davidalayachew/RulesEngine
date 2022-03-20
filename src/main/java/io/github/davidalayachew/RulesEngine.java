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

   private sealed interface Parseable permits Type, Quantity, QuantityType, Instance, FrequencyType, Rule {}
   
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
      IS_A,   //Instance is Type
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = convertToPatternGroup(values());
      
      public String pattern()
      {
      
         return this.name().replace('_', ' ');
      
      }
      
   }

   private enum Response
   {
   
      OK,
      ALREADY_EXISTS,
      OVERWROTE_PREVIOUS_VALUE,
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
            
               Constructor<? extends Parseable> constructor = temp.getConstructor(List.class);
               Parseable godForgiveMe = constructor.newInstance(
                     each.getCanonicalName().contains("Quantity")
                     ? List.of("1", "1")
                     : List.of("EVERY", "1", "1", "1")
                  );
               
               
               switch (godForgiveMe)
               {
               
                  case Type t             -> map.put(Type.regex, Type::new);
                  case Quantity q         -> map.put(Quantity.regex, Quantity::new);
                  case QuantityType qt    -> map.put(QuantityType.regex, QuantityType::new);
                  case Instance i         -> map.put(Instance.regex, Instance::new);
                  case FrequencyType ft   -> map.put(FrequencyType.regex, FrequencyType::new);
                  case Rule r             -> map.put(Rule.regex, Rule::new);
               
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
   
      public static final Pattern regex = Pattern.compile("([a-zA-Z]*)");
   
      public Type(List<String> string)
      {
      
         this(string.get(0));
      
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

   private record Instance(String name, Type type) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            "([a-zA-Z]*) "                //group 1
                                                            + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                            + " " + Type.regex            //group 2
                                                            );
      
      public Instance(List<String> strings)
      {
      
         this(strings.get(0), new Type(strings.subList(1, 2)));
      
      }
      
   }

   private record Rule(FrequencyType frequencyType, QuantityType quantityType) implements Parseable
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            FrequencyType.regex           //group 1 and 2
                                                            + " " + Relationship.HAS      //not a group because no (), so just a pattern
                                                            + " " + QuantityType.regex    //group 3 and 4
                                                            );
      
      public Rule(List<String> strings)
      {
      
         this(new FrequencyType(strings.subList(0, 2)), new QuantityType(strings.subList(2, 4)));
      
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

   private record Query() {}
   
   private final Collection<Type> types = new HashSet<>();
   private final Map<String, Set<Type>> instances = new HashMap<>();
//    private final Map<FrequencyType, Boolean>
   private final Collection<Rule> rules = new HashSet<>();
   
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
   
      if (parseable instanceof Rule r)
      { 
         
         return processRule(r);
      
      }
      
      else if (parseable instanceof Instance i)
      {
      
         return processInstance(i);
      
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
   
   private Response processRule(Rule rule)
   {
   
      boolean valid = switch (rule.frequencyType().frequency())
         {
         
            case EVERY -> true;
            case NOT_EVERY -> true;
            case NONE -> true;
         
         };
   
      this.rules.add(rule);
   
      return Response.OK;
   
   }

   private Response processInstance(Instance instance)
   {
   
      System.out.println(instance);
   
      processType(instance.type());
   
      this.instances.merge(
         instance.name(),
         new HashSet<>(Arrays.asList(instance.type())),
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