/* Class to represent the board state of a Magic: The Gathering game.
 *
 * Much of the content migrated over to Player.java (where it made more sense)
 */

import java.util.ArrayList;
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

  int numplayers = 2;
  Player[] players;

  public int turn;
  public int phase;
  public int priority; // Never pass priority during a simulation as of now!

  public Card[] attackers;
  public Card[][] blockers;

  // Is this top level
  public boolean actual;
  public boolean batch;

  // Initialize a blank BoardState
  public BoardState()
  {
    players = new Player[numplayers];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player("Player " + i, this, i);
    }
    turn = 0;
    priority = 0;
    phase = MAIN1;

    // This is a top level board state
    actual = true;
  }


  // The main constructor we will call to start a game.
  public BoardState(String[] deckFile)
  {
    this();
    for(int i = 0; i < numplayers; i++)
    {
      players[i].loadDeck(deckFile[i]);
      players[i].shuffleDeck();
      //players[i].stackDeck();
      players[i].drawCards(STARTING_HAND_SIZE);
    }
  }

  // Copy constructor (for AI state evals)
  public BoardState(BoardState old, Move m, Move n)
  {
    players = new Player[numplayers];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player(old.players[i], this, m, n);
    }

    turn = old.turn;
    phase = old.phase;

    actual = false;
  }

  // Copy constructor (for AI attack eval)
  public BoardState(BoardState old, Card[] m, Card[] n)
  {
    players = new Player[numplayers];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player(old.players[i], this, m, n);
    }

    turn = old.turn;
    phase = old.phase;

    actual = false;
  }

  // Copy constructor (for AI block eval)
  public BoardState(BoardState old, Card[][] m, Card[][] n)
  {
    players = new Player[numplayers];
    attackers = new Card[old.attackers.length];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player(old.players[i], this, m, n, old.attackers, this.attackers);
    }

    turn = old.turn;
    phase = old.phase;

    actual = false;
  }

  // Advance to the next phase! We skip a lot of phases for now, because reasons.
  public boolean advancePhase()
  {
    priority = turn;
    switch(phase)
    {
      case UNTAP:
        phase = UPKEEP;
        printMessage("Phase is now " + phaseName());
        doUpkeep();
        break;
      case UPKEEP:
        phase = DRAW;
        printMessage("Phase is now " + phaseName());
        doDraw();
        break;
      case DRAW:
        phase = MAIN1;
        printMessage("Phase is now " + phaseName());
        break;
      case MAIN1:
        phase = ATTACK;
        printMessage("Phase is now " + phaseName());
        break;
      case ATTACK:
        if(attackers == null)
        {
          phase = MAIN2;
        }
        else
        {
          phase = BLOCK;
          priority = 1-turn; // TODO: Multiplayer
        }
        printMessage("Phase is now " + phaseName());
        break;
      case BLOCK:
        phase = DAMAGE;
        printMessage("Phase is now " + phaseName());
        doCombatDamage();
        break;
      case DAMAGE:
        phase = MAIN2;
        printMessage("Phase is now " + phaseName());
        break;
      case MAIN2:
        endTurn();
        phase = UNTAP;
        printMessage("Phase is now " + phaseName());
        doUntap();
        return false; // TODO: Make sure this is ok here
      default:
        System.out.println("INVALID PHASE REACHED!");
    }
    return true;
  }

  public void endTurn()
  {
    for (Player p:players)
    {
      p.removeCreatureDamage();
    }
    advanceTurn();
  }

  public void advanceTurn()
  {
    turn++;
    if(turn >= numplayers)
      turn = 0;
    priority = 1-turn; // TODO: Multiplayer
    printMessage("It is now " + players[turn].name + "'s turn.");
    return;
  }


  /* These phases involve game actions and do not leave time for player action, even
   * in the full game. */
  public void doUntap()
  {
    players[turn].untap();
    advancePhase();
  }

  public void doUpkeep()
  {
    for(Card c:players[turn].creatures)
      c.summoningsick = false;
    advancePhase();
  }

  public void doDraw()
  {
    players[turn].drawCard();
    advancePhase();
  }

  public void declareAttackers(Card[] atk)
  {
    attackers = atk;
    if(actual && !batch)
    {
      if(atk != null)
        printMessage(players[turn].name + " attacks with " + cardArrayToString(atk));
      else
        printMessage(players[turn].name + " does not attack.");
    }
    advancePhase();
  }

  public void declareBlockers(Card[][] blk)
  {
    blockers = blk;
    if(actual && !batch)
    {
      if(blk != null)
      {
        String blockMessage = "";
        for(Card[] pair:blk)
        {
          blockMessage+=" <blocks " + pair[1].name + " with " + pair[0].name + ">";
        }
        printMessage(players[turn].name + blockMessage + ".");
      }
      else if(getAttackers() != null)
        printMessage(players[turn].name + " does not block.");
    }
    advancePhase();
  }

  // TODO: Assigning combat damage more intelligently/up to player choice
  // TODO: Allowing for special creatures that block more than one (not bad)
  public void doCombatDamage()
  {
    if(attackers!=null && attackers.length > 0)
    {
      for(Card a:attackers)
      {
        a.remPower = a.power;
      }
      if(blockers != null && blockers.length > 0)
        for(Card[] b:blockers)
        {
          int blockerAmount = b[0].power;
          b[1].dealAttackingDamage(b[0]);
          b[1].takeDamage(blockerAmount);
        }
      for(Card a:attackers)
      {
        // If we still have all our power left, we didn't do damage to a creature
        // TODO: Fix interaction with trample-less removal of a creature after blocks
        if(a.remPower == a.power)
          getDefendingPlayer().takeDamage(a.power);
      }
      attackers = null;
      blockers = null;
    }
    advancePhase();
  }

  /* Find the total number of targetable creatures and players
   * EXTENSION: Account for untargetable objects, possibly hexproof */

  public int countTargetables(Effect e, Player p)
  {
    int numTargets = 0;

    // For now, we can only target creatures and players...
    if (e.canTarget(Effect.CREATURE))
    {
      numTargets += countTargetableCreatures(p);
    }
    if (e.canTarget(Effect.PLAYER))
    {
      numTargets += countTargetablePlayers(p);
    }
    return numTargets;
  }

  public Targetable[] getTargetables(Effect e, Player p)
  {
    int numT = countTargetables(e,p);
    Targetable[] ret = new Targetable[numT];
    int i=0;

    // For now, we can only target creatures and players...
    if (e.canTarget(Effect.CREATURE))
    {
      Card[] tc = getTargetableCreatures(p);
      for(Card t:tc)
      {
        ret[i++] = t;
      }
    }
    if (e.canTarget(Effect.PLAYER))
    {
      Player[] tp = getTargetablePlayers(p);
      for(Player t:tp)
      {
        ret[i++] = t;
      }
    }
    return ret;
  }

  public int countTargetableCreatures(Player p)
  {
    int ret = 0;
    for(Player pl:players)
      ret+=pl.creatures.size();
    return ret;
  }

  public Card[] getTargetableCreatures(Player p)
  {
    Card[] ret = new Card[countTargetableCreatures(p)];
    int i = 0;
    for(Player pl:players)
      for(Card c:pl.creatures)
        ret[i++] = c;
    return ret;
  }

  public int countTargetablePlayers(Player p)
  {
    return numplayers;
  }

  public Player[] getTargetablePlayers(Player p)
  {
    return players;
  }

  public Card[] getAttackers()
  {
    return attackers;
  }

  // TODO: Make this actually interesting and useful (multiplayer)
  public Player getDefendingPlayer()
  {
    return players[1-turn];
  }

  public Player getActivePlayer()
  {
    return players[priority];
  }

  // Handle card resolution here for proper scoping
  public void resolveCard(Card c, Player p, Targetable[] targets)
  {
    EffectList effects = c.getEffects();
    for(int i=0;i<effects.size;i++)
    {
      Effect e = effects.get(i);
      switch(e.type)
      {
        case Effect.GAIN_LIFE:
          p.gainLife(e.amount);
          break;
        case Effect.DEAL_DAMAGE_TO_TARGET:
          targets[i].takeDamage(e.amount);
          break;
        case Effect.DESTROY_TARGET:
          ((Card)targets[i]).destroy();
        default:
          break;
      }
    }
    printMessage(p.name + " plays " + c.name + targetingString(targets)+".");
  }

  // Handle a player losing the game
  // TODO: Multiplayer better
  public void lostGame(Player p)
  {
    p.lost = true;
    printMessage(p.name + " lost the game!");
  }

  // Placeholder for future possible event handling
  public void wonGame()
  {
  }

  public boolean gameOver()
  {
    int active = 0;
    for(Player p:players)
      if(!p.lost)
        active++;
    if(active > 1)
      return false;
    return true;
  }

  public int getWinner()
  {
    for(Player p:players)
      if(!p.lost)
        return p.id;
    return -1;
  }

  // Placeholders for future event handling
  public void creatureEnteredBattlefield(Card c)
  {
    c.summoningsick = true;
    printMessage(c.controller + " played " + c + ".");
  }

  public void creatureDied(Card c)
  {
    printMessage(c.controller + "'s " + c + " died.");
  }

  /* Right now, we just print to standard output, but maybe someday we'll have a GUI or
   * something.... */
  public void printMessage(String s)
  {
    if(actual && !batch)
      System.out.println(s);
  }

  // Slightly modified version of the one in Move, for prettier printing.
  public String targetingString(Targetable[] t)
  {
    if(t!=null && t.length>0)
    {
      String ret = " targeting "+t[0].getName();
      for(int i=1;i<t.length;i++)
        ret += " AND " + t[i].getName();
      return ret;
    }
    else
      return "";
  }

  public static String cardArrayToString(Card[] cards)
  {
    String ret = "|";
    for(Card c:cards)
      ret+=(c.name + "|");
    return ret;
  }

  public void print()
  {
    System.out.println("Phase: " + phaseName());
    for(int i=0;i<numplayers;i++)
    {
      players[i].printBoard();
    }
  }

  public String phaseName()
  {
    switch(phase)
    {
      case UNTAP:
        return "Untap";
      case UPKEEP:
        return "Upkeep";
      case DRAW:
        return "Draw Step";
      case MAIN1:
        return "Main Phase 1";
      case COMBAT:
        return "Start of Combat";
      case ATTACK:
        return "Declare Attackers";
      case BLOCK:
        return "Declare Blockers";
      case DAMAGE:
        return "Combat Damage Step";
      case ENDCOMBAT:
        return "End of Combat Step";
      case MAIN2:
        return "Main Phase 2";
      case END:
        return "End Step";
      case CLEANUP:
        return "Cleanup Step";
    }
    return "";
  }
}