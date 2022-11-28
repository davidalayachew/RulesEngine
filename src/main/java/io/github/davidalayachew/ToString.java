package io.github.davidalayachew;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.InvocationTargetException;

public interface ToString
{

   default public String cleanString()
   {
   
      String output = "";
   
      if (this.getClass().isRecord())
      {
      
         final RecordComponent[] components = this.getClass().getRecordComponents();
      
         for (int i = 0; i < components.length; i++)
         {
         
            output += this.componentToString(i, components[i]);
            
         }
      
      }
   
      return output;
   
   }

   private String componentToString(int i, RecordComponent each)
   {
   
      try
      {
      
         return (i == 0 ? "" : " ") + each.getAccessor().invoke(this);
      
      }
            
      catch (IllegalAccessException | InvocationTargetException e)
      {
            
         throw new IllegalArgumentException(e);
            
      }
         
   
   }

}
