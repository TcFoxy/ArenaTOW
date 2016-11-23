package io.github.TcFoxy.ArenaTOW.BattleArena.objects.spawns;

/**
 * @author alkarin
 */
public class SpawnIndex {
    public final int teamIndex;
    public final int spawnIndex;

    public SpawnIndex(int teamIndex) {
        this(teamIndex, 0);
    }
    public SpawnIndex(int teamIndex, int spawnIndex) {
        this.teamIndex = teamIndex;
        this.spawnIndex = spawnIndex;
    }
    
    //TODO change this to red/blue no need for a whole index
}
