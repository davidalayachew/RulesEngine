package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record FrequencyTypeHasQuantityType(FrequencyType frequencyType, QuantityType quantityType) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                         FrequencyType.regex                 //group 1 and 2
                                                         + " " + Relationship.HAS.pattern()  //not a group because no (), so just a pattern
                                                         + " " + QuantityType.regex          //group 3 and 4
                                                         );

   public FrequencyTypeHasQuantityType(List<String> strings)
   {
   
      this(new FrequencyType(strings.subList(0, 2)), new QuantityType(strings.subList(2, 4)));
   
   }

   public String toString() {
      return cleanString();
   }

}
