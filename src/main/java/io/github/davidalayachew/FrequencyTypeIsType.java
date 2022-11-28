package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record FrequencyTypeIsType(FrequencyType frequencyType, Type type) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                                     //If pattern is surrounded by () then it's a group
                                                         FrequencyType.regex                 //group 1 and 2
                                                         + " " + Relationship.IS_A.pattern() //not a group because no (), so just a pattern
                                                         + " " + Type.regex                  //group 3
                                                         );

   public FrequencyTypeIsType(List<String> strings)
   {
   
      this(new FrequencyType(strings.subList(0, 2)), new Type(strings.subList(2, 3)));
   
   }

   public String toString() {
      return cleanString();
   }

}
