import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Hashtable;
public class Card implements Targetable
{
  /* Card type is represented as an 8 bit integer. For example:
   * 00000110 = 6 represents an artifact creature.
   * Cards usually only have 1-2 types, and if they have 2 one of them is usually
   * "creature" and the other is "artifact" or "enchantment".
   * 
   * For now, we only have creatures, lands, and sorceries, but later we can add more. 
   * We're going to ignore Tribal for now, because it overcomplicates things and is a
   * minor mechanic from a single block. */

  /* No flavor text for now, maybe in the distant future. */

  public static final int LAND = 0x1;
  public static final int CREATURE = 0x2;
  public static final int ARTIFACT = 0x4;
  public static final int ENCHANTMENT = 0x8;
  public static final int PLANESWALKER = 0x10;
  public static final int SORCERY = 0x20;
  public static final int INSTANT = 0x40;

  /* Color is also represented as an 8 bit integer, since cards can be multiple colors.
   * For example:
   * 00010001 = 17 represents a green and white card. */
  public static final int COLORS = 6;
  public static final int COLORLESS = 0;
  public static final int WHITE = 0x1;
  public static final int BLUE = 0x2;
  public static final int BLACK = 0x4;
  public static final int RED = 0x8;
  public static final int GREEN = 0x10;

  /* We store the set of all cards here for now. Maybe move to file or cache lookup
   * if it becomes a memory concern later. */
  public static Hashtable<String,Card> cardList = new Hashtable<String,Card>();
  public static Hashtable<String,EffectList> cardEffects = new Hashtable<String,EffectList>();

  public String name = "";
  public int type;
  public String text = "";
  public BufferedImage image;
  public int color;
  public int[] cost;
  public Player controller;

  public Card(String nname, int ntype, String ntext, int ncolor, int[] ncost)
  {
    name = nname;
    type = ntype;
    text = ntext;
    color = ncolor;
    cost = ncost;
  }
  
  // Copy constructor
  public Card(Card c)
  {
    this(c.name,c.type,c.creatureType,c.text,c.power,c.toughness,c.color,c.cost);
    tapped = c.tapped;
  }
  public Card(String name)
  {
    this(cardList.get(name));
  }

  /* Sample format (no leading asterisks or spaces):

   * For a 2/2 creature that costs 1W and has no special abilities:
   * A Dude
   * 2 1
   * Human
   * 
   * 2 2
   * 1 0 0 0 0 1

   * For a burn spell that costs R:
   * Thunder Bolt
   * 32 8
   * Thunder Bolt does 3 damage to target creature or player.
   * 0 0 0 1 0 0
   */

  /* Should probably switch to JSONs at some point, but this will do for now.
     Input file should be a list of correctly formatted cards, no blank lines
     except for in the appropriate field for cards with blank text boxes. We don't
     do any verification for now, since we're making the files ourselves and we're kind
     of short on time. For now, make sure the last line isn't an extra blank line. */

  public static void loadCardList(String cardFileName)
  {
    try
    {
      Scanner clr = new Scanner(new File(cardFileName));
      String n; // Name
      int t; // Type
      int c; // Color
      Card card; // The new card
      while(clr.hasNextLine())
      {
        n = clr.nextLine();
        t = clr.nextInt();
        c = clr.nextInt();
        clr.nextLine();
        if((t&CREATURE) > 0)
        {
          card = new Card(n,t,clr.nextLine(),clr.nextLine(),clr.nextInt(),clr.nextInt(),c,
                          new int[] {clr.nextInt(),clr.nextInt(),clr.nextInt(),
                                     clr.nextInt(),clr.nextInt(),clr.nextInt()});
          addCard(n,card);
        }
        else
        {
          card = new Card(n,t,clr.nextLine(),c,
                       new int[] {clr.nextInt(),clr.nextInt(),clr.nextInt(),
                                  clr.nextInt(),clr.nextInt(),clr.nextInt()});
          addCard(n, card);
        }
        if(clr.hasNextLine())
          clr.nextLine();
      }
      clr.close();
    }
    catch(IOException e)
    {System.out.println("Error reading card list file.");}
  }

  // Adds a single card to the card list
  public static void addCard(String n, Card card)
  {
    cardList.put(n,card);
    addEffect(n,card);
  }

  // Parses the effects of a single card and adds it to the database
  public static void addEffect(String n, Card card)
  {
    cardEffects.put(n,card.parseEffect());
  }

  public EffectList parseEffect()
  {
    String[] effs = text.split("\\. ");
    int numEffs = effs.length;
    return new EffectList(name, effs);
  }

  public EffectList getEffects()
  {
    return cardEffects.get(name);
  }

  // Some property checkers
  public boolean isLand()
  {
    return ((type&LAND) > 0);
  }

  public boolean isCreature()
  {
    return ((type&CREATURE) > 0);
  }

  public String toString()
  {
    return name;
  }

  /* Stuff for permanents (used to be a subclass) */

  public boolean tapped = false;

  public void tap()
  {
    tapped = true;
  }
  public void untap()
  {
    tapped = false;
  }

  /* Stuff for creatures (used to be a subclass of Permanent) */
  public int power = 0;
  public int toughness = 0;
  public int damage = 0;
  public String creatureType = "";

  public Card(String nname, int ntype, String nctype, String ntext,
                  int np, int nt, int ncolor, int[] ncost)
  {
    this(nname, ntype, ntext, ncolor, ncost);
    power = np;
    toughness = nt;
    creatureType = nctype;
  }

  public void takeDamage(int amount)
  {
    damage += amount;
    if(damage > toughness)
      controller.killCreature(this);
  }
}