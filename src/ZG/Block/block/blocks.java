package ZG.Block.block;

import mindustry.content.UnitTypes;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;

public class blocks {
    public static CoreBlock spaceCore;
    public static Block floorRemover;
    public static void load (){
        spaceCore = new CoreBlock("spaceCore"){{
            health=15000;
            size=6;
            unitCapModifier = 25;
            armor=50;//护甲
            update = true;
            solid = true;//实体方块，不允许普通陆地单位走上去
            hasItems = true;
            hasLiquids = false;
            itemCapacity = 20000;
            unitType= UnitTypes.emanate;
            buildVisibility = BuildVisibility.hidden;
        }};
        floorRemover = new FloorRemover("floorRemover");
    }

}
