package flame.unit;

import arc.util.*;
import flame.unit.empathy.*;
import flame.unit.weapons.*;
import flame.unit.weapons.EndDespondencyWeapon.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;
import mindustry.type.*;

public class DespondencyUnit extends LegsUnit implements PlayerAbilityUnit{
    private int playerAbility;
    public boolean playerFinisher;
    private String playerStatus = "Primary fire controls all regular weapons; anti-air remains automatic.";
    @Override public String[] playerAbilityNames(){ return new String[]{"All regular weapons", "Full finisher (enemy unit)"}; }
    @Override public int selectedPlayerAbility(){ return playerAbility; }
    @Override public void selectPlayerAbility(int index){ if(index >= 0 && index < 2) playerAbility = index; }
    @Override public String playerAbilityStatus(){ return playerStatus; }
    @Override public boolean usePlayerAbility(){
        if(!isPlayer()) return false;
        if(playerAbility == 0){ playerStatus = "Hold primary fire: lasers, railguns, nuclear cannons, missiles; automatic anti-air."; return true; }
        Teamc target = PlayerTargets.enemy(this);
        if(!(target instanceof Unit enemy) || !enemy.isValid()){ playerStatus = "Aim at an enemy unit to start the finisher."; return false; }
        if(playerFinisher){ playerStatus = "Finisher already active."; return false; }
        for(var mount : mounts){
            if(mount.weapon instanceof EndDespondencyWeapon){
                mount.target = target;
                playerFinisher = true;
                playerStatus = "Full finisher active (strays, strong enemies, main target).";
                return true;
            }
        }
        return false;
    }
    @Override public void cancelPlayerAbility(){
        playerFinisher = false;
        for(var mount : mounts) if(mount.weapon instanceof EndDespondencyWeapon weapon){ weapon.cancelPlayerFinisher((DespondencyMount)mount); }
        playerStatus = "Finisher stopped.";
    }
    float trueHealth, trueMaxHealth;
    float invFrames;
    float lastDamage = 0f;

    @Override
    public void update(){
        if(!isPlayer() && playerFinisher) cancelPlayerAbility();
        updateValues();
        super.update();
    }

    @Override
    public void rawDamage(float amount){
        if(EmpathyDamage.isNaNInfinite(amount)) return;
        if(invFrames <= 0f || amount > lastDamage){
            float lam = amount;

            amount -= lastDamage;
            lastDamage = lam;
            amount = Math.min(amount, type.health / 220f);
            trueHealth -= amount;
            super.rawDamage(amount);
            trueHealth = health;
            invFrames = 15f;
        }
    }

    @Override
    public boolean isGrounded(){
        return true;
    }

    void updateValues(){
        if(!EmpathyDamage.isNaNInfinite(health)) trueHealth = Math.max(trueHealth, health);

        health = trueHealth;
        maxHealth = trueMaxHealth;
        if(trueHealth > 0){
            elevation = 1f;
            dead = false;
        }else{
            elevation = 0f;
            dead = true;
        }
        if(invFrames > 0){
            invFrames -= Time.delta;
            if(invFrames <= 0f){
                lastDamage = 0f;
            }
        }
    }

    @Override
    public void setType(UnitType type){
        super.setType(type);
        trueMaxHealth = type.health;
    }

    @Override
    public SolidPred solidity(){
        return null;
    }

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void add(){
        if(!added){
            trueHealth = type.health;
            EmpathyDamage.exclude(this);
        }
        super.add();
    }

    @Override
    public void remove(){
        if(trueHealth > 0 && EmpathyDamage.containsExclude(id)) return;
        if(added){
            boolean valid = EmpathyDamage.removeExclude(this);
            if(valid){
                super.remove();
            }else{
                trueHealth = trueMaxHealth = type.health;
            }
        }
    }
}
