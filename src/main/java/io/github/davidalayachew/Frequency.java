package io.github.davidalayachew;

import java.util.Arrays;

public enum Frequency
{

   EVERY,         //every        xyz is an abc
   NOT_EVERY,     //not every    xyz is an abc
   NOT_A_SINGLE,  //not a single xyz is an abc
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
   
}
