package flame.unit;

import flame.unit.shifts.*;
import flame.unit.weapons.*;
import flame.bullets.*;
import mindustry.ai.types.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class FlameUnitTypes{
    public static UnitType apathy, apathySentry, empathy, yggdrasil, despondency;

    public static void load(){
        apathy = new ApathyUnitType("apathy"){{
            flying = true;
            hitSize = 75;
            drag = 0.06f;
            weapons.add(new PlayerWeapon(FlameBullets.sentryLaser, 30f));
            
            handlers.add(new PrismShift("apathy"));
            handlers.add(new WeakLaserShift("apathy-weak-laser"));
            handlers.add(new AoEShift("apathy-aoe"));
            handlers.add(new SweepShift("apathy-sweep"));
            handlers.add(new StrongLaserShift("apathy-strong-laser"));

            controller = unit -> new ApathyIAI();

            fallEffect = fallEngineEffect = Fx.none;
            fallSpeed = 0f;
            deathExplosionEffect = Fx.none;
            createScorch = false;
            
            envEnabled = Env.any;
            envDisabled = 0;
        }};
        apathySentry = new UnitType("apathy-sweep-0"){{
            outlines = false;
            hidden = false;
            speed = 3f;
            weapons.add(new PlayerWeapon(FlameBullets.sentryLaser, 30f){
                @Override
                protected boolean enabled(Unit unit){
                    return unit.isPlayer() || ((ApathySentryUnit)unit).owner == null;
                }
            });
            useUnitCap = false;

            health = 9000f;
            drag = 0.5f;
            hitSize = 10f;

            controller = unit -> new FlyingAI();
            constructor = ApathySentryUnit::new;

            fallEffect = fallEngineEffect = Fx.none;
            deathExplosionEffect = Fx.none;
            createScorch = false;
            flying = true;

            envEnabled = Env.any;
            envDisabled = 0;
        }};

        empathy = new EmpathyUnitType("empathy");

        yggdrasil = new YggdrasilUnitType("yggdrasil"){{
            outlines = false;

            health = 2250000f;
            drag = 0.1f;
            hitSize = 25f;
            speed = 5f;
            rotateSpeed = 2f;
            armor = 20f;

            groundLayer = Layer.legUnit;
            hovering = true;
            allowLegStep = true;
            drawCell = false;
            pathCost = ControlPathfinder.costLegs;

            envEnabled = Env.any;
            envDisabled = 0;
        }};

        despondency = new DespondencyUnitType("despondency");
        // These flags also expose every unit in the editor and content selection dialogs.
        for(UnitType type : new UnitType[]{apathy, apathySentry, empathy, yggdrasil, despondency}){
            type.hidden = false;
            type.internal = false;
            type.alwaysUnlocked = true;
            type.playerControllable = true;
        }
        apathy.localizedName = "Apathy";
        apathySentry.localizedName = "Apathy Sentry";
        empathy.localizedName = "Empathy";
        yggdrasil.localizedName = "Yggdrasil";
        despondency.localizedName = "Despondency";
    }
}
