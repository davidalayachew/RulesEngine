package io.github.davidalayachew;

import java.util.List;
import java.util.regex.Pattern;

public record Quantity(long count) implements Parseable, ToString
{

   public static final Pattern regex = Pattern.compile("(A|\\d{1,7})");

   public Quantity(List<String> strings)
   {
   
      this(Long.parseLong(strings.get(0).replace("A", "1")));
   
   }

   public String toString() {
      return cleanString();
   }

}
