package flame.unit.empathy;

import arc.*;
import arc.graphics.g2d.*;
import flame.graphics.*;

public class EmpathyRegions{
    public static TextureRegion sword, swordSide, swordTrail, portal, magicCircle, magicCircle2, circle, hcircle, flash, decoy, hand;
    static TextureRegion[] countDown = new TextureRegion[6], endAPI = new TextureRegion[3];
    static SegmentedRegion swordSeg, swordSideSeg;

    public static void loadHeadless(){
        // EndAttack uses sprite widths for ring positions even without a renderer.
        // Read the actual PNG dimensions, never a substitute gameplay size.
        var mod = mindustry.Vars.mods.getMod("flameout");
        for(int i = 0; i < endAPI.length; i++){
            byte[] png = mod.root.child("sprites").child("end-api-" + i + ".png").readBytes();
            java.nio.ByteBuffer header = java.nio.ByteBuffer.wrap(png);
            TextureRegion region = new TextureRegion();
            region.width = header.getInt(16);
            region.height = header.getInt(20);
            endAPI[i] = region;
        }
    }

    public static void load(){
        sword = Core.atlas.find("flameout-sword");
        swordSide = Core.atlas.find("flameout-sword-side");
        swordSeg = new SegmentedRegion("flameout-sword");
        swordSideSeg = new SegmentedRegion("flameout-sword-side");
        swordTrail = Core.atlas.find("flameout-sword-trail");
        portal = Core.atlas.find("flameout-portal");
        magicCircle = Core.atlas.find("flameout-magic-circle");
        magicCircle2 = Core.atlas.find("flameout-magic-circle-2");
        circle = Core.atlas.find("circle");
        hcircle = Core.atlas.find("hcircle");
        flash = Core.atlas.find("flameout-flash");
        decoy = Core.atlas.find("flameout-empathy-decoy");
        hand = Core.atlas.find("flameout-hand");

        for(int i = 0; i < countDown.length; i++){
            countDown[i] = Core.atlas.find("flameout-count-down-" + i);
        }
        for(int i = 0; i < endAPI.length; i++){
            endAPI[i] = Core.atlas.find("flameout-end-api-" + i);
        }
    }
}
