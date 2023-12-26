
package io.github.davidalayachew;

import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;


public class RulesEngine_Attempt2Test
{

   private static final Identifier david  = new Identifier("DAVID");

   private static final Type programmer   = new Type("PROGRAMMER");
   private static final Type artist       = new Type("ARTIST");
   private static final Type genius       = new Type("GENIUS");
   private static final Type gift         = new Type("GIFT");
   private static final Type treasure     = new Type("TREASURE");
   private static final Type blessing     = new Type("BLESSING");
   private static final Type wonder       = new Type("WONDER");
   private static final Type opportunity  = new Type("OPPORTUNITY");

   private static final FrequencyType everyArtist     = new FrequencyType(Frequency.EVERY, artist);
   private static final FrequencyType everyGift       = new FrequencyType(Frequency.EVERY, gift);
   private static final FrequencyType everyBlessing   = new FrequencyType(Frequency.EVERY, blessing);

   private static final IdentifierIsAType davidIsAProgrammer   = new IdentifierIsAType(david, programmer);
   private static final IdentifierIsAType davidIsAnArtist      = new IdentifierIsAType(david, artist);

   private static final FrequencyTypeIsType everyArtistIsAGenius  = new FrequencyTypeIsType(everyArtist, genius);
   private static final FrequencyTypeIsType everyArtistIsAGift    = new FrequencyTypeIsType(everyArtist, gift);

   private static final FrequencyTypeIsType everyGiftIsATreasure  = new FrequencyTypeIsType(everyGift,   treasure);
   private static final FrequencyTypeIsType everyGiftIsABlessing  = new FrequencyTypeIsType(everyGift,   blessing);

   private static final FrequencyTypeIsType everyBlessingIsAWonder         = new FrequencyTypeIsType(everyGift,   wonder);
   private static final FrequencyTypeIsType everyBlessingIsAnOpportunity   = new FrequencyTypeIsType(everyGift,   opportunity);

   private static final RulesEngine_Attempt2.PutResponse SUCCESS = new RulesEngine_Attempt2.PutResponse.NewDirectMappingCreated();
   private static final RulesEngine_Attempt2.PutResponse DIRECT_MAPPING_ALREADY_EXISTS = new RulesEngine_Attempt2.PutResponse.DirectMappingAlreadyExists();

   @Test
   public void testPut()
   {
   
      final var rulesEngine = new RulesEngine_Attempt2();
   
      Assert.assertEquals(SUCCESS, rulesEngine.put(davidIsAProgrammer));
      Assert.assertEquals(DIRECT_MAPPING_ALREADY_EXISTS, rulesEngine.put(davidIsAProgrammer));
      Assert.assertEquals(DIRECT_MAPPING_ALREADY_EXISTS, rulesEngine.put(davidIsAProgrammer));
   
   }

}
