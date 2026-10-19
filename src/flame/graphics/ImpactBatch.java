package flame.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;

public class ImpactBatch extends SpriteBatch{
    public float u, v, u2, v2;
    public float rx, ry;
    public float lastRotation;
    public boolean heavyShader = false;
    public boolean useColor = false;
    public boolean canChangeShader = true;
    boolean white = false;
    float[] svt = new float[1024 * SPRITE_SIZE];

    public static ImpactBatch batch;
    static Batch lastBatch;

    public static void init(){
        batch = new ImpactBatch();
    }
    public static void beginSwap(){
        lastBatch = Core.batch;
        Mat proj = Draw.proj(), trans = Draw.trans();
        //Draw.flush();
        Core.batch = batch;
        Draw.proj(proj);
        Draw.trans(trans);
    }
    public static void endSwap(){
        Draw.flush();
        Core.batch = lastBatch;
    }

    public void setWhite(boolean w){
        //if(white != w) flush();
        white = w;
    }

    public Texture getTexture(){
        return lastTexture;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        // drawSuper writes to Arc's FloatBuffer and handles texture/capacity flushes.
        if(svt.length < count) svt = new float[count];
        System.arraycopy(spriteVertices, offset, svt, 0, count);
        for(int i = 0; i < count; i += VERTEX_SIZE){
            svt[i + 2] = useColor ? spriteVertices[offset + i + 2] : Color.whiteFloatBits;
            svt[i + 5] = white ? Color.whiteFloatBits : Color.blackFloatBits;
        }
        flush();
        u = v = 0f;
        u2 = v2 = 1f;
        lastRotation = 0f;
        super.drawSuper(texture, svt, 0, count);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        // Shader uniforms belong to the previous sprite until that sprite is flushed.
        flush();
        u = region.u;
        v = region.v;
        u2 = region.u2;
        v2 = region.v2;
        lastRotation = rotation;
        float previousColor = colorPacked, previousMix = mixColorPacked;
        colorPacked = useColor ? previousColor : Color.whiteFloatBits;
        mixColorPacked = white ? Color.whiteFloatBits : Color.blackFloatBits;
        super.drawSuper(region, x, y, originX, originY, width, height, rotation);
        colorPacked = previousColor;
        mixColorPacked = previousMix;
    }

    @Override
    protected void setShader(Shader shader, boolean apply){
        if(!canChangeShader) return;
        super.setShader(shader, apply);
    }
}
