package ZG.Block.block;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import static ZG.Unit.coreUnit.FantasyClassMKII;

public class blocks {
    public static CoreBlock spaceCore;
    public static Block floorRemover;
    public static void load (){
        spaceCore = new CoreBlock("spaceCore"){{
            health=15000;
            size=6;
            unitCapModifier = 25;
            armor=5000;//护甲
            update = true;
            solid = true;//实体方块，不允许普通陆地单位走上去
            hasItems = true;
            hasLiquids = false;
            rebuildable = false;//可重建的
            itemCapacity = 20000;
            unitType= FantasyClassMKII;
            buildVisibility = BuildVisibility.hidden;
        }};

        floorRemover = new FloorRemover("floorRemover"){{
            size = 1;
            update = true;
            solid = true;//实体方块，不允许普通陆地单位走上去
            hasItems = false;
            hasLiquids = false;
            buildTime = 1f;
            buildVisibility = BuildVisibility.shown;
            category = Category.effect;
            requirements = ItemStack.with(Items.copper, 3, Items.silicon, 1);


            alwaysUnlocked = true;
            envEnabled |= Env.space;
            envRequired = Env.space;
        }};
    }

}
