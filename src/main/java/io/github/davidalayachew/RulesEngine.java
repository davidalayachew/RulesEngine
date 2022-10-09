package io.github.davidalayachew;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RulesEngine
{

   private sealed interface Parseable
         permits
            Identifier,
            Type,
            IdentifierIsAType,
            IdentifierHasQuantityType,
            Quantity,
            QuantityType,
            FrequencyType,
            FrequencyTypeHasQuantityType,
            FrequencyTypeIsType,
            FrequencyTypeRelationship,
            IsIdentifierAType
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
            
               for (int i = 0; i < components.length; i++)
               {
               
                  var each = components[i];
               
                  output += (i == 0 ? "" : " ") + each.getAccessor().invoke(this);
               
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
               case IS_A   -> this.name().replace("_", " ").replace(" A", "(?: A(?:N|)|)");
            
            };
      
      }
   
   }

   private enum Response
   {
   
      OK,
      NOT_YET_IMPLEMENTED,
      CORRECT,
      POSSIBLY,
      NEED_MORE_INFO,
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

   private static final class ClassParser
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
   
      private static final Optional<? extends Parseable> parse(String text)
      {
      
         for (Map.Entry<Pattern, Function<List<String>, Parseable>> each : map.entrySet())
         {
         
            Pattern regex = each.getKey();
         
            Matcher match = regex.matcher(text);
         
            if (match.matches())
            {
            
               return Optional.of(each.getValue().apply(RulesEngine.fetchGroups(match)));
            
            }
         
         }
      
         return Optional.empty();
      
      }
   
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

   private record Type(String name) implements Parseable, ToString
   {
   
      public static final Pattern regex = Pattern.compile("([a-zA-Z]+[a-zA-Z0-9]*)");
   
      public Type(List<String> strings)
      {
      
         this(strings.get(0));
      
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

   private record IdentifierIsAType(Identifier identifier, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                            Identifier.regex                    //group 1
                                                            + " " + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                            + " " + Type.regex                  //group 2
                                                            );
   
      public IdentifierIsAType(List<String> strings)
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

   private record IsIdentifierAType(Identifier identifier, Type type) implements Parseable, ToString
   {
   
      private static final Pattern regex =   // Unfortunately, I don't (currently) see a way to make this easy or neat or clean
         Pattern.compile(
            "(?:"                                     // Beginning of non-capturing group
                  // Starting Option 1---------------------------------------------------
                  + "IS"                              // not a group             IS
                  + " " + Identifier.regex            // group 1                 Identifier (ex. DAVID)
                  + "(?: A(?:N|)|)"                   // not a capturing group   Optional A or AN
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
   
      public IsIdentifierAType(List<String> strings)
      {
      
         this(new Identifier(strings.subList(0, 1)), new Type(strings.subList(1, 2)));
      
      }
   
      public String toString() {
         return cleanString(); }
   
   }

   //private record

   private final Map<Identifier,    Set<Type>         >  isInstances    = new HashMap<>();
   private final Map<FrequencyType, Set<Type>         >  isRules        = new HashMap<>();
   private final Map<Identifier,    Set<QuantityType> >  hasInstances   = new HashMap<>();
   private final Map<FrequencyType, Set<QuantityType> >  hasRules       = new HashMap<>();

   public RulesEngine()
   {
   
      SwingUtilities.invokeLater(() -> constructJFrame());
   
   }

   private void constructJFrame()
   {
   
      JFrame frame = new JFrame("Rules Engine");
   
      frame.setSize(600, 500);
      frame.setLocation(500, 200);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   
      JPanel panel = new JPanel(new GridLayout());
   
      constructJPanel(panel);
   
      frame.add(panel);
   
      frame.setVisible(true);
   
   }

   private void constructJPanel(final JPanel panel)
   {
   
      final DefaultListModel<Identifier> identifiersModel = new DefaultListModel<>();
      final DefaultListModel<Type> typesModel = new DefaultListModel<>();
   
      final JList<Identifier> identifiersList = new JList<>(identifiersModel);
      final JList<Type> typesList = new JList<>(typesModel);
   
      final JTextField typingArea = new JTextField();
      final JTextArea displayArea = new JTextArea();
      final JScrollPane displayAreaScrollPane = new JScrollPane(displayArea);
   
      typingArea.setText("Enter your text here!");
      typingArea.addMouseListener(
         new MouseAdapter()
         {
         
            public void mouseClicked(MouseEvent event)
            {
            
               typingArea.selectAll();
            
            }
         
         });
   
      displayArea.setEditable(false);
      displayArea.setTabSize(4);
   
      record IdentifierType(List<Identifier> identifiers, List<Type> types) {}
   
      final Consumer<Optional<? extends Parseable>> populateLists =
         potential ->
         {
         
            if (potential.isPresent())
            {
            
               final IdentifierType identifierType =
                  switch (potential.get())
                  {
                  
                     case Type t -> new IdentifierType(List.of(), List.of(t));
                     case Identifier i -> new IdentifierType(List.of(i), List.of());
                     case IdentifierIsAType iiat ->
                                 new IdentifierType(List.of(iiat.identifier()), List.of(iiat.type()));
                  
                     case IdentifierHasQuantityType ihqt ->
                           new IdentifierType(List.of(ihqt.identifier()), List.of(ihqt.quantityType().type()));
                  
                     case Quantity q -> new IdentifierType(List.of(), List.of());
                     case QuantityType qt -> new IdentifierType(List.of(), List.of(qt.type()));
                     case FrequencyType ft -> new IdentifierType(List.of(), List.of(ft.type()));
                     case FrequencyTypeHasQuantityType fthqt ->
                           new IdentifierType(List.of(),
                              List.of(
                                 fthqt.frequencyType().type(),
                                 fthqt.quantityType().type()));
                  
                     case FrequencyTypeIsType ftit ->
                           new IdentifierType(List.of(), List.of(ftit.frequencyType().type(), ftit.type()));
                  
                     case FrequencyTypeRelationship ftr ->
                           new IdentifierType(List.of(), List.of(ftr.frequencyType().type()));
                  
                     case IsIdentifierAType iiat ->
                           //I would like to add these to the list, but that would be misleading
                           //new IdentifierType(List.of(iiat.identifier()), List.of(iiat.type()));
                           new IdentifierType(List.of(), List.of());
                  
                  };
            
               for (Identifier each : identifierType.identifiers())
               {
               
                  if (!identifiersModel.contains(each))
                  {
                  
                     identifiersModel.addElement(each);
                  
                  }
               
               }
            
               for (Type each : identifierType.types())
               {
               
                  if (!typesModel.contains(each))
                  {
                  
                     typesModel.addElement(each);
                  
                  }
               
               }
            
            }
         
            panel.revalidate();
         
         };
   
      typingArea.addActionListener(
            event -> populateLists.accept(processText(typingArea, "", displayArea, typingArea.getText()))
         );
   
      final JButton clear = new JButton("Clear Logs");
      clear.addActionListener(event -> setText(typingArea, "", displayArea, ""));
   
      final JPanel buttonPanel = new JPanel(new GridLayout(1, 0));
      buttonPanel.add(clear);
   
      final Supplier<JPanel> identifiers =
         () ->
         {
         
            identifiersList.setBackground(Color.DARK_GRAY);
            identifiersList.setForeground(Color.WHITE);
         
            final JLabel identifiersLabel = new JLabel("IDENTIFIERS");
            identifiersLabel.setHorizontalAlignment(SwingConstants.CENTER);
            identifiersLabel.setBackground(Color.GRAY);
            identifiersLabel.setForeground(Color.WHITE);
            identifiersLabel.setOpaque(true);
         
            final JPanel identifiersPanel = new JPanel(new BorderLayout());
         
            identifiersPanel.add(identifiersLabel, BorderLayout.PAGE_START);
            identifiersPanel.add(new JScrollPane(identifiersList), BorderLayout.CENTER);
         
            return identifiersPanel;
         
         };
   
      final Supplier<JPanel> types =
         () ->
         {
         
            typesList.setBackground(Color.DARK_GRAY);
            typesList.setForeground(Color.WHITE);
         
            final JLabel typesLabel = new JLabel("TYPES");
            typesLabel.setHorizontalAlignment(SwingConstants.CENTER);
            typesLabel.setBackground(Color.GRAY);
            typesLabel.setForeground(Color.WHITE);
            typesLabel.setOpaque(true);
         
            final JPanel typesPanel = new JPanel(new BorderLayout());
         
            typesPanel.add(typesLabel, BorderLayout.PAGE_START);
            typesPanel.add(new JScrollPane(typesList), BorderLayout.CENTER);
         
            return typesPanel;
         
         };
   
      final Supplier<JPanel> ioPanel =
            () ->
            {
            
               displayArea.setBackground(Color.DARK_GRAY);
               displayArea.setForeground(Color.WHITE);
            
               final JPanel innerPanel = new JPanel(new BorderLayout());
            
               innerPanel.setMinimumSize(new Dimension(50, 50));
            
               innerPanel.add(typingArea, BorderLayout.PAGE_START);
               innerPanel.add(displayAreaScrollPane, BorderLayout.CENTER);
               innerPanel.add(buttonPanel, BorderLayout.PAGE_END);
            
               return innerPanel;
            
            };
   
      panel.add(identifiers.get());
      panel.add(ioPanel.get());
      panel.add(types.get());
   
   }

   private void setText(JTextField typingArea, String newTypingAreaText, JTextArea displayArea, String newDisplayAreaText)
   {
   
      typingArea.setText(newTypingAreaText);
      displayArea.setText(newDisplayAreaText);
      typingArea.requestFocusInWindow();
   
      displayArea.setCaretPosition(0);
      //displayArea.setCaretPosition(displayArea.getDocument().getLength());
   
   }

   private Optional<? extends Parseable> processText(JTextField typingArea, String newTypingAreaText, JTextArea displayArea, String newDisplayAreaText)
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
   
      return parseable;
   
   }

   private Optional<? extends Parseable> convertToParseable(final String input)
   {
   
      final String text = input.trim().toUpperCase().replaceAll("\s+", " ");
   
      return ClassParser.parse(text);
   
   }

   private Response processParseable(Parseable parseable)
   {
   
      System.out.println(parseable);
   
      return
         switch (parseable)
         {
         
            case Type t                               -> Response.NOT_YET_IMPLEMENTED;//processType(t);
            case Quantity q                           -> Response.NOT_YET_IMPLEMENTED;
            case QuantityType qt                      -> Response.NOT_YET_IMPLEMENTED;
            case Identifier i                         -> Response.NOT_YET_IMPLEMENTED;
            case IdentifierHasQuantityType ihqt       -> processIdentifierHasQuantityType(ihqt);
            case IdentifierIsAType iiat               -> processIdentifierIsAType(iiat);
            case FrequencyType ft                     -> Response.NOT_YET_IMPLEMENTED;
            case FrequencyTypeRelationship ftr        -> Response.NOT_YET_IMPLEMENTED;
            case FrequencyTypeHasQuantityType fthqt   -> processFrequencyTypeHasQuantityType(fthqt);
            case FrequencyTypeIsType ftit             -> processFrequencyTypeIsType(ftit);
            case IsIdentifierAType iiat               -> processIsIdentifierAType(iiat);
         
         };
   
   }

   private Response processIsIdentifierAType(final IsIdentifierAType isQuery)
   {
   
   return Response.NOT_YET_IMPLEMENTED;
   
   }

   private Response processFrequencyTypeHasQuantityType(FrequencyTypeHasQuantityType hasRule)
   {
   
   return Response.NOT_YET_IMPLEMENTED;
   
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

   private Response processIdentifierIsAType(IdentifierIsAType isInstance)
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

   // private Map<Identifier, Set<Type>> findAllIsInstances()
   // {
   // 
      // Map<Identifier, Set<Type>> allIsInstances = copyOf(this.isInstances);
      // Map<FrequencyType, Set<Type>> allIsRules = findAllIsRules();
   // 
      // for (Map.Entry<Identifier, Set<Type>> eachIsInstance : this.isInstances.entrySet())
      // {
      // 
         // for (Type eachType : eachIsInstance.getValue())
         // {
         // 
            // for (Map.Entry<FrequencyType, Set<Type>> eachIsRule : allIsRules.entrySet())
            // {
            // 
               // if (eachType.equals(eachIsRule.getKey().type()))
               // {
               // 
                  // allIsInstances.merge(
                     //    eachIsInstance.getKey(),
                     //    eachIsRule.getValue(),
                     //    RulesEngine::merge
                     // );
               // 
               // }
            // 
            // }
         // 
         // }
      // 
      // }
   // 
      // return allIsInstances;
   // 
   // }
// 
   // private Map<FrequencyType, Set<QuantityType>> findAllHasRules()
   // {
   // 
      // Map<FrequencyType, Set<QuantityType>> allHasRules = copyOf(this.hasRules);
      // Map<FrequencyType, Set<Type>> allIsRules = this.findAllIsRules();
   // 
      // for (Map.Entry<FrequencyType, Set<QuantityType>> eachHasRule : this.hasRules.entrySet())
      // {
      // 
         // for (QuantityType eachQuantityType : eachHasRule.getValue())
         // {
         // 
            // for (Map.Entry<FrequencyType, Set<Type>> eachIsRule : allIsRules.entrySet())
            // {
            // 
            // //if ()
            // 
            // }
         // 
         // }
      // 
      // }
   // 
      // return allHasRules;
   // 
   // }
// 
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
