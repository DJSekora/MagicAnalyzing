import java.util.ArrayList;
public class MoveEvaluator
{
  public MoveEvaluator()
  {
  }

  public void selectMove(Player player)
  {
    ArrayList<Move> options = player.determineAvailableMoves();

    // First pass: play lands
    for(Move m:options)
      if(m.card.isLand())
      {
        player.playLand(m.card);
        return;
      }

    // Second pass: make the first move in the list
    // TODO: Targets
    for(Move m:options)
    {
      player.playCard(m.card,m.targets);
      return;
    }

    // If we can't do anything, end the phase.
    player.endPhase();
  }
}

class Move
{
  public Card card;
  public Targetable[] targets;

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