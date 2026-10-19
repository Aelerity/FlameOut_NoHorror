package flame.unit.weapons;

import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/** Native aim/fire controls for units whose boss attacks otherwise run inside custom AI. */
public class PlayerWeapon extends Weapon{
    public PlayerWeapon(BulletType projectile, float reloadTime){
        super("");
        bullet = projectile;
        reload = reloadTime;
        mirror = false;
        rotate = true;
        rotateSpeed = 360f;
        shootCone = 360f;
        x = y = shootY = 0f;
        shootSound = Sounds.shootLaser;
    }

    protected boolean enabled(Unit unit){
        return unit.isPlayer() && (!(unit instanceof flame.unit.PlayerAbilityUnit abilities) || abilities.nativePlayerWeapon());
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        if(enabled(unit)) super.update(unit, mount);
    }
}
