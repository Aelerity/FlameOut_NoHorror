package flame.unit;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import flame.bullets.*;
import flame.effects.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class ApathySentryUnit extends UnitEntity implements PlayerAbilityUnit{
    private int playerAbility;
    private float manualHealTime;
    private ApathyIUnit manualHealTarget;
    private String playerStatus = "Primary fire: laser. Aim at friendly Apathy to repair.";
    @Override public String[] playerAbilityNames(){ return new String[]{"Laser", "Repair allied Apathy"}; }
    @Override public int selectedPlayerAbility(){ return playerAbility; }
    @Override public void selectPlayerAbility(int index){ if(index >= 0 && index < 2) playerAbility = index; }
    @Override public String playerAbilityStatus(){ return playerStatus; }
    @Override public boolean usePlayerAbility(){
        if(!isPlayer()) return false;
        if(playerAbility == 0){ playerStatus = "Hold primary fire to shoot the laser."; return true; }
        if(manualHealTime > 0f){ playerStatus = "Repair is already charging."; return false; }
        ApathyIUnit target = PlayerTargets.friendlyApathy(this);
        if(target == null && owner != null && owner.isValid() && owner.team == team) target = owner;
        if(target == null){ playerStatus = "Aim at an allied Apathy."; return false; }
        manualHealTarget = target;
        manualHealTime = 180f;
        playerStatus = "Repair charging (3 seconds).";
        return true;
    }
    @Override public void cancelPlayerAbility(){ manualHealTime = 0f; manualHealTarget = null; playerStatus = "Repair stopped."; }
    float moveX, moveY, moveTime;
    Teamc target;
    float reload = 0f;
    boolean active;
    ApathyIUnit owner;

    float healDelay = 0f;
    float healFade = 0f;

    void moveSentry(float wx, float wy){
        moveX = wx;
        moveY = wy;
        moveTime = 0f;
    }

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void update(){
        super.update();
        
        if(isPlayer()){
            if(manualHealTime > 0f){
                if(manualHealTarget == null || !manualHealTarget.isValid() || manualHealTarget.team != team){ cancelPlayerAbility(); }
                else{
                    manualHealTime -= Time.delta;
                    if(manualHealTime <= 0f){
                        manualHealTarget.heal(manualHealTarget.maxHealth / 20f);
                        playerStatus = "Apathy repaired.";
                    }
                }
            }
            return;
        }
        if(manualHealTarget != null) cancelPlayerAbility();
        if(owner == null) return;

        if(!owner.isValid()){
            destroy();
            return;
        }

        float lx = (moveX - x) * 0.25f * moveTime;
        float ly = (moveY - y) * 0.25f * moveTime;
        moveTime = Mathf.clamp(moveTime + Time.delta / 20f);
        x += lx;
        y += ly;

        if(active){
            if(healDelay > 0){
                rotation = Angles.moveToward(rotation, angleTo(owner), 15f);
                healDelay -= Time.delta;
                healFade = Mathf.clamp(healFade + Time.delta / 12f);
                if(healDelay <= 0f){
                    owner.heal(owner.maxHealth / 20f);
                    reload = 0f;
                }
            }else{
                if(Units.invalidateTarget(target, team, x, y, FlameBullets.sentryLaser.range - 50f)){
                    target = null;
                }

                if(target != null){
                    rotation = Angles.moveToward(rotation, angleTo(target), 15f);
                    if(reload > 0){
                        reload -= Time.delta;
                        if(reload <= 0f){
                            Tmp.v1.trns(rotation, hitSize / 1.5f).add(x, y);
                            Sounds.shootLaser.at(Tmp.v1);
                            FlameBullets.sentryLaser.create(this, Tmp.v1.x, Tmp.v1.y, rotation);
                        }
                    }
                }else{
                    rotation = Angles.moveToward(rotation, angleTo(owner), 15f);
                }
            }
        }
    }

    @Override
    public float clipSize(){
        return 2000f;
    }

    @Override
    public void destroy(){
        super.destroy();
    }

    @Override
    public void draw(){
        float z = Layer.flyingUnitLow;
        TextureRegion r = type.region;
        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        type.drawSoftShadow(this);
        //Draw.color(Pal.shadow);
        //Draw.rect(r, x + UnitType.shadowTX, y + UnitType.shadowTY, r.width * Draw.scl * 0.5f, r.height * Draw.scl * 0.5f, rotation - 90f);
        if(manualHealTime > 0f && manualHealTarget != null){
            Draw.color(Pal.heal);
            Lines.stroke(2f);
            Lines.line(x, y, manualHealTarget.x, manualHealTarget.y);
        }
        if(healDelay > 0 && owner != null){
            Draw.color(Pal.heal);
            Lines.stroke(3f * healFade * (1f + Mathf.absin(1f, 0.5f)));
            Lines.line(x, y, owner.x, owner.y);
        }

        Draw.z(z);
        Draw.color();
        type.applyColor(this);
        Draw.rect(r, x, y, r.width * Draw.scl * 0.5f, r.height * Draw.scl * 0.5f, rotation - 90f);
        Draw.color();
        Draw.mixcol();
    }
}
