package dev.pixelgba;

import android.test.InstrumentationTestCase;
import android.graphics.Bitmap;
import android.content.Context;
import java.io.*;
import java.nio.file.Files;

public class CoreTest extends InstrumentationTestCase {
    public void testRealEmulationCheatsStatesAndInput() throws Exception {
        Context context=getInstrumentation().getTargetContext();
        File rom=new File(context.getCacheDir(),"test.gba"),save=new File(context.getCacheDir(),"test.sav"),state=new File(context.getCacheDir(),"test.state");
        try(InputStream in=context.getAssets().open("pixel-lab.gba")){Storage.write(rom,Storage.readRom(in));}
        Bitmap bitmap=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888);short[] audio=new short[4096];
        try {
            assertTrue("open original homebrew",Core.open(rom.getPath(),save.getPath()));
            for(int i=0;i<120;i++)Core.frame(bitmap,audio,0,1);
            assertTrue("CPU advances frames",Core.read(0x02000000)>30);
            int beforeFast=Core.read(0x02000000);
            for(int i=0;i<10;i++)Core.frame(bitmap,audio,0,5);
            assertEquals("5x executes fifty frames, not capped at four",50,Core.read(0x02000000)-beforeFast);
            assertTrue("video is rendered",bitmap.getPixel(80,20)!=bitmap.getPixel(0,0));
            for(int i=0;i<4;i++)Core.frame(bitmap,audio,17,1);
            assertEquals("A + Right simultaneous input",17,Core.read(0x02000002));
            Core.frame(bitmap,audio,0,1);Core.frame(bitmap,audio,0,1);
            assertEquals("release input",0,Core.read(0x02000002));
            boolean audible=false;
            for(int i=0;i<8;i++){int samples=Core.frame(bitmap,audio,256,1);for(int j=0;j<samples;j++)if(audio[j]!=0)audible=true;}
            assertTrue("GBA sound produces PCM samples",audible);
            assertFalse("bad cheat is rejected",Core.cheat("invalid","XYZ",4));
            assertTrue("raw cheat accepted",Core.cheat("red marker","02000004:001F",4));
            for(int i=0;i<4;i++)Core.frame(bitmap,audio,0,1);
            assertEquals("cheat actually changes RAM",31,Core.read(0x02000004));
            Core.clearCheats();
            assertTrue("CodeBreaker accepted",Core.cheat("blue marker","82000004 7C00",1));
            for(int i=0;i<4;i++)Core.frame(bitmap,audio,0,1);
            assertEquals("CodeBreaker writes RAM",0x7c00,Core.read(0x02000004));
            Core.clearCheats();
            int frame=Core.read(0x02000000);
            assertTrue("save state",Core.state(state.getPath(),false));
            for(int i=0;i<15;i++)Core.frame(bitmap,audio,0,1);
            assertTrue(Core.read(0x02000000)>frame);
            assertTrue("restore state",Core.state(state.getPath(),true));
            assertEquals("CPU RAM restored",frame,Core.read(0x02000000));
            assertFalse("missing state does not crash",Core.state(state+".missing",true));
            assertTrue("battery flush",Core.flush(save.getPath()));
            Core.close();
            assertTrue("reopen cartridge",Core.open(rom.getPath(),save.getPath()));
            for(int i=0;i<120;i++)Core.frame(bitmap,audio,0,1);
            assertEquals("battery SRAM survives closing core",42,Core.read(0x02000006));
        }finally{Core.close();bitmap.recycle();rom.delete();save.delete();state.delete();}
    }
}
