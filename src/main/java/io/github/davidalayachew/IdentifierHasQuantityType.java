package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record IdentifierHasQuantityType(Identifier identifier, QuantityType quantityType) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                         Identifier.regex                    //group 1
                                                         + " " + Relationship.HAS.pattern()  //not a group because no (), so just a pattern
                                                         + " " + QuantityType.regex          //group 2 and 3
                                                         );

   public IdentifierHasQuantityType(List<String> strings)
   {
   
      this(new Identifier(strings.subList(0, 1)), new QuantityType(strings.subList(1, 3)));
   
   }

   public String toString() {
      return cleanString();
   }

}
