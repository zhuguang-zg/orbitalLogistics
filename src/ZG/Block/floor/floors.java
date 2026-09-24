package ZG.Block.floor;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BuildVisibility;

public class floors {
    public static Floor spacefloor;
public static void load(){
    spacefloor=new Floor("space-station-floor");
    spacefloor.requirements(Category.effect, BuildVisibility.shown, ItemStack.with(Items.copper, 5));
}
}
