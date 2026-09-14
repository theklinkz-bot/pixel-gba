package dev.pixelgba;

import android.test.InstrumentationTestCase;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import java.io.*;

public class ReleaseFeaturesTest extends InstrumentationTestCase {
    public void testRestartManualRestoreAndUltimate() throws Exception {
        MainActivity a=(MainActivity)getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        File rom;
        try(InputStream in=a.getAssets().open("pixel-lab.gba")){rom=a.storeRom(in,"Pixel Lab");}
        getInstrumentation().runOnMainSync(()->{
            a.launch(rom);a.stop();a.manualScreenReset();
            try {
                for(int i=0;i<120;i++)Core.frame(a.bitmap,new short[4096],0,1);
                for(int i=0;i<4;i++)Core.frame(a.bitmap,new short[4096],1,1);
                a.saveState("slot1");byte[] slot=java.nio.file.Files.readAllBytes(a.saveFile("slot1").toPath());
                a.restartGame();
                for(int i=0;i<5;i++)Core.frame(a.bitmap,new short[4096],0,1);
                assertTrue("restart does not resume old state",Core.read(0x02000000)<30);
                for(int i=0;i<120;i++)Core.frame(a.bitmap,new short[4096],0,1);
                assertEquals("battery save survives restart",42,Core.read(0x02000006));
                assertTrue("manual save slot is unchanged",java.util.Arrays.equals(slot,java.nio.file.Files.readAllBytes(a.saveFile("slot1").toPath())));
                a.game.layout(0,0,1200,800);a.game.manualScale=3;a.game.panX=60;a.game.panY=-40;a.screenEditing=true;a.game.resizeGesture=2;
                MotionEvent up=MotionEvent.obtain(0,0,MotionEvent.ACTION_UP,600,400,0);a.game.onTouchEvent(up);up.recycle();a.screenEditing=false;
                MainActivity.GameView restored=a.new GameView(a);restored.layout(0,0,600,400);
                assertEquals(1.5f,restored.manualScale,.001f);assertEquals(30f,restored.panX,.001f);assertEquals(-20f,restored.panY,.001f);
                a.prefs.edit().putInt("filter",3).apply();Bitmap output=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888);restored.draw(new Canvas(output));
                assertEquals(1.5f,restored.screen.width()/restored.screen.height(),.001f);output.recycle();
                int[] pixels={0,1,0,1,0,0,0,0,0}, scaled=new int[36];UltimateFilter.upscale(pixels,3,3,scaled);
                assertEquals("matching corner is smoothed",1,scaled[14]);assertEquals("other corner keeps center",0,scaled[15]);
                UltimateFilter.upscale(new int[]{7},1,1,scaled);for(int i=0;i<4;i++)assertEquals("single pixel borders",7,scaled[i]);
                a.manualScreenReset();MainActivity.GameView reset=a.new GameView(a);reset.layout(0,0,600,400);assertEquals(0f,reset.manualScale);
            }catch(IOException e){throw new AssertionError(e);}
            finally{a.prefs.edit().putInt("filter",0).apply();a.stop();a.finish();}
        });
    }
}
