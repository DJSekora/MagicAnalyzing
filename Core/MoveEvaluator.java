 import java.util.ArrayList;
public class MoveEvaluator
{
  public static int colors = 6;

  public MoveEvaluator()
  {
  }

  public static ArrayList<Move> determineAvailableMoves(BoardState state, int player)
  {
    int[] mana = new int[colors];

    // Start by considering mana currently left in pool.
    for(int i = 0; i<colors;i++)
      mana[i] = state.manaPool[player][i];

    /* See what mana we can get from untapped lands
     * For now, we just have lands as tapping for their "cost"*/
    for(Permanent l:state.lands[player])
      for(int i=0;i<colors;i++)
        if(!l.tapped)
          mana[i]+=l.cost[i];

    int totalMana = 0;
    for(int i = 0; i<colors; i++)
      totalMana+=mana[i];

    ArrayList<Move> moveList = new ArrayList<Move>();
    for(Card c:state.hand[player])
    {
      boolean canPlay = true;
      int totalCost = c.cost[colors-1];
      for(int i = colors-2;i>=0;i--)
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
      if(canPlay)
        moveList.add(new Move(c));
    }
    return moveList;
  }
}