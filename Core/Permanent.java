import java.awt.image.BufferedImage;
public class Permanent
{
  public static final int LAND = 0x1;
  public static final int CREATURE = 0x2;
  public static final int ARTIFACT = 0x4;
  public static final int ENCHANTMENT = 0x8;
  public static final int PLANESWALKER = 0x10;

  public String name = "";
  public int type;
  public String text = "";
  public int color;
  public int[] cost;
  public BufferedImage image;

  public boolean tapped = false;

  public Permanent(String nname, int ntype, String ntext, int ncolor, int[] ncost)
  {
    name = nname;
    type = ntype;
    text = ntext;
    color = ncolor;
    cost = ncost;
  }

  public Permanent(Permanent p)
  {
    this(p.name,p.type,p.text,p.color, p.cost);
  }

  public void tap()
  {
    tapped = true;
  }
  public void untap()
  {
    tapped = false;
  }
}

class Creature extends Permanent
{
  public int power;
  public int toughness;
  private int permanantpower;
  private int permanenttoughness;
  public String creatureType = "";

  public Creature(String nname, int ntype, String nctype, String ntext,
                  int np, int nt, int ncolor, int[] ncost)
  {
    super(nname, ntype, ntext, ncolor, ncost);
    power = np;
    toughness = nt;
    permanantpower=power;
    permanenttoughness=toughness;
    creatureType = nctype;
  }

  public Creature(Creature c)
  {
    this(c.name,c.type,c.creatureType,c.text,c.power,c.toughness,c.color,c.cost);
  }
}