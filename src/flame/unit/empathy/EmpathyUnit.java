package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import flame.unit.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class EmpathyUnit extends UnitEntity implements PlayerAbilityUnit{
    private int playerAbility;
    private boolean playerAttackActive, playerMovementActive;
    private boolean playerWasShooting;
    private String[] playerAbilityNamesCache;
    private String playerStatus = "Select an attack, movement skill, parry or duplication.";

    @Override public String[] playerAbilityNames(){
        if(playerAbilityNamesCache != null && playerAbilityNamesCache.length == 1 + attackAIs.size + movementAIs.size + 2) return playerAbilityNamesCache.clone();
        String[] names = new String[1 + attackAIs.size + movementAIs.size + 2];
        names[0] = "Primary pins";
        for(int i = 0; i < attackAIs.size; i++) names[i + 1] = "Attack: " + abilityName(attackAIs.get(i));
        for(int i = 0; i < movementAIs.size; i++) names[1 + attackAIs.size + i] = "Movement: " + abilityName(movementAIs.get(i));
        names[names.length - 2] = "Parry / reflect";
        names[names.length - 1] = "Duplicate / decoy";
        playerAbilityNamesCache = names;
        return names.clone();
    }

    private String abilityName(EmpathyAI ai){
        return ai.getClass().getSimpleName().replace("Attack", "").replace("Move", "").replaceAll("([a-z])([A-Z])", "$1 $2");
    }
    @Override public int selectedPlayerAbility(){ return playerAbility; }
    @Override public void selectPlayerAbility(int index){ if(index >= 0 && index < playerAbilityNames().length) playerAbility = index; }
    @Override public String playerAbilityStatus(){
        if(playerAttackActive && activeAttack instanceof CountDownAttack countdown) return "Count Down stage " + (countdown.count < 5 ? 5 - countdown.count : "finale") + ": casting. Use again after it finishes for the next stage.";
        return playerStatus;
    }
    @Override public boolean nativePlayerWeapon(){ return !playerAttackActive; }
    @Override public boolean usePlayerAbility(){
        if(!isPlayer() || !initialized) return false;
        String label = playerAbilityNames()[playerAbility];
        int attackIndex = playerAbility - 1;
        int movementIndex = attackIndex - attackAIs.size;
        if(playerAbility == 0){ playerStatus = "Hold primary fire for pins."; return true; }
        if(attackIndex < attackAIs.size){
            EmpathyAI attack = attackAIs.get(attackIndex);
            if(attack instanceof CopyAttack && attack.weight() <= 0f){
                playerStatus = "Copy requires an enemy unit with weapons under the cursor.";
                return false;
            }
            if(activeAttack != null){ activeAttack.end(); activeAttack.endOnce(); activeAttack.stopSounds(); }
            activeAttack = attack;
            activeAttack.quickSwap = false;
            activeAttack.reset();
            activeAttack.init();
            playerAttackActive = true;
        }else if(movementIndex < movementAIs.size){
            EmpathyAI movement = movementAIs.get(movementIndex);
            if(!playerAttackActive) activeAttack = attackAIs.first();
            if(movement instanceof RandomTeleport && !activeAttack.canTeleport()){
                playerStatus = "Finish or cancel the current cast before teleporting.";
                return false;
            }
            if(activeMovement != null){ activeMovement.end(); activeMovement.endOnce(); activeMovement.stopSounds(); }
            activeMovement = movementAIs.get(movementIndex);
            activeMovement.quickSwap = false;
            activeMovement.reset();
            activeMovement.init();
            if(activeMovement instanceof RandomTeleport teleport){
                teleport.teleportTime = teleport.delay = 0f;
                teleport.teleport = 0;
            }
            activeMovement.setTarget(getTarget());
            playerMovementActive = true;
            if(activeMovement instanceof TeleSwapMove teleport) teleport.onSwap();
        }else if(movementIndex == movementAIs.size){
            rotation = PlayerTargets.angle(this);
            parry();
        }else{
            duplicate();
        }
        playerStatus = label + " activated.";
        return true;
    }

    @Override public void cancelPlayerAbility(){
        playerAttackActive = playerMovementActive = playerWasShooting = false;
        moves.clear();
        rotates.clear();
        trueVel.setZero();
        for(EmpathyAI ai : attackAIs){ ai.end(); ai.endOnce(); ai.reset(); ai.stopSounds(); }
        for(EmpathyAI ai : movementAIs){ ai.end(); ai.endOnce(); ai.reset(); ai.stopSounds(); }
        playerStatus = "Abilities stopped. Normal movement and primary pins.";
    }

    private void updatePlayerAbilities(){
        // Native physics/input may already have moved us; sync the protected position store first.
        syncManualPosition();
        updateNearby();
        for(EmpathyAI ai : attackAIs){ ai.updateSounds(); ai.updatePassive(); }
        for(EmpathyAI ai : movementAIs) ai.updateSounds();
        if(playerAttackActive){
            if(!playerMovementActive || activeMovement.updateAttackAI()) activeAttack.update();
        }
        if(playerMovementActive) activeMovement.update();
        float inputVX = vel.x, inputVY = vel.y;
        applyQueuedMovement();
        updateTrueValues();
        vel.set(inputVX, inputVY);
        super.update();
        syncManualPosition();
        updateSpc();
        updateDecoyDelay();
        if(queueParry){ parry(); queueParry = false; }
        if(parryTime > 0f) parryTime -= Time.delta;
        if(autoParryTime > 0f) autoParryTime -= Time.delta;
        boolean pressed = isShooting && !playerWasShooting;
        playerWasShooting = isShooting;
        if(pressed && playerAbility > 0 && !playerAttackActive && !playerMovementActive) usePlayerAbility();
        if(!parryLasers.isEmpty()) parryLasers.removeAll(holder -> {
            holder.time -= Time.delta;
            return holder.time <= 0f || holder.b.id != holder.id;
        });
        updateTrail();
    }

    protected void transferPlayer(EmpathyUnit next){
        if(!isPlayer()) return;
        Player controlling = (Player)controller;
        next.playerAbility = playerAbility;
        controlling.unit(next);
    }

    Seq<EmpathyAI> movementAIs = new Seq<>(), attackAIs = new Seq<>();
    EmpathyAI activeMovement, activeAttack;
    int attackAIChanges = 0;

    boolean decoy = false;
    private float decoyDelay, decoyTime;

    private float trueHealth, trueMaxHealth;
    private Team trueTeam;

    private boolean queueParry = false;
    float parryHealth;
    float parryTime = 0f;
    private final Seq<LaserBulletHolder> parryLasers = new Seq<>(false);
    private float autoParryTime = 0f;

    float tx, ty, trot, trueDrag;
    float ltrot;
    UnitController trueController;
    float trailX, trailY;
    final Vec2 trueVel = new Vec2();
    private float invFrames;

    private float stunTimer = 0f;
    private float stunTimer2 = 0f;
    int stunCount = 0;
    boolean initialized;

    private float moveDistances = 0f;

    private float chainTime;
    private final Vec2 chainPos = new Vec2();

    private Teamc lastTarget;
    private float battleTime;

    private float damageTaken, maxDamageTaken, damageDelay;

    //priority, type(velocity, fixed), x, y
    //priority, rotation, speed
    private final FloatSeq moves = new FloatSeq(), rotates = new FloatSeq();

    protected int nearbyBullets = 0, nearbyUnits = 0, nearbyBuildings = 0;
    protected float nearestTarget = 99999f, nearestTotalHealth = 0f;
    float nearbyScan = 0f;
    private final static float scanRange = 700f;

    private static boolean parryHitSuccess = false;
    private static Bullet strb = null;
    private static float scr = 0f;
    private final static WeightedRandom<EmpathyAI> randAI = new WeightedRandom<>();
    private final static float chainLifetime = 17f;
    private final static Rect scanRect = new Rect();

    public static EmpathyUnit createUnit(){
        return createUnit(null);
    }

    static EmpathyUnit createUnit(EmpathyTransferData last){
        float[] d = new float[13];
        boolean[] d2 = new boolean[1];
        Team[] tm = new Team[1];
        final int ihealth = 0, imaxHealth = 1, ix = 2, iy = 3, ir = 4, idecoyDelay = 5, idamageTaken = 6, imaxDmgTaken = 7, idmgDelay = 8, istunTimer = 9, istunTimer2 = 10, istunCount = 11, iinvFrme = 12;

        if(last != null){
            System.arraycopy(last.d, 0, d, 0, d.length);
            //d2[0] = last.d2[0];
            d2[0] = last.dcy;
            tm[0] = last.t;
        }

        return new EmpathyUnit(){
            @Override
            void updateDamageTaken(float amount){
                d[idamageTaken] += amount;
                d[imaxDmgTaken] = Math.max(d[imaxDmgTaken], d[idamageTaken]);
                d[idmgDelay] = 60f;
                //if(decoyDelay <= 0f && (damageTaken >= 700000f || EmpathyDamage.isNaNInfinite(damageTaken, maxDamageTaken)))
                if(d[idecoyDelay] <= 0f && (d[idamageTaken] >= 700000f || EmpathyDamage.isNaNInfinite(d[idamageTaken], d[imaxDmgTaken]))){
                    duplicate();
                    d[imaxDmgTaken] = 0f;
                    d[idmgDelay] = 0f;
                }
            }

            @Override
            void updateDamageTaken(){
                if(d[idmgDelay] <= 0f){
                    if(d[idamageTaken] > 0 && d[imaxDmgTaken] > 0){
                        d[idamageTaken] = Math.max(0f, d[idamageTaken] - (d[imaxDmgTaken] / (5f * 60f)));
                        if(d[idamageTaken] <= 0f){
                            d[imaxDmgTaken] = 0f;
                        }
                    }
                }else{
                    d[idmgDelay] -= Time.delta;
                }
            }

            @Override
            float getTrueHealth(){
                return d[ihealth];
            }

            @Override
            public void impulse(float x1, float y1){
                float sc = d[istunCount];
                if(sc > 5 || (!activeAttack.canKnockback() || !activeMovement.canKnockback())) return;

                x1 /= (sc + 1f);
                y1 /= (sc + 1f);

                super.impulse(x1, y1);

                //speed * (1f - Mathf.pow(1f - drag, lifetime)
                float range = vel.len() * (1f - Mathf.pow(1f - drag, 120f)) / drag;
                if(range > 100f && d[istunTimer2] <= 0f){
                    d[istunTimer2] = 5f * 60f * Mathf.pow(sc + 1, 1.5f);
                    d[istunCount]++;
                    d[istunTimer] = 120f;
                }
            }

            @Override
            protected void syncManualPosition(){
                super.syncManualPosition();
                d[ix] = x;
                d[iy] = y;
                d[ir] = rotation;
            }

            @Override
            protected void updateTrueValues(){
                if(d[istunTimer] <= 0f){
                    d[ix] += trueVel.x * Time.delta;
                    d[iy] += trueVel.y * Time.delta;
                }
                trueVel.scl(Math.max(0f, 1f - trueDrag * Time.delta));

                statuses.clear();

                float hd = d[ihealth] - health;
                if(hd > 0){
                    updateDamageTaken(hd);
                }

                health = d[ihealth];
                maxHealth = d[imaxHealth];
                team = tm[0];
                hitSize = 10f;
                controller = trueController;

                float fbounds = Vars.finalWorldBounds / 1.5f;

                Rect r2 = Tmp.r2.set(-fbounds, -fbounds, Vars.world.width() * Vars.tilesize + fbounds * 2, Vars.world.height() * Vars.tilesize + fbounds * 2);

                d[ix] = Mathf.clamp(d[ix], r2.x, r2.x + r2.width);
                d[iy] = Mathf.clamp(d[iy], r2.y, r2.y + r2.height);

                if(d[ihealth] > 0){
                    dead = false;
                    elevation = 1;
                }
                if(d[istunTimer] <= 0f){
                    x = d[ix];
                    y = d[iy];
                    rotation = d[ir];
                    drag = trueDrag;
                    vel.setZero();
                }else{
                    d[istunTimer] -= Time.delta;
                    if(d[istunTimer] <= 0f){
                        x = d[ix];
                        y = d[iy];
                        rotation = d[ir];
                        drag = 0f;
                        vel.setZero();
                    }
                }
            }

            @Override
            void updateSpc(){
                //if(invFrames > 0) invFrames -= Time.delta;
                //stunTimer2 = Math.max(0f, stunTimer2 - Time.delta);
                //d[iinvFrme] = Math.max(d[iinvFrme], invFrames);

                if(d[iinvFrme] > 0){
                    d[iinvFrme] -= Time.delta;
                }
                d[istunTimer2] = Math.max(0f, d[istunTimer2] - Time.delta);
            }

            @Override
            void updateDecoyDelay(){
                //super.updateDecoyDelay();
                //if(decoyDelay > 0) decoyDelay -= Time.delta;
                if(d[idecoyDelay] > 0) d[idecoyDelay] -= Time.delta;
            }

            @Override
            public void heal(float amount){
                if(EmpathyDamage.isNaNInfinite(amount)) amount = 0f;
                amount = Math.max(amount, 0f);
                super.heal(amount);
                d[ihealth] += amount;
            }

            @Override
            void parryS(){
                d[ihealth] = Math.max(d[ihealth], parryHealth);
                d[iinvFrme] = Math.max(d[iinvFrme], 6f);
            }

            @Override
            void trueDamage(float amount){
                if(!isDecoy()){
                    updateDamageTaken(Math.max(0f, amount));
                }
                if(EmpathyDamage.isNaNInfinite(amount)) amount = 0f;
                if(!isDecoy()){
                    if(d[iinvFrme] <= 0f){
                        //float cdamage = Mathf.clamp(amount, 0f, 100f / 900f);
                        float cdamage = Mathf.clamp(amount, 0f, 100f / 500f);
                        if(parryTime <= 0f){
                            parryHealth = d[ihealth];
                            parryTime = 6f;
                        }
                        d[ihealth] -= cdamage;
                        health -= cdamage;
                        d[iinvFrme] = 30f;
                        hitTime = 1.0f;
                    }
                }else{
                    if(d[iinvFrme] <= 0f){
                        //super.damage(amount);
                        superDamage(amount);
                        d[iinvFrme] = 3f;
                    }
                }
            }

            @Override
            protected void moveFixed(float mx, float my){
                //super.moveFixed(mx, my);
                x += mx;
                y += my;
                d[ix] += mx;
                d[iy] += my;
            }
            @Override
            protected void moveFixedSlow(float mx, float my){
                trueVel.x *= 0.25f;
                trueVel.y *= 0.25f;
                x += mx;
                y += my;
                d[ix] += mx;
                d[iy] += my;
            }

            @Override
            protected void trueRotateTo(float angle, float speed){
                d[ir] = rotation = Angles.moveToward(d[ir], angle, speed);
            }

            @Override
            boolean isDecoy(){
                return d2[0];
            }

            @Override
            public void add(){
                if(!added && !initialized){
                    d[ihealth] = health;
                    d[imaxHealth] = maxHealth;
                    tm[0] = team;

                    trailX = d[ix] = x;
                    trailY = d[iy] = y;
                    ltrot = d[ir] = rotation;
                    trueDrag = drag;
                    trueController = controller;

                    initAIs();

                    randAI(true, false);
                    randAI(false, false);

                    initialized = true;

                    EmpathyDamage.addEmpathy(this);
                }
                super.add();
                //super.super.add
            }

            @Override
            public void remove(){
                if(d[ihealth] <= 0f || d2[0]){
                    if(!d2[0]){
                        EmpathyDamage.removeEmpathy(this);
                    }
                    for(EmpathyAI ai : attackAIs){
                        ai.stopSounds();
                    }
                    for(EmpathyAI ai : movementAIs){
                        ai.stopSounds();
                    }
                    super.remove();
                }
            }

            @Override
            EmpathyUnit duplicate(){
                EmpathyTransferData o = new EmpathyTransferData();

                d[idecoyDelay] = 2f * 60;

                o.d = d;
                o.dcy = d2[0];
                o.t = tm[0];

                EmpathyUnit u1 = createUnit(o);
                u1.setType(type);
                //u1.team = tm[0];
                u1.elevation = 1f;
                u1.copyFields(this);
                u1.team = tm[0];

                d2[0] = true;
                health = maxHealth = 100000000f;
                attackAIs.clear();
                movementAIs.clear();
                initDecoyAIs();

                activeAttack = activeMovement = null;
                randAI(true, false);
                randAI(false, false);

                u1.add();
                EmpathyDamage.onDuplicate(this, u1);
                transferPlayer(u1);

                return u1;
            }
        };
    }
    private final static class EmpathyTransferData{
        float[] d;
        boolean dcy;
        Team t;
    }

    @Override
    public boolean isBoss(){
        return true;
    }

    float getTrueHealth(){
        return trueHealth;
    }

    @Override
    public void update(){
        if(isPlayer()){
            updatePlayerAbilities();
            return;
        }
        float lastDelta = Time.delta;
        FlameOutSFX.realDelta = Time.delta;
        Time.delta = FlameOutSFX.timeDelta;

        for(EmpathyAI ai : attackAIs){
            ai.updateSounds();
        }
        for(EmpathyAI ai : movementAIs){
            ai.updateSounds();
        }

        applyQueuedMovement();

        if(!isDecoy()){
            updateTrueValues();
        }else{
            updateBoundLimit();
        }

        super.update();

        if(!isDecoy()) Vars.state.teams.bosses.add(this);

        updateNearby();
        updateDamageTaken();
        updateDecoyDelay();

        for(EmpathyAI ai : movementAIs){
            ai.updatePassive();
        }
        for(EmpathyAI ai : attackAIs){
            ai.updatePassive();
        }
        if(chainTime > 0){
            chainTime = Math.max(chainTime - Time.delta, 0f);
            if(chainTime <= 0f && getTarget() instanceof Unit u){
                Vec2 pos = Tmp.v1.set(chainPos).sub(x, y).setLength(25f + u.hitSize / 2f).add(x, y);
                EmpathyDamage.nanLock(u, pos.x, pos.y);
                chainTime = -240f;
            }
        }

        //boolean chained = Core.input.keyTap(KeyCode.x);
        if(getTarget() instanceof Unit u && chainTime <= 0f && !isDecoy()){
            //if(chained){
            Vars.world.getQuadBounds(Tmp.r1);

            if(chainTime > -1){
                boolean nan;
                if((nan = EmpathyDamage.isNaNInfinite(u.x, u.y, u.rotation)) || !Tmp.r1.contains(u.x, u.y)){
                    chainTime = chainLifetime;
                    if(nan){
                        chainPos.rnd(20000f).add(x, y);
                    }else{
                        chainPos.set(u.x, u.y).sub(x, y).setLength(20000f).add(x, y);
                    }
                }
            }else{
                chainTime += Time.delta;
                if(chainTime > 0){
                    chainTime = 0;
                }
            }
        }

        if(stunTimer <= 0f){
            if(activeAttack.updateMovementAI() || !activeMovement.updateAttackAI()) activeMovement.update();
            if(activeMovement.updateAttackAI() && chainTime <= 0f) activeAttack.update();
        }
        
        if(queueParry){
            parry();
            queueParry = false;
        }

        if(parryTime > 0) parryTime -= Time.delta;
        if(autoParryTime > 0) autoParryTime -= Time.delta;
        updateSpc();
        //stunTimer = Math.max(0f, stunTimer - Time.delta);
        if(!parryLasers.isEmpty()){
            parryLasers.removeAll(h -> {
                h.time -= Time.delta;
                return h.time <= 0f || h.b.id != h.id;
            });
        }

        updateTrail();

        Time.delta = lastDelta;
        
        if(!isDecoy() && getTrueHealth() <= 0f && added){
            remove();
        }
    }

    private void applyQueuedMovement(){
        if(!moves.isEmpty()){
            float max = -1f;
            int idx = -1;
            int idx2 = -1;
            float[] ar = moves.items;
            for(int i = 0; i < moves.size; i += 4){
                if(ar[i] > max){
                    max = ar[i];
                    idx2 = idx;
                    idx = i;
                }
            }
            if(idx != -1){
                float mx = ar[idx + 2];
                float my = ar[idx + 3];

                /*
                if(ar[idx + 1] == 0){
                    moveVelocity(mx, my);
                }else{
                    moveFixed(mx, my);
                }
                 */
                if(!Float.isNaN(mx) && !Float.isNaN(my)){
                    switch((int)ar[idx + 1]){
                        case 0 -> moveVelocity(mx, my);
                        case 1 -> moveFixed(mx, my);
                        case 2 -> moveFixedSlow(mx, my);
                        case 3 -> {
                            if(idx2 != -1){
                                float mx2 = ar[idx2 + 2];
                                float my2 = ar[idx2 + 3];

                                switch((int)ar[idx2 + 1]){
                                    case 0 -> moveVelocity(mx2 * mx, mx2 * my);
                                    case 1 -> moveFixed(mx2 * mx, my2 * my);
                                    case 2 -> moveFixedSlow(mx2 * mx, my2 * my);
                                }
                            }
                        }
                        case 4 -> {
                            moveFixed(mx, my);
                            trailX = x;
                            trailY = y;
                        }
                    }
                }
            }
            moves.clear();
        }
        if(!rotates.isEmpty()){
            float max = -1f;
            int idx = -1;
            float[] ar = rotates.items;
            for(int i = 0; i < rotates.size; i += 3){
                if(ar[i] > max){
                    max = ar[i];
                    idx = i;
                }
            }
            if(idx != -1){
                float angle = ar[idx + 1];
                float speed = ar[idx + 2];

                if(angle == angle){
                    if(speed >= 360f) ltrot = angle;

                    trueRotateTo(angle, speed);
                }
            }
            rotates.clear();
        }

    }

    void updateDecoyDelay(){
        if(decoyDelay > 0) decoyDelay -= Time.delta;
    }
    void updateSpc(){
        if(invFrames > 0) invFrames -= Time.delta;
        stunTimer2 = Math.max(0f, stunTimer2 - Time.delta);
    }

    void updateDamageTaken(float amount){
        //private float damageTaken, maxDamageTaken;
        damageTaken += amount;
        maxDamageTaken = Math.max(maxDamageTaken, damageTaken);
        damageDelay = 60f;

        //1000000f
        if(decoyDelay <= 0f && (damageTaken >= 700000f || EmpathyDamage.isNaNInfinite(damageTaken, maxDamageTaken))){
            duplicate();
            damageTaken = 0f;
            maxDamageTaken = 0f;
        }
    }
    void updateDamageTaken(){
        if(damageDelay <= 0f){
            if(damageTaken > 0 && maxDamageTaken > 0){
                damageTaken = Math.max(0f, damageTaken - (maxDamageTaken / (5f * 60f)));
                if(damageTaken <= 0f){
                    maxDamageTaken = 0f;
                }
            }
        }else{
            damageDelay -= Time.delta;
        }
    }
    int getCountDown(){
        for(EmpathyAI ai : attackAIs){
            if(ai instanceof CountDownAttack cd){
                return cd.count;
            }
        }
        return 0;
    }
    void setCountDown(int c){
        for(EmpathyAI ai : attackAIs){
            if(ai instanceof CountDownAttack cd){
                cd.count = c;
            }
        }
    }

    Teamc getTarget(){
        if(isPlayer()) return PlayerTargets.enemy(this);
        if(activeMovement == null) return null;
        return activeMovement.getTarget();
    }
    void retarget(){
        activeMovement.retarget();
    }

    private void updateNearby(){
        if(getTarget() != lastTarget){
            lastTarget = getTarget();
            battleTime = 0f;
        }else if(lastTarget != null){
            battleTime += Time.delta;
        }

        if(nearbyScan <= 0f){
            nearbyScan = 4;

            nearbyBullets = nearbyUnits = nearbyBuildings = 0;
            nearestTotalHealth = 0f;
            nearestTarget = 9999999f;
            scanRect.setCentered(x, y, scanRange * 2);
            Groups.bullet.intersect(scanRect.x, scanRect.y, scanRect.width, scanRect.height, b -> {
                if(b.team != team && within(b, scanRange)){
                    nearbyBullets++;
                }
            });
            for(TeamData data : Vars.state.teams.present){
                if(data.team != team){
                    if(data.unitTree != null){
                        data.unitTree.intersect(scanRect, u -> {
                            if(within(u, scanRange + u.hitSize / 2)){
                                if(u instanceof TimedKillc || u.controller() instanceof MissileAI){
                                    nearbyBullets++;
                                }else{
                                    nearbyUnits++;
                                    nearestTarget = Math.min(nearestTarget, dst(u));
                                    nearestTotalHealth += u.health;
                                }
                            }
                        });
                    }
                    if(data.buildingTree != null){
                        data.buildingTree.intersect(scanRect, b -> {
                            if(within(b, scanRange + b.block.size * Vars.tilesize / 2f)){
                                nearbyBuildings++;
                                nearestTarget = Math.min(nearestTarget, dst(b));
                                nearestTotalHealth += b.health;
                            }
                        });
                    }
                }
            }
            if(EmpathyDamage.isNaNInfinite(nearestTotalHealth)){
                nearestTotalHealth = Float.MAX_VALUE;
            }
        }
        nearbyScan -= Time.delta;
    }
    @Override
    public void controller(UnitController next){
        if(controller != next && (controller instanceof Player || next instanceof Player)) cancelPlayerAbility();
        super.controller(next);
        trueController = controller;
    }

    protected void syncManualPosition(){
        tx = x;
        ty = y;
        trot = rotation;
    }

    protected void updateTrueValues(){
        if(stunTimer <= 0f){
            tx += trueVel.x * Time.delta;
            ty += trueVel.y * Time.delta;
        }
        trueVel.scl(Math.max(0f, 1f - trueDrag * Time.delta));

        statuses.clear();

        float hd = trueHealth - health;
        if(hd > 0){
            updateDamageTaken(hd);
        }

        health = trueHealth;
        maxHealth = trueMaxHealth;
        team = trueTeam;
        hitSize = 10f;
        controller = trueController;
        
        if(trueHealth > 0){
            dead = false;
            elevation = 1;
        }
        if(stunTimer <= 0f){
            x = tx;
            y = ty;
            rotation = trot;
            drag = trueDrag;
            vel.setZero();
        }else{
            stunTimer -= Time.delta;
            if(stunTimer <= 0f){
                x = tx;
                y = ty;
                rotation = trot;
                drag = 0f;
                vel.setZero();
            }
        }
    }
    private void updateBoundLimit(){
        /*
        float fbounds = Vars.finalWorldBounds / 1.5f;

        Rect r2 = Tmp.r2.set(-fbounds, -fbounds, Vars.world.width() * Vars.tilesize + fbounds * 2, Vars.world.height() * Vars.tilesize + fbounds * 2);
        
        x = Mathf.clamp(x, r2.x, r2.x + r2.width);
        y = Mathf.clamp(y, r2.y, r2.y + r2.height);
        tx = x;
        ty = y;
        */

        if(decoyTime < 7f * 60){
            decoyTime += Time.delta;
            if(decoyTime >= 5f * 60){
                maxHealth = 20000f;
                health = Math.min(health, maxHealth);
            }
        }
    }

    void updateTrail(){
        if(Float.isNaN(trailX) || Float.isNaN(trailY)){
            trailX = x;
            trailY = y;
        }
        float dst = Mathf.dst(trailX, trailY, x, y);
        float m = 0f;
        if(dst > 0.001f) moveDistances += Math.max(dst, 16f / 5f);
        Vec2 v = Tmp.v1.set(x, y).sub(trailX, trailY).limit(16f);
        while(moveDistances > 0){
            //Effect
            float ang = Mathf.slerp(ltrot, rotation, m / dst);
            if(activeAttack.canTrail() && activeMovement.canTrail()) FlameFX.empathyTrail.at(trailX, trailY, ang, Tmp.c1.set(m / 16f, 0f, 0f));

            moveDistances -= 16f;
            trailX += v.x;
            trailY += v.y;
            m += 16f;
        }

        ltrot = rotation;
    }

    /*
    @Override
    public void impulse(float x, float y){
        if(stunCount > 5 || (!activeAttack.canKnockback() || !activeMovement.canKnockback())) return;

        x /= (stunCount + 1f);
        y /= (stunCount + 1f);

        super.impulse(x, y);

        //speed * (1f - Mathf.pow(1f - drag, lifetime)
        float range = vel.len() * (1f - Mathf.pow(1f - drag, 120f)) / drag;
        if(range > 100f && stunTimer2 <= 0f){
            stunTimer2 = 5f * 60f * Mathf.pow(stunCount + 1, 1.5f);
            stunCount++;
            stunTimer = 120f;
        }
    }
    */

    protected void moveVelocity(float vx, float vy){
        trueVel.add(vx, vy);
    }
    protected void moveFixed(float mx, float my){
        //float d = Time.delta;
        x += mx;
        y += my;
        tx += mx;
        ty += my;
    }
    protected void moveFixedSlow(float mx, float my){
        trueVel.x *= 0.25f;
        trueVel.y *= 0.25f;

        x += mx;
        y += my;
        tx += mx;
        ty += my;
    }
    protected void trueRotateTo(float angle, float speed){
        trot = rotation = Angles.moveToward(trot, angle, speed);
    }

    void move(float priority, int type, float mx, float my){
        moves.add(priority, type, mx, my);
    }
    void rotate(float priority, float ang, float speed){
        rotates.add(priority, ang, speed);
    }

    @Override
    public void collision(Hitboxc other, float x, float y){
        super.collision(other, x, y);
        if(other instanceof Bullet b && (b.type instanceof LaserBulletType || b.type instanceof RailBulletType || b.type instanceof ShrapnelBulletType || (!within(b, 180f) && !(b.type instanceof ContinuousBulletType)))){
            LaserBulletHolder h = new LaserBulletHolder();
            h.id = b.id;
            h.b = b;
            h.x = b.x;
            h.y = b.y;
            h.rotation = b.rotation();
            h.type = b.type;
            h.data = b.data;
            h.time = 3f;
            h.overrideFdata = b.type instanceof LaserBulletType || b.type instanceof RailBulletType || b.type instanceof ShrapnelBulletType;

            parryLasers.add(h);

            if(autoParryTime <= 0f){
                //parry();
                queueParry();
            }
        }
    }

    @Override
    public void draw(){
        if(isPlayer()){
            boolean hidden = (playerAttackActive && activeAttack.overrideDraw()) || (playerMovementActive && activeMovement.overrideDraw());
            if(!hidden) super.draw();
            if(playerMovementActive && activeMovement.shouldDraw()) activeMovement.draw();
            for(EmpathyAI ai : attackAIs){
                if(ai.shouldDraw() && (playerAttackActive || ai != activeAttack || ai instanceof CopyAttack || ai instanceof SwordBarrageAttack)) ai.draw();
            }
            return;
        }
        boolean drawUnit = true;

        for(EmpathyAI ai : attackAIs){
            if(ai.shouldDraw() && ai.overrideDraw()) drawUnit = false;
        }
        for(EmpathyAI ai : movementAIs){
            if(ai.shouldDraw() && ai.overrideDraw()) drawUnit = false;
        }

        if(!isDecoy()){
            if(drawUnit) super.draw();
        }else{
            Draw.z(Layer.flyingUnitLow);
            Draw.color();
            Draw.rect(EmpathyRegions.decoy, x, y, rotation - 90f);
        }
        Draw.z(Layer.flyingUnit);
        for(EmpathyAI ai : movementAIs){
            if(ai.shouldDraw()) ai.draw();
        }
        if(chainTime > 0){
            float fin = 1 - chainTime / chainLifetime;
            Vec2 v = Tmp.v1.set(chainPos).sub(x, y).scl(1f - ((40f / 20000) * Mathf.pow(fin, 15f))).add(x, y);
            Tmp.c1.set(Color.white).lerp(FlamePal.empathyAdd, Mathf.curve(fin, 0f, 0.5f));
            GraphicUtils.chain(v.x, v.y, x, y, Tmp.c1, Blending.additive);
        }
        for(EmpathyAI ai : attackAIs){
            if(ai.shouldDraw()) ai.draw();
        }
    }

    @Override
    public float clipSize(){
        return Float.MAX_VALUE;
    }

    void queueParry(){
        queueParry = true;
    }

    void parry(){
        parry(true);
    }

    @SuppressWarnings("unchecked")
    void parry(boolean effect){
        Teamc t = getTarget();

        parryHitSuccess = false;

        strb = null;
        scr = 0f;
        LaserBulletHolder strP = null;

        Utils.scanCone((QuadTree<Bullet>)Groups.bullet.tree(), x, y, rotation, 35f, 80f, b -> {
            if(b.team != team){
                float sr = FlameOutSFX.inst.getBulletDps(b.type);
                if(sr > scr){
                    strb = b;
                }
                b.rotation(angleTo(b));
                float dx = ((b.x - x) / 20f) * 2.25f;
                float dy = ((b.y - y) / 20f) * 2.25f;
                b.vel.add(dx, dy);
                b.team = team;

                if(b.vel.len() > 7f){
                    ParriedEntity.create(b, team);
                }

                parryHitSuccess = true;
            }
        });
        Utils.scanCone((QuadTree<Unit>)Groups.unit.tree(), x, y, rotation, 35f, 80f, u -> {
            if(u.team != team && (u.controller() instanceof MissileAI || u instanceof TimedKillUnit)){
                if(u.controller() instanceof MissileAI ai){
                    ai.shooter = null;
                }
                float ang = angleTo(u);
                u.rotation = ang;
                u.vel.setAngle(ang);

                float dx = ((u.x - x) / 20f) * 2.25f;
                float dy = ((u.y - y) / 20f) * 2.25f;
                u.vel.add(dx, dy);
                u.team = team;

                if(u.vel.len() > 7f){
                    ParriedEntity.create(u, team);
                }

                parryHitSuccess = true;
            }
        });

        for(LaserBulletHolder pr : parryLasers){
            if(FlameOutSFX.inst.getBulletDps(pr.type) > scr){
                //strType = pr.type;
                strP = pr;
                strb = null;
            }
            parryHitSuccess = true;
        }
        if(parryHitSuccess){
            if(effect){
                FlameFX.empathyParry.at(x, y, rotation);
                FlameSounds.empathyParry.at(x, y, Mathf.random(0.99f, 1.01f));
            }

            parryS();
            parryTime = 0f;
            autoParryTime = 35f;

            for(LaserBulletHolder pr : parryLasers){
                if(pr != strP || t == null){
                    Vec2 tv = Tmp.v1.set(pr.x, pr.y).sub(x, y).setLength(hitSize / 2f).add(x, y);
                    if(tv.isNaN()){
                        tv.rnd(hitSize / 2f).add(x, y);
                    }

                    if(pr.b.id == pr.id && pr.overrideFdata){
                        Bullet b = pr.b;
                        b.fdata = Mathf.dst(b.x, b.y, tv.x, tv.y);
                    }
                    float ang = angleTo(tv);
                    pr.type.create(pr.b.id == pr.id ? pr.b.owner : this, team, tv.x, tv.y, ang, pr.type.damage, 1, 1, pr.data);
                }
            }

            if(t != null){
                if(strP != null){
                    Vec2 tv = Tmp.v1.set(strP.x, strP.y).sub(x, y).setLength(hitSize / 2f).add(x, y);
                    if(tv.isNaN()){
                        tv.rnd(hitSize / 2f).add(x, y);
                    }
                    if(strP.b.id == strP.id && strP.overrideFdata){
                        Bullet b = strP.b;
                        b.fdata = Mathf.dst(b.x, b.y, tv.x, tv.y);
                    }
                    float ang = tv.angleTo(t);
                    strP.type.create(strP.b.id == strP.id ? strP.b.owner : this, team, tv.x, tv.y, ang, strP.type.damage, 1, 1, strP.data);
                }else if(strb != null){
                    strb.rotation(strb.angleTo(t));
                }
            }
            parryLasers.clear();
        }
    }
    void parryS(){
        trueHealth = Math.max(trueHealth, parryHealth);
        invFrames = Math.max(invFrames, 6f);
    }
    
    void superDamage(float amount){
        super.damage(amount);
    }

    @Override
    public void damage(float amount){
        trueDamage(amount);
    }

    @Override
    public void damagePierce(float amount, boolean withEffect){
        float pre = hitTime;
        trueDamage(amount);
        if(!withEffect){
            hitTime = pre;
        }
    }

    /*
    @Override
    public void heal(float amount){
        if(EmpathyDamage.isNaNInfinite(amount)) amount = 0f;
        amount = Math.max(amount, 0f);
        super.heal(amount);
        trueHealth += amount;
    }
    */

    void trueDamage(float amount){
        if(!isDecoy()){
            updateDamageTaken(Math.max(0f, amount));
        }
        if(EmpathyDamage.isNaNInfinite(amount)) amount = 0f;
        if(!isDecoy()){
            if(invFrames <= 0f){
                //float cdamage = Mathf.clamp(amount, 0f, 100f / 900f);
                float cdamage = Mathf.clamp(amount, 0f, 100f / 500f);
                if(parryTime <= 0f){
                    parryHealth = trueHealth;
                    parryTime = 6f;
                }
                trueHealth -= cdamage;
                health -= cdamage;
                invFrames = 30f;
                hitTime = 1.0f;
            }
        }else{
            if(invFrames <= 0f){
                super.damage(amount);
                invFrames = 3f;
            }
        }
    }

    @Override
    public boolean serialize(){
        return false;
    }

    void initAIs(){
        //movementAIs.addAll(new BulletHellMove().set(this));
        movementAIs.addAll(
                new BulletHellMove().set(this),
                new OrbitMove().set(this),
                new RandomTeleport().set(this),
                new TeleSwapMove().set(this)
        );

        attackAIs.addAll(
                new PinAttack().set(this),
                new SprayAttack().set(this),
                new SwordBarrageAttack().set(this),
                new LaserShotgunAttack().set(this),
                new RicochetAttack().set(this),
                new ShineAttack().set(this),
                
                new MagicAttack().set(this),
                new DepowerAttack().set(this),

                new RendAttack().set(this),
                new PrimeAttack().set(this),
                new DashAttack().set(this),
                new CopyAttack().set(this),
                new BlackHoleAttack().set(this),
                new SwordAttack().set(this),
                new HandAttack().set(this),
                new BlastAttack().set(this),
                new CountDownAttack().set(this),
                new EndAttack().set(this),
                new SurroundLaserAttack().set(this)
        );
    }
    void initDecoyAIs(){
        movementAIs.addAll(
                new OrbitMove().set(this),
                new RandomTeleport().set(this)
                //new TeleSwapMove().set(this)
        );

        attackAIs.addAll(
                new PinAttack().set(this),
                new SprayAttack().set(this),
                new SwordBarrageAttack().set(this),
                new ShineAttack().set(this),
                new RicochetAttack().set(this),
                new LaserShotgunAttack().set(this),

                new MagicAttack().set(this)
        );
    }

    /*
    @Override
    public void add(){
        if(!added && !initialized){
            trueHealth = health;
            trueMaxHealth = maxHealth;
            trueTeam = team;

            trailX = tx = x;
            trailY = ty = y;
            ltrot = trot = rotation;
            trueDrag = drag;
            trueController = controller;

            initAIs();

            randAI(true, false);
            randAI(false, false);

            initialized = true;

            EmpathyDamage.addEmpathy(this);
        }
        super.add();
    }

    @Override
    public void remove(){
        if(trueHealth <= 0f || decoy){
            if(!decoy){
                EmpathyDamage.removeEmpathy(this);
            }
            for(EmpathyAI ai : attackAIs){
                ai.stopSounds();
            }
            for(EmpathyAI ai : movementAIs){
                ai.stopSounds();
            }
            super.remove();
        }
    }
    */

    @Override
    public void destroy(){
        if(isDecoy()){
            super.destroy();
        }
    }

    static Vec2 tmpVec = new Vec2();
    Vec2 trueVelocity(){
        return tmpVec.set(trueVel);
    }

    void copyFields(EmpathyUnit f){
        x = tx = f.tx;
        y = ty = f.ty;
        rotation = trot = f.trot;
        trueVel.set(f.trueVel);
        drag = trueDrag = f.trueDrag;
        health = trueHealth = f.trueHealth;
        maxHealth = trueMaxHealth = f.trueMaxHealth;
        team = trueTeam = f.trueTeam;
        trailX = f.trailX;
        trailY = f.trailY;
        trueController = controller;
        hitSize = 10;

        invFrames = f.invFrames;

        lastTarget = f.lastTarget;
        battleTime = f.battleTime;

        stunTimer = f.stunTimer;
        stunTimer2 = f.stunTimer2;
        stunCount = f.stunCount;

        damageTaken = f.damageTaken;
        maxDamageTaken = f.maxDamageTaken;
        damageDelay = f.damageDelay;

        nearbyBullets = f.nearbyBullets;
        nearbyUnits = f.nearbyUnits;
        nearbyBuildings = f.nearbyBuildings;
        nearestTarget = f.nearestTarget;
        nearestTotalHealth = f.nearestTotalHealth;
        nearbyScan = f.nearbyScan;

        moves.clear();
        rotates.clear();
        moves.addAll(f.moves);
        rotates.addAll(f.rotates);

        initialized = true;

        for(EmpathyAI ai : f.attackAIs){
            ai.set(this);
            attackAIs.add(ai);
        }
        for(EmpathyAI ai : f.movementAIs){
            ai.set(this);
            movementAIs.add(ai);
        }
        activeAttack = f.activeAttack;
        activeMovement = f.activeMovement;
    }

    EmpathyUnit duplicate(){
        EmpathyUnit u = new EmpathyUnit();
        u.team = team;
        u.setType(type);
        u.elevation = 1f;
        u.copyFields(this);
        u.decoyDelay = 2f * 60;

        decoy = true;
        health = maxHealth = 100000000f;
        attackAIs.clear();
        movementAIs.clear();
        initDecoyAIs();

        activeAttack = activeMovement = null;
        randAI(true, false);
        randAI(false, false);

        u.add();
        EmpathyDamage.onDuplicate(this, u);
        transferPlayer(u);

        return u;
    }

    boolean isDecoy(){
        return decoy;
    }

    void updateUsages(boolean attack){
        Seq<EmpathyAI> ais = attack ? attackAIs : movementAIs;
        int c = 0;
        for(EmpathyAI ai : ais){
            if(ai.weight() > 0) c++;
        }
        for(EmpathyAI ai : ais){
            //ai.aiUsages = Math.max(0f, ai.aiUsages - 1f / c);
            ai.aiUsages *= 1f / c;
        }
    }

    float getTargetHealthFract(){
        if(!(getTarget() instanceof Healthc h)) return 0f;
        float health = EmpathyDamage.getHealth(getTarget());
        if(Float.isNaN(health)){
            health = h.health() / h.maxHealth();
        }

        return health;
    }

    boolean targetLowHealth(){
        //activeMovement == null || 
        if(!(getTarget() instanceof Healthc h)) return false;
        float health = EmpathyDamage.getHealth(getTarget());
        if(Float.isNaN(health)){
            health = h.health() / h.maxHealth();
        }
        if(Float.isNaN(health)){
            return true;
        }

        return health <= 0.3f;
    }
    boolean useLethal(){
        return targetLowHealth() || battleTime > 4f * 60f * 60f || nearestTotalHealth >= 2000000 || (attackAIChanges % 7) >= 6;
    }
    float extraLethalScore(){
        return 10f / Math.max(0.000001f, getTargetHealthFract());
    }

    void randAI(boolean attack, boolean quickSwap){
        if(isPlayer() && !isDecoy()){
            if(attack) playerAttackActive = false; else playerMovementActive = false;
            playerStatus = "Pattern complete. Use again or select another ability.";
            return;
        }
        if(attack){
            randAI.clear();
            for(EmpathyAI ai : attackAIs){
                float use = 1f + (ai.aiUsages / 2f);
                //randAI.add(ai, Math.max(ai.weight() / use, 1f));
                randAI.add(ai, ai.weight() > 0 ? Math.max(ai.weight() / use, 0.5f) : -1f);
            }
            EmpathyAI i = randAI.get();

            if(i == null){
                EmpathyAI alt = null;
                float max = 0f;
                for(EmpathyAI ai : attackAIs){
                    if(alt == null || ai.weight() > max){
                        alt = ai;
                        max = ai.weight();
                    }
                }
                i = alt;
            }

            i.quickSwap = quickSwap;
            switchAI(i);
        }else{
            randAI.clear();
            for(EmpathyAI ai : movementAIs){
                //float use = 1f + (ai.aiUsages / 5f);
                randAI.add(ai, ai.weight());
            }
            EmpathyAI i = randAI.get();
            i.quickSwap = quickSwap;
            switchAI(i);
        }
    }

    void switchAI(EmpathyAI ai){
        if(!isPlayer() && ai.attack && activeMovement instanceof TeleSwapMove tp && !tp.swaping){
            if(tp.onSwap()) return;
        }

        if(isPlayer()){ if(ai.attack) playerAttackActive = true; else playerMovementActive = true; }
        if(ai.attack){
            updateUsages(true);
            attackAIChanges++;
            if(activeAttack != null){
                activeAttack.aiUsages += activeAttack.aiUsages / 5f + 1f;
                activeAttack.end();
            }
            if(ai != activeAttack){
                if(activeAttack != null){
                    //activeAttack.end();
                    activeAttack.quickSwap = false;
                    //updateUsages
                    activeAttack.endOnce();
                }
                activeAttack = ai;
                ai.init();
            }
        }else{
            updateUsages(false);
            if(activeMovement != null){
                activeMovement.aiUsages += activeMovement.aiUsages / 5f + 1f;
                activeMovement.end();
            }
            if(ai != activeMovement){
                if(activeMovement != null){
                    activeMovement.quickSwap = false;
                    activeMovement.endOnce();
                }

                Teamc target = activeMovement != null ? activeMovement.getTarget() : null;
                //if(activeMovement != null) activeMovement.end();
                activeMovement = ai;
                if(target != null) activeMovement.setTarget(target);
                ai.init();
            }
        }
    }

    static class LaserBulletHolder{
        float x, y, rotation;
        float time = 0f;
        int id;
        Bullet b;
        BulletType type;
        Object data;
        boolean overrideFdata = true;
    }
}
