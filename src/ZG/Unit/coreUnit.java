package ZG.Unit;

import mindustry.ai.types.BuilderAI;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

public class coreUnit {
    public static UnitType FantasyClassMKII;
    public static void load(){
        FantasyClassMKII = new UnitType("FantasyClassMKII"){{
            coreUnitDock=true;
            controller=u->new BuilderAI(true,1000f);
            constructor = UnitEntity::create;
            isEnemy=false;
            envDisabled=0;

            health=7500;
            armor=3500;
            itemCapacity=125;
            hitSize = 12f;

            range = 65f;
            faceTarget = true;
            targetPriority = -2;
            lowAltitude = false;
            mineWalls = false;
            mineFloor = false;
            mineHardnessScaling = false;
            flying = true;
            buildSpeed = 1.5f;
            drag = 0.04f;
            speed = 7.8f;
            rotateSpeed = 8f;
            accel = 0.08f;
            buildBeamOffset = 8f;
            payloadCapacity = 2f * 2f * 32 * 32;
            pickupUnits = false;
            vulnerableWithPayloads = true;

            fogRadius = 0f;
            targetable = false;
            hittable = false;

            engineOffset = 7.5f;
            engineSize = 3.4f;

            setEnginesMirror(
                new UnitEngine(35 / 4f, -13 / 4f, 2.7f, 315f),
                new UnitEngine(28 / 4f, -35 / 4f, 2.7f, 315f)
            );
        }};
    }
}
