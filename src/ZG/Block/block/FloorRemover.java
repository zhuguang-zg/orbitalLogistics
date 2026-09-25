package ZG.Block.block;

import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;

import static ZG.Block.floor.floors.spacefloor;

public class FloorRemover extends Block{
    public FloorRemover(String name){
        super(name);
        rebuildable = false;//可重建的
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return tile.floor() == spacefloor;
    }
    public class FloorRemoverBuild extends Building {
        @Override
        public void updateTile(){
            if (tile.floor() == spacefloor){
                tile.setFloorNet(Blocks.space);
            }
            this.kill();
        }
    }
}
