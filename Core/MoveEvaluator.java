import java.util.ArrayList;
public class MoveEvaluator
{
  /* Set of measured attributes.
   * Own life, enemy life, total power, total toughness. */
  public static final int MYLIFE = 0;
  public static final int THEIRLIFE = 1;
  public static final int POWER = 2;
  public static final int TOUGHNESS = 3;

  public double[][] heuristics;
  
  //public double card = 2.0; // TODO: Drawing a card

  public MoveEvaluator(double[] h0, double[] h1)
  {
    heuristics = new double[2][];
    heuristics[0] = h0;
    heuristics[1] = h1;
  }

  public boolean stepAI(Player player)
  {
    BoardState orig = player.parent;
    switch(orig.phase)
    {
      case BoardState.MAIN1:
      case BoardState.MAIN2:
        return selectMove(player);
      case BoardState.ATTACK:
        return decideAttack(player);
      case BoardState.BLOCK:
        return decideBlock(player);
    }
    return false;
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

    // Second pass: Test each move, evaluate heuristics, pick the best one
    for(Move m:options)
    {
      Move n = new Move(m.numTargets()); // Make a blank move of the same size
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
      return(player.endPhase());
    }
  }

  /* Very similar workflow to selectMove */
  public boolean decideAttack(Player player)
  {
    ArrayList<Card[]> options = player.determineAvailableAttackers();

    BoardState orig = player.parent;
    BoardState sim;
    Player simplayer;
    Player blockingplayer;
    Card[] bestMove = null;
    // TODO: Consider opponent's moves 
    double bestVal = 0; // Don't attack if it's disadvantageous
    double val;
    int pid = player.id;

    //
    for(Card[] m:options)
    {
      Card[] n = new Card[m.length]; // Make a blank attack list of the same size
      sim = new BoardState(orig,m,n); // Copy the board state, copy move m to n
      simplayer = sim.players[pid]; // Figure out which one is you
      simplayer.declareAttacks(n);
      blockingplayer = sim.players[1-pid]; // TODO: Multiplayer
      decideBlock(blockingplayer);

      val = applyHeuristics(pid, orig, sim);

      if(val > bestVal)
      {
        bestVal = val;
        bestMove = m;
      }
    }

    if(bestMove!=null)
    {
      player.declareAttacks(bestMove);
      return !(orig.gameOver());
    }
    else
    {
    // If we can't do anything, end the phase.
      return(player.endPhase());
    }
  }

  public boolean decideBlock(Player player)
  {
    ArrayList<Card[][]> options = player.determineAvailableBlockers();

    BoardState orig = player.parent;
    BoardState sim;
    Player simplayer;
    Card[][] bestMove = null;
    // TODO: Consider opponent's moves 
    double bestVal = Integer.MIN_VALUE; // Best value so far (cheat a little with MIN)
    double val;
    int pid = player.id;

    for(Card[][] m:options)
    {
      Card[][] n = new Card[m.length][2]; // Make a blank block list of the same size
      sim = new BoardState(orig,m,n); // Copy the board state, copy move m to n
      simplayer = sim.players[pid]; // Figure out which one is you
      simplayer.declareBlocks(n);

      val = applyHeuristics(pid, orig, sim);

      if(val > bestVal)
      {
        bestVal = val;
        bestMove = m;
      }
    }

    // If we want to block....
    if(bestMove!=null)
    {
      player.declareBlocks(bestMove);
      return !(orig.gameOver());
    }
    else
    {
    // If we can't do anything, end the phase.
      return(player.endPhase());
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
    return heuristics[pid][MYLIFE]*mylifediff + heuristics[pid][THEIRLIFE]*opplifediff +
           creatureHeuristics(pid,oldbs,newbs);
  }

  // TODO: Multiplayer
  public double creatureHeuristics(int pid, BoardState oldbs, BoardState newbs)
  {
    double score = 0;

    // Difference in our creatures
    for(Card c:oldbs.players[pid].creatures)
    {
      score -= heuristics[pid][POWER]*c.power + heuristics[pid][TOUGHNESS]*c.toughness;
    }
    for(Card c:newbs.players[pid].creatures)
    {
      score += heuristics[pid][POWER]*c.power + heuristics[pid][TOUGHNESS]*c.toughness;
    }

    // Difference in enemy creatures
    for(Card c:oldbs.players[1-pid].creatures)
    {
      score += heuristics[pid][POWER]*c.power + heuristics[pid][TOUGHNESS]*c.toughness;
    }
    for(Card c:newbs.players[1-pid].creatures)
    {
      score -= heuristics[pid][POWER]*c.power + heuristics[pid][TOUGHNESS]*c.toughness;
    }

    return score;
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