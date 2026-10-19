package flame;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

import static mindustry.Vars.*;

/** Runs real v160.7 content/entity code in a private headless application. */
public class CompatibilityTest{
    private static int checks;
    private static ClassLoader modLoader;

    public static void main(String[] args) throws Exception{
        verifyArchive();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new HeadlessApplication(new ApplicationListener(){
            @Override
            public void init(){
                try{
                    initialize();
                    Core.app.post(() -> {
                        try{
                            verifyUnits();
                            verifyFactory();
                            verifyNoEncounter();
                            verifyPlayerAbilities();
                            Log.info("PASS: @ compatibility checks on Mindustry @", checks, Version.buildString());
                        }catch(Throwable error){ failure.set(error); }
                        finally{ done.countDown(); }
                    });
                }catch(Throwable error){
                    failure.set(error);
                    done.countDown();
                }
            }
        }, error -> {
            failure.set(error);
            done.countDown();
        });
        if(!done.await(45, TimeUnit.SECONDS)) throw new AssertionError("Headless verification timed out");
        if(failure.get() != null){
            failure.get().printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void verifyArchive() throws Exception{
        try(JarFile jar = new JarFile(System.getProperty("mod.jar"))){
            for(var entry : java.util.Collections.list(jar.entries())){
                String name = entry.getName();
                require(!name.startsWith("mindustry/") && !name.startsWith("arc/"), "game dependency bundled: " + name);
                require(!name.startsWith("flame/special/") && !name.startsWith("extras/") && !name.contains("EmpathySpawner"), "horror payload bundled: " + name);
                require(!name.contains("BloodSplatter") && !name.contains("screams-of-the-damned") &&
                    !name.contains("apathy-bleed") && !name.contains("apathy-death") &&
                    !name.contains("flameout-silence") && !name.contains("des-spear-cry") &&
                    !name.contains("explosion-organic"), "horror asset bundled: " + name);
            }
            require(jar.getEntry("mod.json") != null, "missing mod metadata");
        }
    }

    private static void initialize(){
        Core.settings.setDataDirectory(new Fi("data"));
        // A saved horror stage from the original mod must have no effect in this fork.
        Core.settings.put("flame-special", 5);
        Core.settings.getDataDirectory().child("mods").mkdirs();
        new Fi(System.getProperty("mod.jar")).copyTo(Core.settings.getDataDirectory().child("mods/FlameOut_NoHorror.jar"));
        Core.camera = new Camera();
        Core.camera.resize(1024, 768);
        headless = true;
        loadLocales = false;
        net = new Net(null);
        Vars.init();
        content.createBaseContent();
        modLoader = mods.list().find(m -> m.name.equals("flameout")).main.getClass().getClassLoader();
        invoke("flame.FlameSounds", "load");
        content.createModContent();
        logic = new Logic();
        netServer = new NetServer();
        content.init();
        require(!mods.hasContentErrors(), "mod content errors");
        require(mods.list().contains(m -> m.name.equals("flameout") && m.enabled()), "mod was not enabled");
        require(Version.build == 160 && Version.revision == 7, "wrong game version");
        require(mods.list().find(m -> m.name.equals("flameout")).meta.minGameVersion.equals("160.7"), "packaged mod still has old compatibility metadata");
        invoke("flame.unit.empathy.EmpathyDamage", "initContent");
        Time.setDeltaProvider(() -> 1f);
        Time.delta = 1f;
        world.loadGenerator(128, 128, tiles -> {
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    tiles.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.air));
                }
            }
        });
        state.rules.unitCap = 1000;
        state.rules.infiniteResources = true;
        state.rules.waves = false;
        state.set(State.playing);
        player = Player.create();
        player.team(Team.sharded);
        player.add();
    }

    private static void verifyUnits(){
        UnitType[] types = {unitType("apathy"), unitType("apathy-sweep-0"), unitType("empathy"),
            unitType("yggdrasil"), unitType("despondency")};
        for(UnitType type : types){
            require(type != null && content.getByName(ContentType.unit, type.name) == type, "unit registration");
            require(!type.isHidden() && !type.internal && type.unlockedNow(), type.name + " is hidden or locked");
            require(type.playerControllable, type.name + " disallows control");
            Unit unit = type.create(Team.sharded);
            require(!type.name.endsWith("-empathy") || unit.getClass().getName().startsWith("flame.unit.empathy.EmpathyUnit"), "Empathy constructor is a decoy");
            unit.set(480f, 480f);
            unit.rotation = 0f;
            unit.add();
            require(unit.isAdded() && !unit.dead, type.name + " failed to spawn");
            // Exercise the game's real player handoff, rather than only checking a flag.
            player.unit(unit);
            require(unit.isPlayer() && unit.controller() == player && unit.playerControllable(), type.name + " handoff failed");
            for(int tick = 0; tick < 180; tick++){
                unit.moveAt(new Vec2(2f, 0f));
                unit.aim(900f, 480f);
                unit.controlWeapons(true, true);
                unit.update();
                Time.update();
                require(unit.isAdded() && !unit.dead && unit.controller() == player, type.name + " lost player control");
            }
            require(unit.x > 480.1f, type.name + " cannot move under player control");
            if(type.name.endsWith("-yggdrasil")){
                Seq<?> tentacles = Reflect.get(unit, "tentacles");
                require(tentacles.contains(t -> !(boolean)Reflect.invoke(t, "canShoot")), "Yggdrasil did not attack");
            }else{
                require(java.util.Arrays.stream(unit.mounts).anyMatch(m -> m.totalShots > 0), type.name + " did not fire");
            }
            float lastX = unit.x;
            player.unit(null);
            require(!unit.isPlayer(), type.name + " did not release player control");
            unit.update();
            require(Math.abs(unit.x - lastX) < 30f, type.name + " snapped back after release");
            player.unit(unit);
            unit.update();
            require(unit.controller() == player, type.name + " could not be controlled again");
            player.unit(null);
            Groups.clear();
            invoke("flame.unit.empathy.EmpathyDamage", "reset");
            player.add();
            Log.info("PASS: spawn, possess, move, fire, release, repossess @", type.localizedName);
        }
    }

    private static void verifyFactory(){
        var unitFactory = (mindustry.world.blocks.units.UnitFactory)content.block("flameout-unit-factory");
        require(unitFactory.plans.size == 5, "factory must offer all five units");
        require(unitFactory.buildVisibility == BuildVisibility.sandboxOnly, "factory must be sandbox-only");
        world.tile(32, 32).setBlock(unitFactory, Team.sharded);
        UnitFactoryBuild factory = (UnitFactoryBuild)world.tile(32, 32).build;
        for(int plan = 0; plan < 5; plan++){
            factory.currentPlan = plan;
            factory.progress = 0f;
            factory.payload = null;
            factory.efficiency = 1f;
            for(int tick = 0; tick < 65; tick++) factory.updateTile();
            require(factory.payload != null && factory.payload.unit.type == unitFactory.plans.get(plan).unit,
                "factory failed to produce plan " + plan);
            Unit produced = factory.payload.unit;
            produced.set(700f, 700f);
            require(factory.payload.dump(), "factory unit could not leave its payload: " + produced.type.name);
            require(produced.isAdded() && !produced.dead, "factory unit did not enter the world");
            player.unit(produced);
            produced.update();
            require(produced.controller() == player, "factory unit rejected player control");
            player.unit(null);
            Groups.clear();
            invoke("flame.unit.empathy.EmpathyDamage", "reset");
            player.add();
        }
    }

    private static void verifyNoEncounter(){
        require(!(boolean)Reflect.get(staticField("flame.effects.SpecialDeathEffects", "def"), "hasInternal"), "organic death effect");
        // Removing Apathy must not summon an unsolicited enemy or re-hide Empathy.
        Unit apathy = unitType("apathy").create(Team.sharded);
        apathy.set(600f, 600f);
        apathy.add();
        apathy.remove();
        for(int tick = 0; tick < 1200; tick++) invoke("flame.unit.empathy.EmpathyDamage", "update");
        require(!Groups.unit.contains(u -> u.type == unitType("empathy")), "unsolicited Empathy spawn");
        require(!unitType("empathy").hidden, "Empathy became hidden");
        require(Core.settings.getInt("flame-special") == 5, "legacy horror state was modified");
    }

    private static void verifyPlayerAbilities(){
        try{ PlayerAbilityTest.run(modLoader); }
        catch(Exception error){ throw new RuntimeException(error); }
    }

    private static UnitType unitType(String name){
        return content.unit("flameout-" + name);
    }

    private static Object staticField(String className, String field){
        try{
            return Class.forName(className, true, modLoader).getField(field).get(null);
        }catch(Exception error){
            throw new RuntimeException(error);
        }
    }

    private static void invoke(String className, String method){
        try{
            var target = Class.forName(className, true, modLoader).getDeclaredMethod(method);
            target.setAccessible(true);
            target.invoke(null);
        }catch(Exception error){
            throw new RuntimeException(error);
        }
    }

    private static void require(boolean condition, String message){
        checks++;
        if(!condition) throw new AssertionError(message);
    }
}
