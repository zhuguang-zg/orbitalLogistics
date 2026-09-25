package ZG.Block.floor;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

public class floors {
    public static Floor spacefloor;
public static void load(){
    spacefloor=new SpaceStationFloor("space-station-floor");
    spacefloor.requirements(Category.effect, BuildVisibility.shown, ItemStack.with(Items.copper, 5));
    spacefloor.alwaysUnlocked = true;        // ← 战役里可见
    spacefloor.envEnabled |= Env.space;

    spacefloor.envRequired = Env.space;
}
}
