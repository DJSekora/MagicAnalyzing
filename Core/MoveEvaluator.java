import java.util.ArrayList;
public class MoveEvaluator
{
  public MoveEvaluator()
  {
  }

  public void selectMove(Player player)
  {
    ArrayList<Card> options = player.determineAvailableMoves();
    // First pass: play lands
    for(Card c:options)
      if(c.isLand())
      {
        player.playLand(c);
        return;
      }
    // Second pass: make the first move in the list
    for(Card c:options)
    {
      player.playCard(c);
      return;
    }
  }
}