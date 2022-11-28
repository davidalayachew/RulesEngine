package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record IdentifierIsAType(Identifier identifier, Type type) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                         Identifier.regex                    //group 1
                                                         + " " + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                         + " " + Type.regex                  //group 2
                                                         );

   public IdentifierIsAType(List<String> strings)
   {
   
      this(new Identifier(strings.subList(0, 1)), new Type(strings.subList(1, 2)));
   
   }

   public String toString() {
      return cleanString();
   }

}
