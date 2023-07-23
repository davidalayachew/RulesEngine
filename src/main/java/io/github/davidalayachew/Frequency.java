package io.github.davidalayachew;

public enum Frequency
{

   EVERY,         //every        xyz is an abc
   NOT_EVERY,     //not every    xyz is an abc
   NOT_A_SINGLE,  //not a single xyz is an abc
   ;

   //Creates a String in the following format -- (A|B|C)
   //useful in Java's Pattern class, when you want a pattern capturing group for an enum

   public static final String regex = ToString.captureGroupForEnums(values());

}
