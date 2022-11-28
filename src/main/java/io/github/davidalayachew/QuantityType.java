package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record QuantityType(Quantity quantity, Type type) implements Parseable, ToString
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
      return cleanString();
   }

}
