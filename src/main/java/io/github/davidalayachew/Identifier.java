package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record Identifier(String name)  implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile("([a-zA-Z]+(?:[_]?[a-zA-Z0-9]+)*)");

   public Identifier(List<String> strings)
   {
   
      this(strings.get(0));
   
   }

   public String toString() {
      return cleanString();
   }

}
