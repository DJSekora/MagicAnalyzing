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

  // Initialize a blank BoardState
  public BoardState()
  {
    players = new Player[numplayers];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player("Player " + i, this);
    }
    turn = 0;
    phase = MAIN1;
  }


  // The main constructor we will call to start a game.
  public BoardState(String[] deckFile)
  {
    this();
    for(int i = 0; i < numplayers; i++)
    {
      players[i].loadDeck(deckFile[i]);
      players[i].shuffleDeck();
      players[i].stackDeck();
      players[i].drawCards(STARTING_HAND_SIZE);
    }
  }

  // Copy constructor (for AI state evals)
  public BoardState(BoardState old)
  {
    players = new Player[numplayers];

    for(int i = 0; i < numplayers; i++)
    {
      players[i] = new Player(old.players[i], this);
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
        endTurn();
        phase = UNTAP;
        doUntap();
        break;
      default:
        System.out.println("INVALID PHASE REACHED!");
    }
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
    printMessage("It is now " + players[turn].name + "'s turn.");
    return;
  }

  public void doUntap()
  {
    players[turn].untap();
    advancePhase();
  }

  public void doDraw()
  {
    players[turn].drawCard();
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
    //printMessage(p.name + " plays " + c.name + ".");
  }

  // Handle a player losing the game
  // TODO: Multiplayer
  public void lostGame(Player p)
  {
    int i = 0;
    /* Find the losing player. Later on, we can remove from the array and
     * keep playing. */
    while (players[i] != p)
      i++;
    printMessage(p.name + " lost the game!");
    System.exit(0);
  }

  // Placeholders for future event handling
  public void creatureEnteredBattlefield(Card c)
  {
    
  }
  public void creatureDied(Card c)
  {

  }

  /* Right now, we just print to standard output, but maybe someday we'll have a GUI or
   * something.... */
  public void printMessage(String s)
  {
    System.out.println(s);
  }

  public void print()
  {
    for(int i=0;i<numplayers;i++)
    {
      players[i].printBoard();
    }
  }
}