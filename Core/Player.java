import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Random;
public class Player implements Targetable
{
  public static final int ATTACK_SEARCH_CUTOFF = 100;
  public static final int BLOCK_SEARCH_CUTOFF = 10;

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
  public int id;
  public boolean lost = false; // Kind of useless for 2 player

  // Fresh Player
  public Player(BoardState par, int nid)
  {
    parent = par;
    id = nid;

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

  public Player(String nname, BoardState par, int id)
  {
    this(par, id);
    name = nname;
  }

  /* General copy constructor (stuff I don't want to copy 5 times) */
  public Player(Player pl, BoardState par)
  {
    this(pl.name,par,pl.id);

    life = pl.life;
    lost = pl.lost;
    landsToPlay = pl.landsToPlay;

    for (int i=0; i< Card.COLORS; i++)
      manaPool[i] = pl.manaPool[i];
  }

  /* Copy constructor (for going from one board state to another, so we can
     maintain control). Updates the move n to point to the copies */
  public Player(Player pl, BoardState par, Move m, Move n)
  {
    this(pl, par);

    for(Card p:pl.creatures)
      creatures.add(new Card(p, this, m, n));
    for(Card p:pl.lands)
      lands.add(new Card(p, this, m, n));
    for(Card c:pl.library)
      library.add(new Card(c, this, m, n));
    for(Card c:pl.hand)
      hand.add(new Card(c, this, m, n));
    for(Card c:pl.graveyard)
      graveyard.add(new Card(c, this, m, n));
    for(Card c:pl.exile)
      exile.add(new Card(c, this, m, n));

    // Check to see if this player is a target
    int tempmax = m.numTargets();
    for(int i=0;i<tempmax;i++)
      if(pl == m.targets[i])
        n.targets[i] = this;
  }

  /* Copy constructor for attacking */
  public Player(Player pl, BoardState par, Card[] m, Card[] n)
  {
    this(pl,par);

    for(Card p:pl.creatures)
      creatures.add(new Card(p, this, m, n));
    for(Card p:pl.lands)
      lands.add(new Card(p, this, m, n));
    for(Card c:pl.library)
      library.add(new Card(c, this, m, n));
    for(Card c:pl.hand)
      hand.add(new Card(c, this, m, n));
    for(Card c:pl.graveyard)
      graveyard.add(new Card(c, this, m, n));
    for(Card c:pl.exile)
      exile.add(new Card(c, this, m, n));
  }

  /* Copy constructor for blocking (have to update the attackers array) */
  public Player(Player pl, BoardState par, Card[][] m, Card[][] n, Card[] atkm, Card[] atkn)
  {
    this(pl,par);

    for(Card p:pl.creatures)
      creatures.add(new Card(p, this, m, n, atkm, atkn));
    for(Card p:pl.lands)
      lands.add(new Card(p, this, m, n));
    for(Card c:pl.library)
      library.add(new Card(c, this, m, n));
    for(Card c:pl.hand)
      hand.add(new Card(c, this, m, n));
    for(Card c:pl.graveyard)
      graveyard.add(new Card(c, this, m, n));
    for(Card c:pl.exile)
      exile.add(new Card(c, this, m, n));
  }

  // For Targetable
  public String getName()
  {
    return name;
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
      parent.printMessage(name + " plays land " + c.name + ".");
      return true;
    }
    return false;
  }

  /* Execute a move, which usually involves playing a card! */
  // TODO - Handle lands more smoothly?
  public boolean applyMove(Move m)
  {
    return playCard(m.card,m.targets);
  }

  /* Play a card! Defer to parent boardstate where appropriate. */
  // TODO: Counterspells?
  public boolean playCard(Card c, Targetable[] t)
  {
    if(c.isCreature())
    {
      if(hand.remove(c))
      {
        payMana(c);
        creatures.add(c);
        //System.out.println(name + " plays " + c.name + ".");
        parent.creatureEnteredBattlefield(c);
        return true;
      }
    }
    else
    {
      if(hand.remove(c))
      {
        payMana(c);
        parent.resolveCard(c,this,t);
        graveyard.add(c);
        return true;
      }
    }
    return false;
  }

  /* Figure out how to pay mana for a card, tapping additional lands if necessary.
   * For now, we deal with colorless mana in a naive way.
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

  // Do nothing more! Return true if we don't advance the turn.
  public boolean endPhase()
  {
    // Empty the mana pool
    for(int i=0;i<Card.COLORS;i++)
    {
      manaPool[i] = 0;
    }
    // Signal the parent to move on
    return (parent.advancePhase());
  }

  // Damage on creatures is reset to 0 at end of turn.
  public void removeCreatureDamage()
  {
    for(Card c:creatures)
    {
      c.damage = 0;
    }
  }

  public void killCreature(Card c)
  {
    creatures.remove(c);
    graveyard.add(c);
    parent.creatureDied(c);
  }

  public void gainLife(int amount)
  {
    life += amount;
  }

  public void takeDamage(int amount)
  {
    life -= amount;
    if(life <= 0)
    {
      parent.lostGame(this);
    }
  }

  // TODO: multiplayer
  public void declareAttacks(Card[] atk)
  {
    for(Card c:atk)
      c.tap();
    parent.declareAttackers(atk);
  }

  public void declareBlocks(Card[][] blk)
  {
    parent.declareBlockers(blk);
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
          library.add(new Card(line, this));
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

  public void stackDeck()
  {
    
    //Demo 1
    library.add(new Card("Plains", this));
    library.add(new Card("Blessing of the Angel", this));
    library.add(new Card("Plains", this));
    library.add(new Card("Normal Guy", this));
    library.add(new Card("Plains", this));
    library.add(new Card("Great Hero", this));
    library.add(new Card("Strong Man", this));
    
    /*
    //Demo 2: Red
    library.add(new Card("Shock", this));
    library.add(new Card("Lightning Bolt", this));
    library.add(new Card("Mountain", this));
    library.add(new Card("Shock", this));
    library.add(new Card("Lightning Bolt", this));
    library.add(new Card("Mountain",this));
    library.add(new Card("Mountain", this));*/
  }

  public void drawCard()
  {
    if(library.size() > 0)
      hand.add(library.remove(library.size()-1));
    else
      parent.lostGame(this);
  }

  public void drawCards(int num)
  {
    for(int i=0;i<num;i++)
      drawCard();
  }

  public ArrayList<Move> determineAvailableMoves()
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

    ArrayList<Card> playableCardList = new ArrayList<Card>();
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
        playableCardList.add(c);
    }

    // Find possible combinations of targets for a spell
    ArrayList<Move> moveList = new ArrayList<Move>();
    EffectList effs;
    boolean targeted;
    int[] targetNums;
    Effect cur;
    int numeffs;
    int totalTargets;

    for(Card c:playableCardList)
    {
      effs = c.getEffects();
      numeffs = effs.size;
      targeted = false;
      targetNums = new int[numeffs];
      totalTargets = 1;
      for(int i=0;i<numeffs;i++)
      {
        cur = effs.get(i);
        if(cur.isTargetedEffect())
        {
          targeted = true;
          targetNums[i] = parent.countTargetables(cur, this);
          totalTargets*=targetNums[i];
        }
        else
          targetNums[i] = 1;
      }
      if (targeted)
      {
        Targetable[][] possibleTargets = new Targetable[numeffs][];
        for(int i=0; i<numeffs; i++)
        {
          cur = effs.get(i);
          possibleTargets[i] = parent.getTargetables(cur, this);
        }
        Targetable[] instanceTargets;
        int[] ind = new int[numeffs];
        int bi = 0;
        while(bi < totalTargets)
        {
          instanceTargets = new Targetable[numeffs];

          // Assign a particular set of targets
          for(int i=0;i<numeffs;i++)
          {
            instanceTargets[i] = possibleTargets[i][ind[i]];
          }
          moveList.add(new Move(c,instanceTargets));

          // Increment the index (use of bi lets us avoid additional checks here)
          for(int i=0;i<numeffs;i++)
          {
            if(++ind[i] < targetNums[i])
              break;
            else
              ind[i] = 0;
          }
          bi++;
        }
      }
      else
        moveList.add(new Move(c));
    }
    return moveList;
  }

  public ArrayList<Card> getPossibleAttackers()
  {
    ArrayList<Card> possibleAttackers = new ArrayList<Card>();
    for(Card c:creatures)
    {
      if(!(c.tapped || c.summoningsick))
      { 
        possibleAttackers.add(c);
      }
    }
    return possibleAttackers;
  }

  public ArrayList<Card> getPossibleBlockers()
  {
    ArrayList<Card> possibleBlockers = new ArrayList<Card>();
    for(Card c:creatures)
    {
      if(!c.tapped)
      { 
        possibleBlockers.add(c);
      }
    }
    return possibleBlockers;
  }

  /* Consider all combinations of attackers */
  // TODO: Choice of target
  public ArrayList<Card[]> determineAvailableAttackers()
  {
    ArrayList<Card[]> attackList = new ArrayList<Card[]>();
    ArrayList<Card> possAttackers = getPossibleAttackers();
    int maxAt = possAttackers.size();
    int tot = (int)Math.pow(2,maxAt);
    Card[] cur;
    int numAt;

    for(int i=0; i < tot; i++)
    {
      numAt = countOnes(i);
      cur = new Card[numAt];
      int j = 0;
      int k = 0;
      while(j < numAt)
      {
        if(((i >> k)&1) == 1)
        {
          cur[j] = possAttackers.get(k);
          j++;
        }
        k++;
      }
      attackList.add(cur);
    }
    return attackList;
  }

  /* Consider all combinations of blockers 
   * Format: n*2 array of blocker-attacker pairs.
   * FOR NOW, if there are too many possibilities we pick randomly.
   */

  public ArrayList<Card[][]> determineAvailableBlockers()
  {
    ArrayList<Card[][]> blockList = new ArrayList<Card[][]>();
    ArrayList<Card> possBlockers = getPossibleBlockers();
    int maxBl = possBlockers.size();

    Card[] attackers = parent.getAttackers();
    if(attackers == null)
      return blockList;

    int numAttackers = attackers.length;
    int adjNumAtk = numAttackers + 1; // Account for no block
    int tot = (int)Math.pow(adjNumAtk,maxBl); // (attackers + 1)^blockers total
    Card[][] cur; // Current block we are adding
    int numbl; // Number of blockers in this block
    int[] ind = new int[maxBl];

    if(tot > BLOCK_SEARCH_CUTOFF)
    {
      Random random = new Random();
      for(int i=0; i < BLOCK_SEARCH_CUTOFF; i++)
      {
        // Randomly generate a particular assignment of blockers
        for(int j=0;j<maxBl;j++)
        {
          ind[j] = random.nextInt(adjNumAtk);
        }

        numbl = 0;
        for(int j=0;j<maxBl;j++)
          if(ind[j] > 0)
            numbl++;
        cur = new Card[numbl][2]; // store blocker and corresponding attacker
        int k = 0;
        for(int j=0;j<maxBl;j++)
        {
          if(ind[j] > 0)
          {
            cur[k][0] = possBlockers.get(j);
            cur[k][1] = attackers[ind[j] - 1];
            k++;
          }
        }
  
        blockList.add(cur);
      }
    }

    for(int i=0; i < tot; i++)
    {
      numbl = 0;
      for(int j=0;j<maxBl;j++)
        if(ind[j] > 0)
          numbl++;
      cur = new Card[numbl][2]; // store blocker and corresponding attacker
      int k = 0;
      for(int j=0;j<maxBl;j++)
      {
        if(ind[j] > 0)
        {
          cur[k][0] = possBlockers.get(j);
          cur[k][1] = attackers[ind[j] - 1];
          k++;
        }
      }
  
      blockList.add(cur);

      // Increment the index (use of i lets us avoid additional checks here)
      for(int j=0;j<maxBl;j++)
      {
        if(++ind[j] < adjNumAtk)
          break;
        else
          ind[j] = 0;
      }
    }
    return blockList;
  }

  /* Used in determineAvailableAttackers to find how many creatures are
   * attacking at each i. */
  public int countOnes(int num)
  {
    int count = 0;
    while(num > 0)
    {
      count+=(num & 1);
      num = (num >> 1);
    }
    return count;
  }

  public void printBoard()
  {
    System.out.println();
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
    System.out.println(name + "'s life total: " + life);
    System.out.println("Cards in " + name + "'s library: " + library.size());
    System.out.println("Cards in " + name + "'s graveyard: " + graveyard.size());
    System.out.println();
  }

  /* Return true if we need a response from the AI (blocking etc) */
  public boolean parseTextCommand(String cmd)
  {
    switch(parent.phase)
    {
     case BoardState.MAIN1:
     case BoardState.MAIN2:
      if(cmd.matches("play .*"))
      {
        String n = cmd.substring(5);
        Card c;
        for(Move m:determineAvailableMoves())
        {
          c = m.card;
          if(n.equalsIgnoreCase(c.name + m.targetString()))
          {
            if(c.isLand())
              playLand(c);
            else
              playCard(c,m.targets);
            break;
          }
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
      break;

     case BoardState.ATTACK:
      if(cmd.matches("attack .*"))
      {
        String n = cmd.substring(7);
        String[] atkers = n.split("+");
        ArrayList<Card> declaredAttackers = new ArrayList<Card>();
        for(String a:atkers)
        {
          for(Card c:getPossibleAttackers())
          {
            if(a.equalsIgnoreCase(c.name) && !declaredAttackers.contains(c))
            {
              declaredAttackers.add(c);
              break;
            }
          }
        }
        if(declaredAttackers.size() > 0)
        {
          declareAttacks((Card[])declaredAttackers.toArray());
          return(parent.phase == BoardState.BLOCK);
        }
      }
      else if(cmd.matches("pass"))
      {
        endPhase();
        return(parent.phase == BoardState.BLOCK);
      }
      break;
     case BoardState.BLOCK:
      break;
    }
    if(cmd.matches("pass"))
    {
      endPhase();
    }
    return false;
  }

  public String toString()
  {
    return "Player " + id;
  }
}