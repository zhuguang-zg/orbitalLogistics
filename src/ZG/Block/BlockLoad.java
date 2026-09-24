package ZG.Block;

import ZG.Block.block.blocks;
import ZG.Block.floor.floors;

public class BlockLoad {
    public static void load(){
        floors.load();
        blocks.load();
    }
}
