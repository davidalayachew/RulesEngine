package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record FrequencyTypeRelationship(FrequencyType frequencyType, Relationship relationship) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile(                               //If pattern is surrounded by () then it's a group
                                                         FrequencyType.regex           //group 1 and 2
                                                         + " " + Relationship.regex    //group 3
                                                         );

   public FrequencyTypeRelationship(List<String> strings)
   {
   
      this(new FrequencyType(strings.subList(0, 2)), Relationship.valueOf(strings.get(2).replace(" ", "_")));
   
   }

   public String toString() {
      return cleanString();
   }

}
