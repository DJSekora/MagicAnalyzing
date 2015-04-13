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

    // Simple text interface for 2 players (0 is AI, 1 is user)
    while(go)
    {
      command = in.nextLine();
      switch(command)
      {
        case "quit":
          go = false;
          break;
        case "moves":
          for(Move m:currentState.players[0].determineAvailableMoves())
          {
            System.out.println("Player 0 can play card " + m.card);
          }
          break;
        case "print":
          currentState.print();
          break;
        case "go":
          if(currentState.turn == 0)
            eval.selectMove(currentState.players[0]);
          break;
        default:
          if(currentState.turn == 1)
            currentState.players[1].parseTextCommand(command);
          break;
      }
      
    }
    in.close();
  }
}