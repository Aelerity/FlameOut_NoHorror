package flame.unit;

import flame.*;
import flame.effects.*;
import flame.bullets.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class EmpathyUnitType extends UnitType{
    public EmpathyUnitType(String name){
        super(name);
        flying = true;
        hitSize = 7;
        drag = 0.07f;

        health = 100f;
        speed = 6f;
        weapons.add(new flame.unit.weapons.PlayerWeapon(FlameBullets.pin, 6f));

        outlines = false;
        drawCell = false;
        bounded = false;

        createScorch = false;
        hidden = false;

        engineSize = -1f;

        controller = u -> new NullAI();

        envEnabled = Env.any;
        envDisabled = 0;
        constructor = EmpathyUnit::createUnit;

        deathExplosionEffect = FlameFX.empathyDecoyDestroy;
        deathSound = FlameSounds.expDecoy;

        description = "An agile energy combat unit.";
    }

    @Override
    public void init(){
        super.init();
        if(Vars.headless) EmpathyRegions.loadHeadless();
        for(StatusEffect s : Vars.content.statusEffects()){
            immunities.add(s);
        }
    }

    @Override
    public void load(){
        super.load();
        EmpathyRegions.load();
    }

    @Override
    public void update(Unit unit){
        if(!(unit instanceof EmpathyUnit)){
            unit.destroy();
            return;
        }
        super.update(unit);
    }
}
