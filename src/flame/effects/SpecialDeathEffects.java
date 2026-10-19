package flame.effects;

import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.Fragmentation.*;
import flame.graphics.*;
import flame.graphics.VaporizeBatch.*;
import flame.unit.empathy.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;


public class SpecialDeathEffects{
    public static SpecialDeathEffects def;
    public static Seq<SpecialDeathEffects> all = new Seq<>();

    public Effect debrisEffect = FlameFX.heavyDebris, sparkEffect = FlameFX.destroySparks, explosionEffect = Fx.none;
    public Effect fragmentTrailEffect = null;
    public Cons<FragmentEntity> fragDeath = null;
    public Color disintegrateColor = Pal.rubble, internalColor = Pal.darkerMetal;
    public boolean solid = true, hasInternal = false;
    public Sound deathSound = Sounds.explosion, primaryDeathSound = Sounds.none;

    public static void load(){
        def = new SpecialDeathEffects();
    }

    public static SpecialDeathEffects get(MappableContent content){
        return def;
    }

    public static SpecialDeathEffects get(String name){
        return def;
    }

    public SpecialDeathEffects(){
        all.add(this);
    }

    protected void postLoad(){
        //
    }

    public void cutAlt(Unit u){
        explosionEffect.at(u.x, u.y, u.hitSize / 2f);
        deathSound.at(u.x, u.y, 1f, 1f);
    }

    public void hit(Sized ent, float x, float y, float rotation, float scl){
        //explosionEffect.at(x, y, (u.hitSize / 2f) * scl);
        explosionEffect.at(x, y, (ent.hitSize() / 2f) * scl);
    }

    public void disintegrateUnit(Unit u, float x1, float y1, float x2, float y2, float width){
        Vec2 vec = Utils.v, vec2 = Utils.vv;
        float rotation = Angles.angle(x1, y1, x2, y2);

        disintegrateUnit(u, x1, y1, x2, y2, width, (d, within) -> {
            if(within){
                d.disintegrating = true;

                Vec2 n = Intersector.nearestSegmentPoint(x1, y1, x2, y2, d.x, d.y, vec);
                float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / (width / 2f));
                float force = Interp.pow2.apply(dst) * Mathf.random(0.9f, 1.5f);

                d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), Interp.pow2.apply(dst));

                vec2.trns(rotation, force, Mathf.range(0.07f * dst));
                d.vx = vec2.x;
                d.vy = vec2.y;
                d.vr = Mathf.range(5f) * dst;
                d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                d.zOverride = Layer.flyingUnit;
            }
        });
    }

    public void disintegrateUnit(Unit u, float x1, float y1, float x2, float y2, float width, VaporizeHandler handler){
        FlameOut.vaporBatch.discon = d -> d.scorchColor.set(disintegrateColor);
        FlameOut.vaporBatch.switchBatch(x1, y1, x2, y2, width, u::draw, handler);
    }

    public void deathUnit(Unit u, float x, float y, float rotation){
        deathUnit(u, x, y, rotation, null);
    }

    public void deathUnit(Unit u, float x, float y, float rotation, Cons<FragmentEntity> alt){
        //float ang = Angles.angle(x, y, u.x, u.y);
        //Fx.dynamicExplosion.at(u.x, u.y, u.hitSize / 2f / 12f);
        debrisEffect.at(u.x, u.y, rotation, u.type.outlineColor, u.hitSize / 2f);
        explosionEffect.at(u.x, u.y, u.hitSize / 2f);
        Sound sound = primaryDeathSound != Sounds.none ? primaryDeathSound : deathSound;
        sound.at(u.x, u.y, 1f, 1.5f);

        Tmp.v1.trns(rotation - 180f, u.hitSize / 2).add(u.x, u.y);
        sparkEffect.at(Tmp.v1.x, Tmp.v1.y, rotation, u.hitSize / 2f);

        //UnitType type = u.type;
        FragmentationBatch batch = FlameOut.fragBatch;
        Tmp.v1.trns(rotation - 180f, 80f).add(x, y);
        float sx = Tmp.v1.x, sy = Tmp.v1.y;
        //batch.updateCircle();
        batch.baseElevation = u.type.shadowElevationScl * (u.isFlying() ? 1f : 0.25f);
        if(alt != null){
            batch.fragFunc = alt;
        }else{
            batch.fragFunc = e -> {
                float dx = (e.x - u.x) / 65f + u.vel.x;
                float dy = (e.y - u.y) / 65f + u.vel.y;
                float dx2 = (e.x - sx) / 15f;
                float dy2 = (e.y - sy) / 15f;

                float adst = Utils.angleDistSigned(Angles.angle(sx, sy, e.x, e.y), rotation);
                float scl = Mathf.pow(Mathf.clamp((90 - Math.abs(adst)) / 90f), 2.5f) + Mathf.pow(Mathf.clamp((30 - Math.abs(adst)) / 30f), 3f);
                e.vx = dx + dx2 * scl;
                e.vy = dy + dy2 * scl;
                e.vr = (Mathf.pow(Math.abs(adst), 0.5f) * (adst > 0 ? 1 : -1)) / 15 + Mathf.range(1f);
                //e.lifetime = 180f;
                e.vz = Mathf.random(-0.01f, 0.1f);
            };
        }
        //batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, ang, tex);
        batch.onDeathFunc = fragDeath;
        batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, Angles.angle(x, y, hx, hy), Draw.getColor(), tex);
        batch.trailEffect = fragmentTrailEffect;
        batch.explosionEffect = explosionEffect != Fx.none ? explosionEffect : null;
        batch.fragColor = Color.white;
        batch.sound = deathSound;

        batch.switchBatch(u::draw);
    }

    public void deathBuilding(Building b, float x, float y, float rotation){
        //Fx.dynamicExplosion.at(b.x, b.y, b.hitSize() / 2f / 12f);
        debrisEffect.at(b.x, b.y, b.angleTo(x, y) + 180f, b.block.outlineColor, b.hitSize() / 2f);
        //FlameFX.destroySparks.at(b.x, b.y, ang, b.hitSize() / 2f);

        Tmp.v1.trns(rotation - 180f, b.hitSize() / 2).add(b.x, b.y);
        sparkEffect.at(Tmp.v1.x, Tmp.v1.y, rotation, b.hitSize() / 2f);

        Sound sound = primaryDeathSound != Sounds.none ? primaryDeathSound : deathSound;
        sound.at(b.x, b.y, 1f, 1.5f);

        Tmp.v1.trns(rotation - 180f, 250f).add(x, y);
        float sx = Tmp.v1.x, sy = Tmp.v1.y;

        FragmentationBatch batch = FlameOut.fragBatch;
        //batch.updateCircle();
        batch.baseElevation = 0.1f;
        batch.fragFunc = e -> {
            float dx = (e.x - b.x) / 65f;
            float dy = (e.y - b.y) / 65f;
            float dx2 = (e.x - sx) / 15f;
            float dy2 = (e.y - sy) / 15f;

            float adst = Utils.angleDistSigned(Angles.angle(sx, sy, e.x, e.y), rotation);
            float scl = Mathf.pow(Mathf.clamp((90 - Math.abs(adst)) / 90f), 2.5f) + Mathf.pow(Mathf.clamp((30 - Math.abs(adst)) / 30f), 3f);
            e.vx = dx + dx2 * scl * 0.3f;
            e.vy = dy + dy2 * scl * 0.3f;
            e.vr = (Mathf.pow(Math.abs(adst), 0.5f) * (adst > 0 ? 1 : -1)) / 15 + Mathf.range(1f);
            //e.lifetime = 180f;
            e.vz = Mathf.random(-0.01f, 0.1f);
        };
        batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, Angles.angle(x, y, hx, hy), tex);
        batch.explosionEffect = batch.trailEffect = null;
        batch.fragColor = Color.white;
        batch.sound = deathSound;

        batch.switchBatch(b::draw);
    }

}
