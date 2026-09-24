package ZG;

import ZG.Block.BlockLoad;
import ZG.Planet.PlanetLoad;
import mindustry.mod.Mod;

public class OrbitalLogistics extends Mod {
    @Override
    public void loadContent(){
        BlockLoad.load();
        PlanetLoad.load();
    }
}
