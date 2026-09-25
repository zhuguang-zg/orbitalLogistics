package ZG.Block.floor;

import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class SpaceStationFloor extends Floor{
    public SpaceStationFloor(String name){
        super(name);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return tile.floor() == Blocks.space;
    }
}