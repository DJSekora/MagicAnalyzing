import java.util.Scanner;
public class MagicTUI
{
  public static BoardState currentState;
  public static void main(String[] args)
  {
    Card.loadCardList("cards.txt");
    Scanner in = new Scanner(System.in);
    String[] dl = {"WhiteDeck.txt","WhiteDeck.txt"};

    currentState = new BoardState(dl);
    boolean go = true;
    String command;
    MoveEvaluator eval = new MoveEvaluator();

    while(go)
    {
      command = in.nextLine();
      switch(command)
      {
        case "quit":
          go = false;
          break;
        case "moves":
          for(Card c:eval.determineAvailableMoves(currentState,0))
            System.out.println("Player 0 can play card " + c);
          break;
        case "print":
          currentState.print();
          break;
        case "go":
          eval.selectMove(currentState,0);
        default:
          //for()
          break;
      }
      
    }
    in.close();
  }
}