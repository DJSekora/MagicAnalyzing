/* Class to hold a single card effect. Assume for now that a card can have multiple
 * effects, but an effect can only have at most 1 target.
 * Also assume that all effects on a card are executed (no 'or' statements for now)
 */
public class Effect
{
  // Effect types
  public static final int GAIN_LIFE = 1;
  public static final int DEAL_DAMAGE_TO_TARGET = 2;
  public static final int DESTROY_TARGET = 3;
  public static final int DRAW_CARDS = 4;
  public static final int LOSE_LIFE = 5;
  public static final int DISCARD_CARDS = 6; // DO NOT USE - AI CANNOT HANDLE

  // Target types
  public static final int NO_TARGET = 0;
  public static final int CREATURE = 1;
  public static final int PLAYER = 2;

  public int[] type = {0};
  public int[] amount = {0};
  public int targetType = NO_TARGET;

  public Effect(String name, String s)
  {
    if(s.matches("Gain [0-9]* life.*"))
    {
      type[0] = GAIN_LIFE;
      amount[0] = findAmount(s,5);
    }
    else if(s.matches(name + " deals [0-9]* damage to target.*"))
    {
      type[0] = DEAL_DAMAGE_TO_TARGET;
      amount[0] = findAmount(s, name.length() + 7);
      targetType = findTargetType(s, (name + amount).length() + 25);
    }
    else if(s.matches("Destroy target.*"))
    {
      type[0] = DESTROY_TARGET;
      targetType = findTargetType(s,15);
    }
    else if(s.matches("Target player .*"))
    {
      String[] compoundEffect = s.substring(14).split(" & ");
      amount = new int[compoundEffect.length];
      targetType = PLAYER;
      for(int i = 0; i<compoundEffect.length; i++)
      {
        String cseff = compoundEffect[i];
        if(cseff.matches("draws [a-z0-9]* card.*"))
        {
          type[i] = DRAW_CARDS;
          amount[i] = findAmount(cseff,6);
        }
        else if(cseff.matches("loses [a-z0-9]* life.*"))
        {
          type[i] = LOSE_LIFE;
          amount[i] = findAmount(cseff,6);
        }
        else if(cseff.matches("discards [a-z0-9]* card.*"))
        {
          type[i] = DISCARD_CARDS;
          amount[i] = findAmount(cseff,9);
        }
      }
    }
  }

  public static int findAmount(String s, int start)
  {
    String comp = "" + s.charAt(start);
    int i = start+1;
    while(s.charAt(i) != ' ')
      comp+=s.charAt(i);
    if(comp.equals("a") || comp.equals("one"))
      return 1;
    else if(comp.equals("two"))
      return 2;
    else if(comp.equals("three"))
      return 3;
    else if(comp.equals("four"))
      return 4;
    else if(comp.equals("five"))
      return 5;
    return Integer.parseInt(comp);
  }

  public static int findTargetType(String s, int start)
  {
    int targetType = 0;
    String[] sub = s.substring(start).split("[ \\.]");
    for(String ss:sub)
    {
      switch(ss)
      {
        case "creature":
          targetType = (targetType|CREATURE);
          break;
        case "player":
          targetType = (targetType|PLAYER);
          break;
        default:
          break;
      }
    }
    return targetType;
  }

  public boolean isTargetedEffect()
  {
    return (targetType>0);
  }

  public boolean canTarget(int type)
  {
    return ((targetType & type) > 0);
  }
}

// Class to hold the list of effects of a card
/* I kind of forgot why I wanted to make a special structure like this instead of
 * just using an ArrayList like before... maybe to handle targets better?*/
class EffectList
{
  int size;
  Effect[] list;
  public EffectList(String name, String[] effs)
  {
    size = effs.length;
    list = new Effect[size];
    for(int i=0;i<size;i++)
      list[i] = new Effect(name, effs[i]);
  }

  public int requiredTargets()
  {
    int ret = 0;
    for(int i=0;i<size;i++)
      if(list[i].isTargetedEffect())
        ret++;
    return ret;
  }

  public Effect get(int i)
  {
    return list[i]; 
  }
}