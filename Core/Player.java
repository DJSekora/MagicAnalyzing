import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Random;
public class Player
{
  public ArrayList<Card> creatures;
  public ArrayList<Card> lands;

  public ArrayList<Card> library;
  public ArrayList<Card> hand;
  public ArrayList<Card> graveyard;
  public ArrayList<Card> exile;
  
  public int[] manaPool;

  public int life;
  public int poison; //Because why not?

  public int landsToPlay;
  public int landsPerTurn = 1;

  public String name = "";

  public BoardState parent;

  // Fresh Player
  public Player(BoardState par)
  {
    parent = par;

    creatures = new ArrayList<Card>();
    lands = new ArrayList<Card>();
    library = new ArrayList<Card>();
    hand = new ArrayList<Card>();
    graveyard = new ArrayList<Card>();
    exile = new ArrayList<Card>();
    life = 20;

    landsToPlay = 1;

    manaPool = new int[Card.COLORS];
  }

  public Player(String nname, BoardState par)
  {
    this(par);
    name = nname;
  }

  public Player(Player pl, BoardState par)
  {
    this(pl.name,par);
    life = pl.life;
    for(Card p:pl.creatures)
      creatures.add(new Card(p));
    for(Card p:pl.lands)
      lands.add(new Card(p));
    for(Card c:pl.library)
      library.add(new Card(c));
    for(Card c:pl.hand)
      hand.add(new Card(c));
    for(Card c:pl.graveyard)
      graveyard.add(new Card(c));
    for(Card c:pl.exile)
      exile.add(new Card(c));
  }

  public void untap()
  {
    for(Card p:creatures)
      p.untap();
    for(Card p:lands)
      p.untap();
    landsToPlay = landsPerTurn;
  }

  public boolean playLand(Card c)
  {
    if(hand.remove(c))
    {
      lands.add(c);
      landsToPlay--;
      return true;
    }
    return false;
  }

  public boolean playCard(Card c)
  {
    if(c.isCreature())
    {
      if(hand.remove(c))
      {
        payMana(c);
        creatures.add(c);
        return true;
      }
    }
    else
    {
      if(hand.remove(c))
      {
        payMana(c);
        return true;
      }
    }
    return false;
  }

  /* For now, we deal with colorless mana in a naive way.
   * (Ideally, we fold tapping a land for mana in as a "move")
   * Also, we more or less assume that untapped lands tap for a single color of mana
   * at a time, for simplicity. Also disregard hybrid costs.
   * 
   * Also assume that we won't get here unless we already know we can pay the cost! */
  public void payMana(Card c)
  {
    int[] remCost = new int[Card.COLORS];
    int cls = Card.COLORS-1; // Index for colorless mana, for convenience.
    // Pay what we can of colored costs out of mana pools
    for(int i=0;i<Card.COLORS;i++)
    {
      if(manaPool[i]>=c.cost[i])
      {
        manaPool[i] -= c.cost[i];
        remCost[i] = 0;
      }
      else
      {
        remCost[i] = c.cost[i] - manaPool[i];
        manaPool[i] = 0;
      }
    }
    // Pay remaining colored costs with appropriate lands
    for(int i=0;i<cls;i++)
    {
      if(remCost[i]>0)
      {
        for(Card l:lands)
        {
          if(!l.tapped && l.cost[i]>0)
          {
            l.tap();
            remCost[i] -= l.cost[i];
            if(remCost[i]<=0)
              break;
          }
        }
      }
    }
    
    // Pay what we can of colorless cost with mana pool, then lands (empty in WUBRG order)
    if(remCost[cls] > manaPool[cls])
    {
      remCost[cls] -= manaPool[cls];
      manaPool[cls] = 0;

      for(int i=0;i<cls;i++)
      {
        if(remCost[cls] > manaPool[i])
        {
          remCost[cls] -= manaPool[i];
          manaPool[i] = 0;
        }
        else
        {
          manaPool[i] -= remCost[cls];
          remCost[cls] = 0;
          break;
        }
      }
      /* Lands for colorless last (don't care about order for now)
       * Also disregard remaining mana (need to handle lands that produce multiple
       * mana later) */
      if(remCost[cls] > 0)
      {
        for(Card l:lands)
        {
          if(!l.tapped)
          {
            l.tap();
            for(int i=0;i<Card.COLORS;i++)
              remCost[cls] -= l.cost[i];
            if(remCost[cls]<=0)
              break;
          }
        }
      }
    }
    else
    {
      manaPool[cls] -= remCost[cls];
    }
  }

  public void tapLand(Card c)
  {
    c.tap();
    for(int i=0;i<Card.COLORS;i++)
    {
      manaPool[i] += c.cost[i];
    }
  }

  // Do nothing more!
  public void endPhase()
  {
    // Empty the mana pool
    for(int i=0;i<Card.COLORS;i++)
    {
      manaPool[i] = 0;
    }
    // Signal the parent to move on
    parent.advancePhase();
  }

  //Deck stuff (maybe move to Library if we make a class like that)
  public void loadDeck(String fname)
  {
    library = new ArrayList<Card>(60);
    try
    {
      Scanner deckReader = new Scanner(new File(fname));
      int num;
      String line;
      while(deckReader.hasNextLine())
      {
        num = deckReader.nextInt();
        line = deckReader.nextLine();
        line = line.trim(); // Remove leading space
        for(int i=0;i<num;i++)
          library.add(new Card(line));
      }
      deckReader.close();
    }
    catch(IOException e)
    {System.out.println("Error reading deck.");}
  }

    /* Randomization algorithm from Tic Tac Oh No random generator.
   * Apparently this is something called "Fisher-Yates" or Knuths "Algorithm P",
   * but those references were not used.
   * Note that this technically might not cover all permutations, depending on how
   * the random numbers are generated by Random (number of possible seeds might be too
   * small).
   *
   * In the future, maybe implement various simulated shuffle techniques (riffle,
   * bridge, pile ("Magic shuffle"), clump, etc.).*/
  public void shuffleDeck()
  {
    Random rand = new Random();
    int open = library.size();
    int k;
    Card temp;
    while(open>0)
    {
      k = rand.nextInt(open);
      open--;
      if(k!=open)
      {
        temp = library.get(k);
        library.set(k, library.get(open));
        library.set(open, temp);
      }
    }
  }

  public void drawCard()
  {
    hand.add(library.remove(library.size()-1));
  }

  public void drawCards(int num)
  {
    for(int i=0;i<num;i++)
      drawCard();
  }

  public ArrayList<Card> determineAvailableMoves()
  {
    int[] mana = new int[Card.COLORS];

    // Start by considering mana currently left in pool.
    for(int i = 0; i<Card.COLORS;i++)
      mana[i] = manaPool[i];

    /* See what mana we can get from untapped lands
     * For now, we just have lands as tapping for their "cost"*/
    for(Card l:lands)
      for(int i=0;i<Card.COLORS;i++)
        if(!l.tapped)
          mana[i]+=l.cost[i];

    int totalMana = 0;
    for(int i = 0; i<Card.COLORS; i++)
      totalMana+=mana[i];

    ArrayList<Card> moveList = new ArrayList<Card>();
    for(Card c:hand)
    {
      boolean canPlay = true;
      if(c.isLand())
      {
        canPlay = (landsToPlay > 0);
      }
      else
      {
        int totalCost = c.cost[Card.COLORS-1];
        for(int i = Card.COLORS-2;i>=0;i--)
        {
          if (mana[i] < c.cost[i])
          {
            canPlay = false;
            break;
          }
          totalCost+=c.cost[i];
        }
        if (totalCost > totalMana)
          canPlay = false;
      }
      if(canPlay)
        moveList.add(c);
    }
    return moveList;
  }

  public void printBoard()
  {
    System.out.print(name + "'s hand: |");
    for(Card c:hand)
      System.out.print(c.name + "|");
    System.out.println();

    System.out.print(name + "'s creatures: |");
    for(Card c:creatures)
      System.out.print(c.name + ", " + c.power + "/" + c.toughness + 
                       (c.tapped ? "(T)" : "") + "|");
    System.out.println();

    System.out.print(name + "'s lands: |");
    for(Card c:lands)
      System.out.print(c.name + (c.tapped ? "(T)" : "") + "|");
    System.out.println();

    //System.out.print(name + "'s mana pool: ");
  }

  public void parseTextCommand(String cmd)
  {
    if(cmd.matches("play .*"))
    {
      String n = cmd.substring(5);
      for(Card c:determineAvailableMoves())
        if(c.name.equalsIgnoreCase(n))
        {
          if(c.isLand())
            playLand(c);
          else
            playCard(c);
          break;
        }
    }
    else if(cmd.matches("tap .*"))
    {
      String n = cmd.substring(4);
      for(Card c:lands)
        if(!c.tapped && c.name.equalsIgnoreCase(n))
        {
          tapLand(c);
          break;
        }
    }
    else if(cmd.matches("pass"))
    {
      endPhase();
    }
  }
}