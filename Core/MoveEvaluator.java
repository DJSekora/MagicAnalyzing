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
      player.playCard(m.card,null);
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
}