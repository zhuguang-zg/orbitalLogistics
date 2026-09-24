package ZG.Block.block;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

import static ZG.Block.floor.floors.spacefloor;

public class FloorRemover extends Block{
    public FloorRemover(String name){
        super(name);
        size = 1;
        update = true;
        solid = true;//实体方块，不允许普通陆地单位走上去
        hasItems = false;
        hasLiquids = false;
        buildTime = 1f;
        buildVisibility = BuildVisibility.shown;
        category = Category.effect;
        requirements = ItemStack.with(Items.copper, 3, Items.silicon, 1);
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
