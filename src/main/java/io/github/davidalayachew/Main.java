package io.github.davidalayachew;

public class Main
{

   public static void main(String[] args)
   {
   
      final var rulesEngine = new RulesEngine_Attempt2();
   
      rulesEngine.put(new IdentifierIsAType(List.of("David", "Programmer")));
   
   }

}
