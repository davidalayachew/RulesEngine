package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record FrequencyType(Frequency frequency, Type type) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                         Frequency.regex               //group 1
                                                         + " " + Type.regex            //group 2
                                                         );

   public FrequencyType(List<String> strings)
   {
   
      this(Frequency.valueOf(strings.get(0).replace(" ", "_")), new Type(strings.subList(1, 2)));
   
   }

   public String toString() {
      return cleanString();
   }

}
