/**
 * Created by Joseph on 3/31/2015.
 */
/*
NOTE: I removed Permanent.java to avoid inheritance headaches and other complications, so this class needs to be overhauled. I went ahead and changed a few things to Card rather than Creature, and fixed a typo blockler -> blocker*/
public class Combat {}
/*
    BoardState boardState;
    public Combat(BoardState boardState){
        this.boardState=boardState;
    }



    public void combatHandler(Card attacker, Card[] blockers){

    }

    public void combatHandler(Card attacker, Card blocker){
        attacker.toughness-=blocker.power;
        blocker.power-=attacker.toughness;

        if(attacker.toughness==0){
            boardState.creatures.remove(attacker);
        }
        if(blocker.toughness==0){
            return boardState.creatures.remove        }

    }

}

*/
