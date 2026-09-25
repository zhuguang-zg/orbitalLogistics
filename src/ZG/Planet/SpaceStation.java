package ZG.Planet;

import ZG.OrbitalLogistics;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.graphics.g3d.PlanetGrid.Ptile;
import mindustry.maps.planet.AsteroidGenerator;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.type.SectorPreset;
import mindustry.world.meta.Env;

import static ZG.Block.block.blocks.floorRemover;
import static ZG.Block.block.blocks.spaceCore;
import static ZG.Block.floor.floors.spacefloor;
import static mindustry.Vars.*;

public class SpaceStation {
    public static ObjectMap<Planet, Planet> stations = new ObjectMap<>();
    public static ObjectMap<Planet, SectorPreset> maps = new ObjectMap<>();
    public static void load(){
        Seq<Planet> planets = content.planets().select(p -> p.visible && p.hasGrid());

        for (Planet p : planets){
            Planet station = new Planet("station-" + p.name,p,0.12f){{
                hasAtmosphere = false;
                updateLighting = false;
                sectors.add(new Sector(this, Ptile.empty));
                defaultEnv = Env.space;
                alwaysUnlocked = true;
                generator = new AsteroidGenerator();
            }};
            stations.put(p, station);
            p.updateGroup.add(station);
            station.defaultCore = spaceCore;
            String mapName = mods.getMod(OrbitalLogistics.class).name + "-station";
            SectorPreset preset = new SectorPreset(p.name, mapName, station, 0);
            maps.put(p, preset);
            preset.overrideLaunchDefaults = true;
            preset.allowLaunchLoadout = false;
            preset.allowLaunchSchematics = false;

            spacefloor.shownPlanets.add(station);
            floorRemover.shownPlanets.add(station);
        }
    }
}
