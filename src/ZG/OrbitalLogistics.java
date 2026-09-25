package ZG;

import ZG.Block.BlockLoad;
import ZG.Planet.PlanetLoad;
import ZG.Unit.UnitLoad;
import mindustry.mod.Mod;

public class OrbitalLogistics extends Mod {
    @Override
    public void loadContent(){
        UnitLoad.load();
        BlockLoad.load();
        PlanetLoad.load();
    }
}
