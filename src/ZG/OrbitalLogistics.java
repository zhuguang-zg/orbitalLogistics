package ZG;

import ZG.Block.BlockLoad;
import ZG.Plant.PlantLoad;
import mindustry.mod.Mod;

public class OrbitalLogistics extends Mod {
    @Override
    public void loadContent(){
        BlockLoad.load();
        PlantLoad.load();
    }
}
