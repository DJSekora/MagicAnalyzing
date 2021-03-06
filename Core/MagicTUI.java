/* MagicTUI - Basic Text User Interface for our Magic: The Gathering simulation*/

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
    double[] heur0 = {1,1,1,1};
    double[] heur1 = {1,2,4,2};
    MoveEvaluator eval = new MoveEvaluator(heur0, heur1);

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
            System.out.println("Player 0 can play card " + m.card + m.targetString());
          }
          break;
        case "print":
          currentState.print();
          break;
        case "go":
          if(currentState.priority == 0)
            eval.stepAI(currentState.players[0]);
          break;
        default:
          if(currentState.priority == 1)
            if(currentState.players[1].parseTextCommand(command))
              eval.stepAI(currentState.players[0]);
          break;
      }
    }
    in.close();
  }
}