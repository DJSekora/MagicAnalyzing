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
    while(go)
    {
      command = in.nextLine();
      switch(command)
      {
        case "quit":
          go = false;
          break;
        default:
          currentState.print();
          break;
      }
      
    }
    in.close();
  }
}