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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RulesEngine
{

   //private record

   private final Map<Identifier,    Set<Type>         >  isInstances    = new HashMap<>();
   private final Map<FrequencyType, Set<Type>         >  isRules        = new HashMap<>();
   private final Map<Identifier,    Set<QuantityType> >  hasInstances   = new HashMap<>();
   private final Map<FrequencyType, Set<QuantityType> >  hasRules       = new HashMap<>();

   public RulesEngine()
   {

      SwingUtilities.invokeLater(this::constructJFrame);

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

      record IdentifierType(List<Identifier> identifiers, List<Type> types)
      {

         IdentifierType(Identifier identifier, Type... types)
         {

            this(List.of(identifier), List.of(types));

         }

         IdentifierType(Type... types)
         {

            this(List.of(), List.of(types));

         }

      }

      final Consumer<Optional<? extends Parseable>> populateLists =
         potential ->
         {

            if (potential.isPresent())
            {

               final IdentifierType identifierType =
                  switch (potential.get())
                  {

                     case Type type                                              -> new IdentifierType(type);
                     case Identifier identifier                                  -> new IdentifierType(identifier);
                     case IdentifierIsAType(var identifier, var type)            -> new IdentifierType(identifier, type);
                     case IdentifierHasQuantityType(var identifier, var qType)   -> new IdentifierType(identifier, qType.type());
                     case Quantity q                                             -> new IdentifierType();
                     case QuantityType(var __, var type)                         -> new IdentifierType(type);
                     case FrequencyType(var __, var type)                        -> new IdentifierType(type);
                     case FrequencyTypeHasQuantityType(var fType, var qType)     -> new IdentifierType(fType.type(), qType.type());
                     case FrequencyTypeIsType(var fType, var type)               -> new IdentifierType(fType.type(), type);
                     case FrequencyTypeRelationship(FrequencyType fType, var __) -> new IdentifierType(fType.type());

                     //Adding query components to the sidebars would be misleading
                     case IsIdentifierAType iiat                                 -> new IdentifierType(List.of(), List.of());

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

      final Identifier givenIdentifier = isQuery.identifier();
      final Type givenType = isQuery.type();

      UNKNOWN_IDENTIFIER:
      {

         final Predicate<Identifier> cantFindTheGivenIdentifier =
            parameter -> !this.isInstances.containsKey(parameter) && !this.hasInstances.containsKey(parameter);

         if (cantFindTheGivenIdentifier.test(givenIdentifier))
         {

            return Response.UNKNOWN_IDENTIFIER;

         }

      }

      UNKNOWN_TYPE:
      {

         final Predicate<Type> cantFindTheGivenType =
            parameter -> Stream
                           .of
                           (
                              this.isInstances.values().stream().flatMap(Set::stream),
                              this.isRules.entrySet().stream().flatMap(each -> Stream.concat(Stream.of(each.getKey().type()), each.getValue().stream())),
                              this.hasInstances.values().stream().flatMap(Set::stream).map(QuantityType::type),
                              this.hasRules.entrySet().stream().flatMap(each -> Stream.concat(Stream.of(each.getKey().type()), each.getValue().stream().map(QuantityType::type)))
                           )
                           .flatMap(Function.identity())
                           // .peek(System.out::println)
                           .noneMatch(givenType::equals)
                           ;

         if (cantFindTheGivenType.test(givenType))
         {

            return Response.UNKNOWN_TYPE;

         }

      }

      final Set<Type> typesForGivenIdentifier = this.isInstances.get(givenIdentifier);

      if (typesForGivenIdentifier == null || typesForGivenIdentifier.isEmpty())
      {

         return Response.NEED_MORE_INFO;

      }

      if (typesForGivenIdentifier.contains(givenType))
      {

         return Response.CORRECT;

      }

      complexTreeWalking:
      {

         final List<Type> allMatchingTypesForGivenIdentifier =
            new ArrayList<>(typesForGivenIdentifier);

         int index = -1;

         while (++index < allMatchingTypesForGivenIdentifier.size())
         {

            final Type potentialMatch = allMatchingTypesForGivenIdentifier.get(index);

            if (potentialMatch.equals(givenType))
            {

               return Response.CORRECT;

            }

            final FrequencyType correctMatch = new FrequencyType(Frequency.EVERY, potentialMatch);

            if (this.isRules.get(correctMatch) instanceof Set<Type> matchingTypes)
            {

               for (Type matchingType : matchingTypes)
               {

                  if (!allMatchingTypesForGivenIdentifier.contains(matchingType))
                  {

                     allMatchingTypesForGivenIdentifier.add(matchingType);

                  }

               }

            }

            final FrequencyType incorrectMatch = new FrequencyType(Frequency.NOT_A_SINGLE, potentialMatch);

            if (this.isRules.get(incorrectMatch) instanceof Set<Type> nonMatchingTypes && nonMatchingTypes.contains(givenType))
            {

               return Response.INCORRECT;

            }

         }

         return Response.NEED_MORE_INFO;

      }

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

}
