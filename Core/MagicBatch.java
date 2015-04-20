/* For processing of large batches of Magic games */
import java.util.Random;
public class MagicBatch
{
  public static final int EXHAUSTIVE = 1;
  public static final int HILL_CLIMBING = 2;

  public static void main(String[] args)
  {
    int method = HILL_CLIMBING;
    int maxHillSteps = 10;

    int trials = 1000;
    if(args.length>0)
      trials = Integer.parseInt(args[0]);

    String learnDeck = "RedDeck.txt";
    String[] dl = {"WhiteDeck.txt","BlackDeck.txt","RedDeck.txt","GreenDeck.txt"};

    int numHeurs = 4;
    double[] heur0 = {1,1,1,1};
    double[][] heur1 = {{1,3,1,1},{1,2,2,1},{1,4,1,1},{1,1,1,2}};

    double[] bestheur = new double[numHeurs];
    for(int i=0;i<numHeurs;i++)
      bestheur[i] = heur0[i];

    System.out.println("Testing starting rate...");
    double bestrate = batchMagic(1000, learnDeck, dl, heur0, heur1);
    System.out.println("Starting winrate: " + bestrate);

    Random random = new Random();

    if(method == EXHAUSTIVE)
    {
      for(int i=0;i<625;i++)
      {
        System.out.println("Testing heuristic " + i);
        for(int j=0;j<numHeurs;j++)
        {
          if(++heur0[j] < 5)
            break;
          else
            heur0[j] = 0;
        }
        double temp = batchMagic(trials, learnDeck, dl, heur0, heur1);
        if(temp > bestrate)
        {
          bestrate = temp;
          bestheur = new double[numHeurs];
          for(int j=0;j<numHeurs;j++)
          bestheur[j] = heur0[j];
        }
      }
    }
    else if(method == HILL_CLIMBING)
    {
      System.out.println("");
      double[] heur = new double[numHeurs];
      double bestsign = 0;
      int bestdir = 0;

      for(int i=0; i<numHeurs;i++)
        heur[i] = heur0[i];
      for(int k = 0; k < maxHillSteps; k++)
      {
        System.out.println("Computing hill step " + k + "....");
        for(int i = 0; i < numHeurs; i++)
        {
          heur[i]++;
          double temp = batchMagic(trials, learnDeck, dl, heur,heur1);
          if(temp > bestrate)
          {
            bestrate = temp;
            bestsign = 1;
            bestdir = i+1;
          }
          heur[i] = heur[i]-2;
          temp = batchMagic(trials, learnDeck, dl, heur,heur1);
          if(temp > bestrate)
          {
            bestrate = temp;
            bestsign = -1;
            bestdir = i+1;
          }
          heur[i]++;
        }
        if(bestdir > 0)
          heur[bestdir-1] += bestsign;
        else
          break;
      }
      bestheur = heur;
    }
    System.out.print("Heuristic: ");
    for(int i=0; i<numHeurs; i++)
      System.out.print(bestheur[i] + " ");
    System.out.println("Rate: " + bestrate);

    System.out.println("Testing found rate...");
    System.out.println(batchMagic(1000, learnDeck, dl, bestheur, heur1));
  }

  public static double batchMagic(int trials, String learnDeck, String[] decks, double[] heur0, double[][] heur1)
  {
    Card.loadCardList("cards.txt");
    BoardState currentState;
    boolean go;
    MoveEvaluator eval;

    int[] win = new int[]{0,0};
    String[] dl = new String[2];
    dl[0] = learnDeck;

    for(int d=0; d<decks.length; d++)
    {
      dl[1] = decks[d];
      eval = new MoveEvaluator(heur0, heur1[d]);
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