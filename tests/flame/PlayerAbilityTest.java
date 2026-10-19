package flame;

import arc.*;
import arc.util.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import java.lang.reflect.*;

import static mindustry.Vars.*;

/** Exercises the original pattern implementations through the same API used by the HUD. */
public class PlayerAbilityTest{
    private static ClassLoader loader;
    private static Method damageUpdate, damageReset;
    private static int scenarios;
    private static final IntSet createdEntities = new IntSet();
    private static final ObjectSet<Object> createdBullets = new ObjectSet<>();

    public static void run(ClassLoader modLoader) throws Exception{
        loader = modLoader;
        Class<?> damage = Class.forName("flame.unit.empathy.EmpathyDamage", true, loader);
        damageUpdate = damage.getMethod("update");
        damageReset = damage.getMethod("reset");
        groupRestoration();
        apathyForms();
        sentries();
        volley();
        empathyPatterns();
        finisher();
        Log.info("PASS: @ full player-ability scenarios", scenarios);
    }

    private static void groupRestoration() throws Exception{
        Object all = Groups.all, units = Groups.unit, bullets = Groups.bullet, draw = Groups.draw,
            sync = Groups.sync, build = Groups.build, physics = unitPhysics;
        Method swap = Class.forName("flame.entities.MockGroup", true, loader).getMethod("swap", Runnable.class, arc.func.Cons.class);
        try{
            swap.invoke(null, (Runnable)() -> { throw new IllegalStateException("expected fixture failure"); }, (arc.func.Cons<Entityc>)entity -> {});
            throw new AssertionError("fixture exception was swallowed");
        }catch(InvocationTargetException expected){
            require(expected.getCause() instanceof IllegalStateException, "unexpected copy error");
        }
        require(Groups.all == all && Groups.unit == units && Groups.bullet == bullets && Groups.draw == draw &&
            Groups.sync == sync && Groups.build == build && unitPhysics == physics, "copy failed to restore world groups/physics");
        scenarios++;
    }

    private static Unit possess(String name) throws Exception{
        if(player.unit() != null) player.unit(null);
        Mathf.rand.setSeed(0x51ab1eL + scenarios);
        Groups.clear();
        Time.clear();
        damageReset.invoke(null);
        player.add();
        createdBullets.clear();
        createdEntities.clear();
        Unit unit = content.unit("flameout-" + name).create(Team.sharded);
        unit.set(400f, 400f);
        unit.rotation = 0f;
        unit.add();
        player.unit(unit);
        return unit;
    }

    private static Unit target(Unit owner) throws Exception{
        Unit enemy = UnitTypes.reign.create(Team.crux);
        enemy.set(owner.x + 240f, owner.y);
        enemy.health = enemy.maxHealth = 1000000000f;
        enemy.add();
        state.teams.updateTeamStats();
        Groups.unit.updatePhysics();
        owner.aim(enemy.x, enemy.y);
        player.mouseX = enemy.x;
        player.mouseY = enemy.y;
        Object selected = Class.forName("flame.unit.PlayerTargets", true, loader).getMethod("enemy", Unit.class).invoke(null, owner);
        require(selected == enemy, "cursor target acquisition failed for " + owner.type.name);
        return enemy;
    }

    private static void tick(Unit unit, int frames) throws Exception{
        for(int i = 0; i < frames; i++){
            Time.delta = 1f;
            state.teams.updateTeamStats();
            Groups.unit.updatePhysics();
            Groups.bullet.updatePhysics();
            (player.unit().type == unit.type ? player.unit() : unit).update();
            for(Bullet bullet : Groups.bullet) createdBullets.add(bullet.type);
            for(Entityc entity : Groups.all) if(entity.getClass().getName().startsWith("flame.") && entity != unit) createdEntities.add(entity.id());
            Groups.bullet.update();
            damageUpdate.invoke(null);
            Time.update();
            require(player.unit() == unit && unit.isPlayer() && unit.controller() == player, "ability unexpectedly replaced the controlled body");
            require(!Groups.all.contains(entity -> entity instanceof Unit), "native unit inserted into global update group");
        }
    }

    private static Object call(Unit unit, String name, Class<?>[] types, Object... args) throws Exception{
        Method method = unit.getClass().getMethod(name, types);
        method.setAccessible(true);
        try{ return method.invoke(unit, args); }
        catch(InvocationTargetException error){ throw new RuntimeException(unit.type.name + ": " + name, error.getCause()); }
    }

    private static Object call(Unit unit, String name) throws Exception{ return call(unit, name, new Class<?>[0]); }
    private static String[] names(Unit unit) throws Exception{ return (String[])call(unit, "playerAbilityNames"); }

    private static void activate(Unit unit, int index) throws Exception{
        call(unit, "selectPlayerAbility", new Class<?>[]{int.class}, index);
        require((int)call(unit, "selectedPlayerAbility") == index, "ability selection was ignored");
        String label = names(unit)[index];
        require((boolean)call(unit, "usePlayerAbility"), "ability activation rejected: " + label + " / " + call(unit, "playerAbilityStatus"));
    }

    private static Object field(Object object, String name) throws Exception{
        for(Class<?> type = object.getClass(); type != null; type = type.getSuperclass()){
            try{
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            }catch(NoSuchFieldException ignored){}
        }
        throw new NoSuchFieldException(name);
    }

    private static void apathyForms() throws Exception{
        for(int form = 1; form <= 5; form++){
            Unit unit = possess("apathy");
            require(names(unit).length == 8, "missing Apathy forms or sentry commands");
            activate(unit, form);
            tick(unit, 400);
            require(field(unit, "current") == ((Seq<?>)field(unit.type, "handlers")).get(form - 1), "wrong transformation");
            if(form > 1){
                String bulletField = new String[]{"", "", "smallLaser", "aoe", "sweep", "bigLaser"}[form];
                Object projectile = Class.forName("flame.bullets.FlameBullets", true, loader).getField(bulletField).get(null);
                require(createdBullets.contains(projectile), "original form projectile missing: " + bulletField);
            }
            call(unit, "cancelPlayerAbility");
            require((boolean)call(unit, "nativePlayerWeapon"), "cancel did not restore primary fire");
            scenarios++;
            Log.info("PASS: player Apathy @", names(unit)[form]);
        }
    }

    private static void sentries() throws Exception{
        Unit unit = possess("apathy");
        target(unit);
        activate(unit, 6);
        tick(unit, 120);
        require(Groups.unit.count(u -> u.type == content.unit("flameout-apathy-sweep-0")) == 16, "sentries did not deploy");
        unit.health = unit.maxHealth / 2f;
        activate(unit, 7);
        float damaged = unit.health;
        for(Unit sentry : Groups.unit.copy()) if(sentry.type == content.unit("flameout-apathy-sweep-0")){
            for(int i = 0; i < 350; i++) sentry.update();
        }
        require(unit.health > damaged, "sentry recall did not repair Apathy");
        scenarios += 2;
        // Standalone sentries can perform the same repair while the player possesses them.
        Unit sentry = possess("apathy-sweep-0");
        Unit ally = content.unit("flameout-apathy").create(Team.sharded);
        ally.set(650f, 400f);
        ally.add();
        ally.health = ally.maxHealth / 2f;
        sentry.aim(ally.x, ally.y);
        player.mouseX = ally.x;
        player.mouseY = ally.y;
        activate(sentry, 1);
        tick(sentry, 181);
        require(ally.health > ally.maxHealth / 2f, "player sentry repair did not work");
        scenarios++;
    }

    private static void volley() throws Exception{
        Unit unit = possess("yggdrasil");
        target(unit);
        activate(unit, 0);
        require((int)field(unit, "playerVolleyShots") == 24, "volley did not schedule every tentacle");
        tick(unit, 240);
        require((int)field(unit, "playerVolleyShots") == 0, "full tentacle volley did not finish");
        Seq<?> tentacles = (Seq<?>)field(unit, "tentacles");
        require(tentacles.contains(t -> !(boolean)Reflect.invoke(t, "canShoot")), "volley never fired tentacles");
        scenarios++;
    }

    private static void empathyPatterns() throws Exception{
        String[] required = {"Pin", "Spray", "Sword Barrage", "Laser Shotgun", "Ricochet", "Shine", "Magic", "Depower", "Rend", "Prime", "Dash", "Copy", "Black Hole", "Sword", "Hand", "Blast", "Count Down", "End", "Surround Laser"};
        for(String pattern : required){
            Unit unit = possess("empathy");
            Unit enemy = target(unit);
            String[] abilities = names(unit);
            int index = java.util.Arrays.asList(abilities).indexOf("Attack: " + pattern);
            require(index > 0, "original attack absent from selector: " + pattern);
            if(pattern.equals("Ricochet")){
                UnitTypes.dagger.weapons.first().bullet.create(enemy, enemy.team, unit.x + 80f, unit.y, 90f);
                Groups.bullet.updatePhysics();
            }
            int physicsBefore = ((Seq<?>)field(unitPhysics, "refs")).size;
            activate(unit, index);
            Object attack = field(unit, "activeAttack");
            require(attack.getClass().getSimpleName().replace("Attack", "").equals(pattern.replace(" ", "")), "wrong pattern selected");
            tick(unit, 1);
            boolean utilityEffect = pattern.equals("Depower") && (float)field(attack, "reload") > 0f;
            if(pattern.equals("Copy")){
                utilityEffect = !((Seq<?>)field(attack, "units")).isEmpty();
                require(utilityEffect, "Copy did not create a suspended unit");
                require(((Seq<?>)field(unitPhysics, "refs")).size == physicsBefore, "holographic copy leaked a physics body");
            }
            tick(unit, pattern.equals("End") ? 1599 : pattern.equals("Count Down") ? 649 : 349);
            boolean effect = utilityEffect || createdBullets.size > 0 || createdEntities.size > 0 || !enemy.isAdded() || enemy.health < enemy.maxHealth || unit.dst(400f, 400f) > 1f;
            if(pattern.equals("Copy")) effect |= !((Seq<?>)field(attack, "units")).isEmpty();
            require(effect, "selected pattern produced no gameplay effect: " + pattern);
            require(unit.isAdded(), "pattern removed controlled unit: " + pattern);
            call(unit, "cancelPlayerAbility");
            require((boolean)call(unit, "nativePlayerWeapon"), "pattern did not cancel: " + pattern);
            scenarios++;
            Log.info("PASS: player Empathy @", pattern);
        }
        for(String movement : new String[]{"Bullet Hell", "Orbit", "Random Teleport", "Tele Swap"}){
            Unit unit = possess("empathy");
            target(unit);
            int index = java.util.Arrays.asList(names(unit)).indexOf("Movement: " + movement);
            require(index > 0, "movement skill missing: " + movement);
            activate(unit, index);
            tick(unit, 80);
            require(unit.dst(400f, 400f) > 1f, "movement ability had no movement: " + movement);
            scenarios++;
        }
        Unit countdownUnit = possess("empathy");
        int countdownIndex = java.util.Arrays.asList(names(countdownUnit)).indexOf("Attack: Count Down");
        Object countdown = ((Seq<?>)field(countdownUnit, "attackAIs")).get(countdownIndex - 1);
        for(int stage = 0; stage < 6; stage++){
            for(Unit oldTarget : Groups.unit.copy()) if(oldTarget.team == Team.crux) oldTarget.remove();
            countdownUnit = player.unit();
            countdownUnit.set(400f, 400f);
            target(countdownUnit);
            activate(countdownUnit, countdownIndex);
            int frames = 0;
            while((int)field(countdown, "count") == stage && frames++ < 2400) tick(countdownUnit, 1);
            require((int)field(countdown, "count") == (stage + 1) % 6, "Count Down stage did not complete: " + stage);
            call(player.unit(), "cancelPlayerAbility");
            Log.info("PASS: player Empathy Count Down stage @", stage);
            scenarios++;
        }
        Unit unit = possess("empathy");
        Unit attacker = target(unit);
        Bullet incoming = UnitTypes.dagger.weapons.first().bullet.create(attacker, attacker.team, unit.x + 20f, unit.y, 180f);
        Groups.bullet.updatePhysics();
        int count = names(unit).length;
        activate(unit, count - 2);
        require(unit.controller() == player && incoming.team == unit.team, "manual parry did not reflect hostile projectile");
        activate(unit, count - 1);
        require(player.unit() != unit && player.unit().type == unit.type, "duplication did not transfer player to new body");
        tick(player.unit(), 10);
        scenarios += 2;
    }

    private static void finisher() throws Exception{
        Unit unit = possess("despondency");
        require(!(boolean)call(unit, "usePlayerAbility") || (int)call(unit, "selectedPlayerAbility") == 0, "unexpected default action");
        Unit enemy = target(unit);
        // These are CPU-only descriptors for dimensions consumed by the finisher.
        // Reign dimensions come from v160.7 core/assets-raw/sprites/units/reign.png.
        TextureAtlas previousAtlas = Core.atlas;
        Core.atlas = new TextureAtlas(){
            @Override public TextureAtlas.AtlasRegion find(String name){
                require(name.equals("flameout-despondency-spear"), "unexpected headless texture request: " + name);
                byte[] png = mods.getMod("flameout").root.child("sprites").child("unit").child("weapons").child("despondency-special").child("despondency-spear.png").readBytes();
                TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion();
                region.width = java.nio.ByteBuffer.wrap(png).getInt(16);
                region.height = java.nio.ByteBuffer.wrap(png).getInt(20);
                return region;
            }
        };
        enemy.type.region = new TextureRegion();
        enemy.type.region.width = 214;
        enemy.type.region.height = 140;
        activate(unit, 1);
        try{ tick(unit, 1400); }finally{ Core.atlas = previousAtlas; }
        require(!enemy.isAdded() || enemy.dead || enemy.health <= 0f, "full finisher did not affect its main target");
        call(unit, "cancelPlayerAbility");
        require(!(boolean)field(unit, "playerFinisher"), "finisher cancellation failed");
        scenarios++;
    }

    private static void require(boolean condition, String message){ if(!condition) throw new AssertionError(message); }
}
