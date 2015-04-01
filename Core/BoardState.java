/* Class to represent the board state of a Magic: The Gathering game.
 *
 * We should possibly add a Player object at some point, maybe one who has a
 * Library object and a Hand object and so forth, just for good coding practice.
 *
 */

import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Random;
public class BoardState
{
  public static final int UNTAP = 0;
  public static final int UPKEEP = 1;
  public static final int DRAW = 2;
  public static final int MAIN1 = 3;
  public static final int COMBAT = 4;
  public static final int ATTACK = 5;
  public static final int BLOCK = 6;
  public static final int DAMAGE = 7;
  public static final int ENDCOMBAT = 8;
  public static final int MAIN2 = 9;
  public static final int END = 10;
  public static final int CLEANUP = 11;

  public static final int COLORS = 6;
  public static final int STARTING_HAND_SIZE = 7;

  int players = 2;

  public ArrayList<Card>[] creatures;
  public ArrayList<Card>[] lands;

  public ArrayList<Card>[] library;
  public ArrayList<Card>[] hand;
  public ArrayList<Card>[] graveyard;
  public ArrayList<Card>[] exile;

  public int[][] manaPool;

  public int[] life;

  public int turn;
  public int phase;
  public int landsToPlay = 1;

  // Initialize a blank BoardState
  public BoardState()
  {
    creatures = new ArrayList[players];
    lands = new ArrayList[players];
    library = new ArrayList[players];
    hand = new ArrayList[players];
    graveyard = new ArrayList[players];
    exile = new ArrayList[players];

    life = new int[players];
    manaPool = new int[players][COLORS];

    for(int i = 0; i < players; i++)
    {
      creatures[i] = new ArrayList<Card>();
      lands[i]= new ArrayList<Card>();
      library[i] = new ArrayList<Card>();
      hand[i] = new ArrayList<Card>();
      graveyard[i] = new ArrayList<Card>();
      exile[i] = new ArrayList<Card>();
      life[i] = 20;
    }

    turn = 0;
    phase = MAIN1;
  }


  // The main constructor we will call to start a game.
  public BoardState(String[] deckFile)
  {
    this();
    for(int i = 0; i < players; i++)
    {
      library[i] = loadDeck(deckFile[i]);
      shuffleDeck(i);
      for(int j=0; j<STARTING_HAND_SIZE; j++)
        drawCard(i);
    }
  }

  public BoardState(BoardState old)
  {
    creatures = new ArrayList[players];
    lands = new ArrayList[players];
    library = new ArrayList[players];
    hand = new ArrayList[players];
    graveyard = new ArrayList[players];
    exile = new ArrayList[players];

    life = new int[players];
    manaPool = new int[players][COLORS];

    for(int i = 0; i < players; i++)
    {
      creatures[i] = new ArrayList<Card>();
      lands[i]= new ArrayList<Card>();
      library[i] = new ArrayList<Card>();
      hand[i] = new ArrayList<Card>();
      graveyard[i] = new ArrayList<Card>();
      exile[i] = new ArrayList<Card>();
      life[i] = old.life[i];

      for(Card p:old.creatures[i])
        creatures[i].add(new Card(p));
      for(Card p:old.lands[i])
        lands[i].add(new Card(p));
      for(Card c:old.library[i])
        library[i].add(new Card(c));
      for(Card c:old.hand[i])
        hand[i].add(new Card(c));
      for(Card c:old.graveyard[i])
        graveyard[i].add(new Card(c));
      for(Card c:old.exile[i])
        exile[i].add(new Card(c));
    }

    turn = old.turn;
    phase = old.phase;
  }

  // Advance to the next phase! We skip a lot of phases for now, because reasons.
  public void advancePhase()
  {
    switch(phase)
    {
      case UNTAP:
        phase = DRAW;
        doDraw();
        break;
      case DRAW:
        phase = MAIN1;
        break;
      case MAIN1:
        advanceTurn();
        phase = UNTAP;
        doUntap();
        break;
      default:
        System.out.println("INVALID PHASE REACHED!");
    }
  }

  public void advanceTurn()
  {
    turn++;
    if(turn >= players)
      turn = 0;
    printMessage("It is now player " + turn + "'s turn.");
    return;
  }

  public void doUntap()
  {
    for(Card p:creatures[turn])
      p.untap();
    for(Card p:lands[turn])
      p.untap();
    advancePhase();
  }

  public void doDraw()
  {
    drawCard(turn);
    advancePhase();
  }

  public void playLand(int player, Card c)
  {
    if(hand[player].remove(c))
    {
      lands[player].add(c);
      landsToPlay--;
      printMessage("Player " + player + " plays land " + c + ".");
    }
    return;
  }

  public void playCard(int player, Card c)
  {
    if(c.isCreature())
    {
      if(hand[player].remove(c))
      {
        creatures[player].add(c);
        printMessage("Player " + player + " summons creature " + c + "!");
      }
    }
    else
    {
      if(hand[player].remove(c))
      {
        printMessage("Player " + player + " plays spell " + c + "!");
      }
    }
  }

  /* Right now, we just print to standard output, but maybe someday we'll have a GUI or
   * something.... */
  public void printMessage(String s)
  {
    System.out.println(s);
  }

  // Deck stuff (move to Library when that becomes a thing)
  public ArrayList<Card> loadDeck(String fname)
  {
    ArrayList<Card> ret = new ArrayList<Card>(60);
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
          ret.add(new Card(line));
      }
      deckReader.close();
    }
    catch(IOException e)
    {System.out.println("Error reading deck.");}
    return ret;
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
  public void shuffleDeck(int player)
  {
    Random rand = new Random();
    ArrayList<Card> d = library[player];
    int open = d.size();
    int k;
    Card temp;
    while(open>0)
    {
      k = rand.nextInt(open);
      open--;
      if(k!=open)
      {
        temp = d.get(k);
        d.set(k, d.get(open));
        d.set(open, temp);
      }
    }
  }

  public void drawCard(int player)
  {
    hand[player].add(library[player].remove(library[player].size()-1));
    return;
  }

  public void print()
  {
    for(int i=0;i<players;i++)
    {
      System.out.print("Player " + i + "'s hand: |");
      for(Card c:hand[i])
        System.out.print(c.name + "|");
      System.out.println();

      System.out.print("Player " + i + "'s creatures: |");
      for(Card c:creatures[i])
        System.out.print(c.name + ", " + c.power + "/" + c.toughness + 
                         (c.tapped ? "(T)" : "") + "|");
      System.out.println();

      System.out.print("Player " + i + "'s lands: |");
      for(Card c:lands[i])
        System.out.print(c.name + (c.tapped ? "(T)" : "") + "|");
      System.out.println();

      //System.out.print("Player " + i + "'s mana pool: ");
    }
  }
}