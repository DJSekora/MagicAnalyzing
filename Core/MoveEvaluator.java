import java.util.ArrayList;
public class MoveEvaluator
{
  /* Prototype set of measured attributes */
  public double ownlife = 1.0;
  public double enemylife = ownlife;
  public double power = 1.0;
  public double toughness = 1.0;
  public double card = 2.0; // Drawing a card

  public MoveEvaluator()
  {
  }

  /* Take in the player for which we wish to select a move.
     Make the move if possible, return true if a move was made. */
  public boolean selectMove(Player player)
  {
    ArrayList<Move> options = player.determineAvailableMoves();

    // First pass: play lands
    // TODO: Integrate lands more smoothly?
    for(Move m:options)
      if(m.card.isLand())
      {
        player.playLand(m.card);
        // TODO: Check for if a land makes us lose the game somehow?
        return true;
      }

    BoardState orig = player.parent;
    BoardState sim;
    Player simplayer;
    Move bestMove = null;
    // TODO: Consider opponent's moves 
    double bestVal = 0; // Change this if we consider opponent's moves
    double val;
    int pid = player.id;

    // Second pass: make the first move in the list
    for(Move m:options)
    {
      Move n = new Move(m.numTargets()); // Make a blank move of the
      sim = new BoardState(orig,m,n); // Copy the board state, copy move m to n
      simplayer = sim.players[pid]; // Figure out which one is you
      simplayer.applyMove(n);
      
      val = applyHeuristics(pid, orig, sim);
      if(val > bestVal)
      {
        bestVal = val;
        bestMove = m;
      }
    }

    if(bestMove!=null)
    {
      player.applyMove(bestMove);
      return !(orig.gameOver());
    }
    else
    {
    // If we can't do anything, end the phase.
      player.endPhase();
      return false;
    }
  }


  /* Apply the heuristics to the calculated values! Only look at life for now. */
  public double applyHeuristics(int pid, BoardState oldbs, BoardState newbs)
  {
    int mylifediff = newbs.players[pid].life - oldbs.players[pid].life;
    int opplifediff = 0;
    for(int i=0;i<oldbs.numplayers;i++)
      if(i != pid)
        opplifediff -= (newbs.players[i].life - oldbs.players[i].life);
    return ownlife*mylifediff + enemylife*opplifediff;
  }
}

class Move
{
  public Card card;
  public Targetable[] targets;

  // Blank constructor, only used when generating a new simulated move
  public Move(int targetnum)
  {
    targets = new Targetable[targetnum];
  }

  public Move(Card c)
  {
    card = c;
  }

  public Move(Card c, Targetable[] t)
  {
    this(c);
    targets = t;
  }

  public boolean isTargeted()
  {
    return (targets != null);
  }

  public int numTargets()
  {
    if(targets!=null)
      return targets.length;
    else
      return 0;
  }

  public String targetString()
  {
    if(isTargeted())
    {
      String ret = "->"+targets[0].getName();
      for(int i=1;i<targets.length;i++)
        ret += " && " + targets[i].getName();
      return ret;
    }
    else
      return "";
  }
}