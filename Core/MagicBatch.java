/* For processing of large batches of Magic games */

public class MagicBatch
{
  public static void main(String[] args)
  {
    int trials = 1000;
    if(args.length>0)
      trials = Integer.parseInt(args[0]);

    Card.loadCardList("cards.txt");
    BoardState currentState;
    String[] dl = {"WhiteDeck.txt","WhiteDeck.txt"};
    MoveEvaluator eval = new MoveEvaluator();
    boolean go;

    int[] win = new int[]{0,0};

    for(int i=0; i<trials; i++)
    {
      currentState = new BoardState(dl);
      currentState.batch = true;
      go = true;
      while(go)
      {
        eval.stepAI(currentState.getActivePlayer());
        go = !(currentState.gameOver());
      }
      win[currentState.getWinner()]++;
    }
    for(int i=0; i< 2;i++)
      System.out.println("Player " + i + " wins: " + win[i]);
  }
}