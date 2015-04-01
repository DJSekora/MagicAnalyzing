/**
 * Created by Joseph on 3/31/2015.
 */
public class Combat {
    BoardState boardState;
    public Combat(BoardState boardState){
        this.boardState=boardState;
    }



    public void combatHandler(Creature attacker, Creature[] blockers){

    }

    public void combatHandler(Creature attacker, Creature blockler){
        attacker.toughness-=blockler.power;
        blockler.power-=attacker.toughness;

        if(attacker.toughness==0){
            boardState.creatures.remove(attacker);
        }
        if(blockler.toughness==0){
            return boardState.creatures.remove        }

    }

}
