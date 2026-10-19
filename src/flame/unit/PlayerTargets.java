package flame.unit;

import arc.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class PlayerTargets{
    private static Unit cursorOwner;
    private static float cursorX, cursorY;

    public static void captureCursor(Unit unit){
        // Native aim is clamped to a weapon's minimum range; ability targeting needs the raw cursor.
        // Keep the last world position while the player clicks the ability panel.
        if(!Vars.headless && Vars.player != null && Vars.player.unit() == unit &&
            Core.scene != null && !Core.scene.hasMouse() && !Core.scene.hasDialog()){
            Vec2 point = Core.input.mouseWorld();
            cursorOwner = unit;
            cursorX = point.x;
            cursorY = point.y;
        }
    }

    private static float aimX(Unit unit){
        return cursorOwner == unit ? cursorX : unit.controller() instanceof Player player ? player.mouseX : unit.aimX;
    }
    private static float aimY(Unit unit){
        return cursorOwner == unit ? cursorY : unit.controller() instanceof Player player ? player.mouseY : unit.aimY;
    }

    public static Vec2 aim(Unit unit, Vec2 out){
        captureCursor(unit);
        return out.set(aimX(unit), aimY(unit));
    }

    public static float angle(Unit unit){
        captureCursor(unit);
        return unit.angleTo(aimX(unit), aimY(unit));
    }

    /** Target the enemy under the cursor, never an unrelated enemy elsewhere on the map. */
    public static Teamc enemy(Unit unit){
        captureCursor(unit);
        float x = aimX(unit), y = aimY(unit);
        return Units.closestTarget(unit.team, x, y, 160f,
            target -> !target.dead && target.targetable(unit.team),
            target -> target.isValid());
    }

    public static ApathyIUnit friendlyApathy(Unit unit){
        captureCursor(unit);
        float x = aimX(unit), y = aimY(unit);
        ApathyIUnit best = null;
        float distance = 160f;
        for(Unit other : Groups.unit){
            if(other instanceof ApathyIUnit apathy && other.team == unit.team && other.isValid()){
                float dst = other.dst(x, y);
                if(dst < distance){ best = apathy; distance = dst; }
            }
        }
        return best;
    }
}
