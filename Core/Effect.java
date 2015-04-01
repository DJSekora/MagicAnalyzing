public class Effect
{
  public static final int GAIN_LIFE = 1;

  public int type = 0;
  public int amount = 0;

  public Effect(String s)
  {
    if(s.matches("Gain [0-9]* life.*"))
    {
      type = GAIN_LIFE;
      amount = findAmount(s,5);
    }
  }

  public static int findAmount(String s, int start)
  {
    String comp = "" + s.charAt(5);
    int i = start+1;
    while(s.charAt(i) != ' ')
      comp+=s.charAt(i);
    return Integer.parseInt(comp);
  }
}