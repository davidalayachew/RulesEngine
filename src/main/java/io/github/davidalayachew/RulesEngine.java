package io.github.davidalayachew;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
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

   private interface Request {}

   private enum Frequency
   {
   
      EVERY,
      SOME,
      NONE,
      ;
   
   }

   private enum Relationship
   {
   
      HAS,  //Frequency Type has Quantity Type
      IS,   //Instance is Type
      ;
   
   }

   private record Type(String name, Set<Quantity> has, Set<Type> is)
   {
    
      Type(String name) { this(name, new HashSet<>(), new HashSet<>()); } 
      
   }

   private record Quantity(long count, Type type) {}

   private record Instance(String name, Type type) implements Request
   {
   
      public static final Pattern instanceRegex = Pattern.compile("([a-zA-Z]*) " + Relationship.IS + " ([a-zA-Z]*)");
      
      static Optional<Instance> of(String text)
      {
      
         Matcher match = instanceRegex.matcher(text);
      
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
      
      static boolean matches(String text)
      {
      
         return instanceRegex.matcher(text).matches();
      
      }
   
   }

   private record Rule(Frequency frequency, Type type, Quantity quantity) implements Request
   {
   
      public static final Pattern ruleRegex = Pattern.compile(asString(Frequency.class) + " ([a-zA-Z]*) " + Relationship.HAS + " (\\d{1,7}) ([a-zA-Z]*)");
      
      static Optional<Rule> of(String text)
      {
      
         Matcher match = ruleRegex.matcher(text);
      
         if (match.matches())
         {
         
            Frequency frequency = Frequency.valueOf(match.group(1));
            Type type = new Type(match.group(2));
            Quantity quantity = new Quantity(Long.parseLong(match.group(3)), new Type(match.group(4)));
            
            //SHOULD WE NOT GIVE TYPE THE NEW ATTRIBUTE THAT WE JUST FOUND?
            
            return Optional.of(new Rule(frequency, type, quantity));
         
         }
         
         else
         {
         
            return Optional.empty();
         
         }
      
      }
      
      static boolean matches(String text)
      {
      
         return ruleRegex.matcher(text).matches();
      
      }
   
   }

   private record Query() implements Request {}

   private static void constructJPanel(JPanel panel)
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
   
   private static void setText(JTextField typingArea, String typingAreaText, JTextArea displayArea, String displayAreaText)
   {
   
      typingArea.setText(typingAreaText);
      displayArea.setText(displayAreaText);
      typingArea.requestFocusInWindow();
   
   }
   
   private static void processText(JTextField typingArea, String typingAreaText, JTextArea displayArea, String displayAreaText)
   {
   
      displayAreaText += "\n" + processText(displayAreaText);
      displayAreaText += "\n" + displayArea.getText();
      setText(typingArea, typingAreaText, displayArea, displayAreaText);
   
   }
   
   private static Optional<? extends Request> processText(final String input)
   {
   
      final String text = input.trim().toUpperCase().replaceAll("\s+", " ");
      
      if (Rule.matches(text))
      {
      
         return Rule.of(text);
      
      }
      
      else if (Instance.matches(text))
      {
      
         return Instance.of(text);
      
      }
      
      else
      {
      
         return Optional.empty();
      
      }
      
   }

   public static void constructJFrame(JFrame frame)
   {
   
      frame.setSize(500, 500);
      frame.setLocation(500, 200);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      JPanel panel = new JPanel();
      
      constructJPanel(panel);
      
      frame.add(panel);
      
   }
   
   static <T extends Enum<T>> String asString(Class<T> clazz)
   {
      
      String values = Arrays.asList(clazz.getEnumConstants()).toString().toUpperCase();
         
      return values
                  .replace('[', '(')
                  .replace(']', ')')
                  .replaceAll(", ", "|")
                  ;
      
   }
   
   public static void main(String[] args)
   {
   
      JFrame frame = new JFrame("Rules Engine");
      
      constructJFrame(frame);
      
      frame.setVisible(true);
   
   }

}