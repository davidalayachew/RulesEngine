package io.github.davidalayachew;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class RulesEngine
{

   private sealed interface Request permits Type, Instance, Rule {}
   
   private enum Frequency
   {
   
      EVERY,
      SOME,
      NONE,
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = Arrays.toString(values())
                  .replace('[', '(')
                  .replace(']', ')')
                  .replaceAll(", ", "|")
                  ;
      
   }

   private enum Relationship
   {
   
      HAS,  //Frequency Type has Quantity Type
      IS,   //Instance is Type
      ;
   
      //turns [A, B, C] into (A|B|C)
      //useful in Java's Pattern class, when you want a pattern capturing group for an enum
      
      public static final String regex = Arrays.toString(values())
                  .replace('[', '(')
                  .replace(']', ')')
                  .replaceAll(", ", "|")
                  ;
      
   }

   private enum Response
   {
   
      OK,
      ALREADY_EXISTS,
      ;
   
   }

   private record Type(String name) implements Request
   {
   
      public static final Pattern regex = Pattern.compile("([a-zA-Z]*)");
      
      public static Optional<Type> of(String text)
      {
      
         if (isParseable(text))
         {
         
            Matcher match = regex.matcher(text);
            match.matches();
         
            return Optional.of(new Type(match.group(1)));
         
         }
         
         else
         {
         
            return Optional.empty();
         
         }
         
      }
   
      public static boolean isParseable(String text)
      {
      
         return 
            text != null
            && !text.isBlank()
            && regex.matcher(text).matches()
            ;
         
      }
    
   }

   private record Quantity(long count, Type type)
   {
   
      public static final Pattern regex = Pattern.compile("(\\d{1,7}) ([a-zA-Z]*)");
      
      public static Optional<Quantity> of(String text)
      {
      
         if (isParseable(text))
         {
         
            Matcher match = regex.matcher(text);
            match.matches();
         
            long count = Long.parseLong(match.group(1));
            Type type = Type.of(match.group(2)).orElseThrow();
            
            return Optional.of(new Quantity(count, type));
         
         }
         
         else
         {
         
            return Optional.empty();
         
         }
         
      }
   
      public static boolean isParseable(String text)
      {
      
         return 
            text != null
            && !text.isBlank()
            && regex.matcher(text).matches()
            ;
         
      }
    
   }

   private record Instance(String name, Type type) implements Request
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            "([a-zA-Z]*) "                //group 1
                                                            + Relationship.IS             //not a group because no (), so just a pattern
                                                            + " " + Type.regex            //group 2
                                                            );
      
      static final Optional<Instance> of(String text)
      {
      
         Matcher match = regex.matcher(text);
         match.matches();
      
         if (match.matches())
         {
         
            String name = match.group(1);
            Type type = new Type(match.group(2));
            
            return Optional.of(new Instance(name, type));
         
         }
         
         else
         {
         
            return Optional.empty();
         
         }
      
      }
      
      public static boolean isParseable(String text)
      {
      
         return 
            text != null
            && !text.isBlank()
            && regex.matcher(text).matches()
            ;
         
      }
    
   }

   private record Rule(Frequency frequency, Type type, Quantity quantity) implements Request
   {
   
      private static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                            Frequency.regex               //group 1
                                                            + " " + Type.regex            //group 2
                                                            + " " + Relationship.HAS      //not a group because no (), so just a pattern
                                                            + " " + Quantity.regex        //group 3 and 4
                                                            );
      
      public static Optional<Rule> of(String text)
      {
      
         if (isParseable(text))
         {
         
            Matcher match = regex.matcher(text);
            match.matches();
         
            List<String> values = List.of(match.group(1), match.group(2), match.group(3), match.group(4));
         
            Frequency frequency = Frequency.valueOf(match.group(1));
            Type type = Type.of(match.group(2)).orElseThrow();
            Quantity quantity = Quantity.of(match.group(3) + " " + match.group(4)).orElseThrow();
            
            return Optional.of(new Rule(frequency, type, quantity));
         
         }
         
         else
         {
         
            return Optional.empty();
         
         }
         
      }
      
      public static boolean isParseable(String text)
      {
      
         return 
            text != null
            && !text.isBlank()
            && regex.matcher(text).matches()
            ;
         
      }
    
   }

   private record Query() {}
   
   private final Collection<Type> types = new HashSet<>();
   private final Collection<Instance> instances = new HashSet<>();
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
   
   }
   
   private void processText(JTextField typingArea, String newTypingAreaText, JTextArea displayArea, String newDisplayAreaText)
   {
   
      Optional<? extends Request> request = convertToRequest(newDisplayAreaText);
      
      Response response = processRequest(request.orElseThrow());
      
      newDisplayAreaText += "\n\t" + response;
      newDisplayAreaText += "\n" + displayArea.getText();
      setText(typingArea, newTypingAreaText, displayArea, newDisplayAreaText);
   
   }
   
   private Optional<? extends Request> convertToRequest(final String input)
   {
   
      final String text = input.trim().toUpperCase().replaceAll("\s+", " ");
      
      Optional<Rule> rule = Rule.of(text);
      Optional<Instance> instance = Instance.of(text);
      Optional<Type> type = Type.of(text);
      
      if (rule.isPresent())
      {
      
         return rule;
      
      }
      
      else if (instance.isPresent())
      {
      
         return instance;
      
      }
      
      else if (type.isPresent())
      {
      
         return type;
      
      }
      
      else
      {
      
         return Optional.empty();
      
      }
      
   }

   private Response processRequest(Request request)
   {
   
      Response response;
      
      if (request instanceof Rule r)
      {
      
         response = processRule(r);
      
      }
      
      else if (request instanceof Instance i)
      {
      
         response = processInstance(i);
      
      }
      
      else if (request instanceof Type t)
      {
      
         response = processType(t);
      
      }
      
      else
      {
      
         throw new IllegalStateException();
      
      }
   
      return response;
   
   }
   
   private Response processRule(Rule rule)
   {
   
      return null;
   
   }

   private Response processInstance(Instance instance)
   {
   
      return null;
   
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

   public static void main(String[] args)
   {
   
      new RulesEngine();
   
   }

}