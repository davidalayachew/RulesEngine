package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record IsIdentifierAType(Identifier identifier, Type type) implements Parseable, ToString
{

   public static final Pattern regex =   // Unfortunately, I don't (currently) see a way to make this easy or neat or clean
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
      return cleanString();
   }

}
