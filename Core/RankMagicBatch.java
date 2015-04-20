/* For processing of large batches of Magic games with bad rank based heuristics */
import java.util.Random;
public class RankMagicBatch
{
  public static void main(String[] args)
  {
    int trials = 100;
    if(args.length>0)
      trials = Integer.parseInt(args[0]);

    String learnDeck = "BlackDeck.txt";
    String[] dl = {"WhiteDeck.txt","BlackDeck.txt","RedDeck.txt"};//,"GreenDeck.txt"};

    int numHeurs = 5;
    int[] order0 = {0,1,2,3,4};
    int[][] order1 = {{2,0,1,3,4},{4,2,1,3,0},{1,4,1,1,1}};//,{1,0,2,3,4}};

    int[] bestorder = new int[numHeurs];
    for(int i=0;i<numHeurs;i++)
      bestorder[i] = order0[i];

    System.out.println("Testing starting order...");
    double bestrate = batchMagic(1000, learnDeck, dl, order0, order1);
    System.out.println("Starting winrate: " + bestrate);

    Random random = new Random();

    for(int i=1;i<120;i++)
    {
      System.out.println("Testing heuristic " + i);
      while(!validOrder(incrementOrder(order0))){}
      double temp = batchMagic(trials, learnDeck, dl, order0, order1);
      if(temp > bestrate)
      {
        bestrate = temp;
        bestorder = new int[numHeurs];
        for(int j=0;j<numHeurs;j++)
        bestorder[j] = order0[j];
      }
    }
    
    System.out.print("Order: ");
    for(int i=0; i<numHeurs; i++)
      System.out.print(bestorder[i] + " ");
    System.out.println("Rate: " + bestrate);

    System.out.println("Testing found rate...");
    System.out.println(batchMagic(1000, learnDeck, dl, bestorder, order1));
  }

  public static boolean validOrder(int[] order)
  {
    boolean ok = true; 
    for(int i=0; i<order.length; i++)
    {
      boolean hasNum=false;
      for(int j=0;j<order.length;j++)
        if(order[i] == j)
          hasNum = true;
      ok = (ok && hasNum);
    }
    return ok;
  }

  public static int[] incrementOrder(int[] old)
  {
    int numHeurs = old.length;
    for(int j=0;j<numHeurs;j++)
    {
      if((++old[j]) < numHeurs)
        break;
      else
        old[j] = 0;
    }
    return old;
  }

  public static double batchMagic(int trials, String learnDeck, String[] decks, int[] heur0, int[][] heur1)
  {
    Card.loadCardList("cards.txt");
    BoardState currentState;
    boolean go;
    RankMoveEvaluator eval;

    int[] win = new int[]{0,0};
    String[] dl = new String[2];
    dl[0] = learnDeck;

    for(int d=0; d<decks.length; d++)
    {
      dl[1] = decks[d];
      eval = new RankMoveEvaluator(heur0, heur1[d]);
      for(int i=0; i<trials; i++)
      {
        currentState = new BoardState(dl);
        currentState.batch = true;
        go = true;
        while(go)
        {
          eval.stepAI(currentState.getActivePlayer());
          //currentState.print();
  /*try{
          Thread.sleep(500);
  }
  catch(Exception e){}*/
          go = !(currentState.gameOver());
        }
        win[currentState.getWinner()]++;
      }
    }
    return ((double)win[0])/(trials*decks.length);
    //for(int i=0; i< 2;i++)
      //System.out.println("Player " + i + " wins: " + win[i]);
  }
}