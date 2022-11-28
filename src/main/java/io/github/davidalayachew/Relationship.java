package io.github.davidalayachew;

import java.util.Arrays;

public enum Relationship
{

   HAS,  //Frequency Type has Quantity Type
   IS_A, //Instance is Type
   ;

   //turns [A, B, C] into (A|B|C)
   //useful in Java's Pattern class, when you want a pattern capturing group for an enum

   public static final String regex = Arrays
                                       .toString(values())
                                       .replace('[', '(')
                                       .replace(']', ')')
                                       .replace('_', ' ')
                                       .replaceAll(", ", "|")
                                       ;

   public String pattern()
   {
   
      return switch (this)
         {
         
            case HAS    -> this.name();
            case IS_A   -> this.name().replace("_", " ").replace(" A", "(?: A(?:N|)|)");
         
         };
   
   }

}
