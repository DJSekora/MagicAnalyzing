 import java.util.ArrayList;
public class MoveEvaluator
{
  public static final int COLORS = 6;

  public MoveEvaluator()
  {
  }

  // This should MAYBE (PROBABLY) be moved to BoardState
  public static ArrayList<Card> determineAvailableMoves(BoardState state, int player)
  {
    int[] mana = new int[COLORS];

    // Start by considering mana currently left in pool.
    for(int i = 0; i<COLORS;i++)
      mana[i] = state.manaPool[player][i];

    /* See what mana we can get from untapped lands
     * For now, we just have lands as tapping for their "cost"*/
    for(Card l:state.lands[player])
      for(int i=0;i<COLORS;i++)
        if(!l.tapped)
          mana[i]+=l.cost[i];

    int totalMana = 0;
    for(int i = 0; i<COLORS; i++)
      totalMana+=mana[i];

    ArrayList<Card> moveList = new ArrayList<Card>();
    for(Card c:state.hand[player])
    {
      boolean canPlay = true;
      if(c.isLand())
      {
        canPlay = (state.landsToPlay > 0);
      }
      else
      {
        int totalCost = c.cost[COLORS-1];
        for(int i = COLORS-2;i>=0;i--)
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
        moveList.add(c);
    }
    return moveList;
  }

  public void selectMove(BoardState state, int player)
  {
    ArrayList<Card> options = determineAvailableMoves(state,player);
    // First pass: play lands
    for(Card c:options)
      if(c.isLand())
      {
        state.playLand(player, c);
        return;
      }
    // Second pass: make the first move in the list
    for(Card c:options)
    {
      state.playCard(player, c);
      return;
    }
  }
}