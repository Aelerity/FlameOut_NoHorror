package flame.blocks;

import arc.struct.*;
import flame.unit.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

/** An ordinary sandbox factory gives all five units a visible, explicit spawn path. */
public class FlameBlocks{
    public static UnitFactory unitFactory;

    public static void load(){
        unitFactory = new UnitFactory("unit-factory"){{
            localizedName = "Flame Out Unit Factory";
            description = "Select any Flame Out unit. Take control of the friendly unit using the game's unit control key.";
            requirements(Category.units, BuildVisibility.sandboxOnly, ItemStack.empty);
            alwaysUnlocked = true;
            size = 3;
            hasPower = false;
            regionSuffix = "-dark";
            plans = Seq.with(
                new UnitPlan(FlameUnitTypes.apathy, 60f, ItemStack.empty),
                new UnitPlan(FlameUnitTypes.apathySentry, 60f, ItemStack.empty),
                new UnitPlan(FlameUnitTypes.empathy, 60f, ItemStack.empty),
                new UnitPlan(FlameUnitTypes.yggdrasil, 60f, ItemStack.empty),
                new UnitPlan(FlameUnitTypes.despondency, 60f, ItemStack.empty)
            );
        }

            @Override
            public void load(){
                super.load();
                // Reuse built-in art instead of shipping an unrelated factory sprite.
                region = Blocks.airFactory.region;
                topRegion = ((UnitFactory)Blocks.airFactory).topRegion;
                outRegion = ((UnitFactory)Blocks.airFactory).outRegion;
                fullIcon = uiIcon = Blocks.airFactory.uiIcon;
            }
        };
    }
}
