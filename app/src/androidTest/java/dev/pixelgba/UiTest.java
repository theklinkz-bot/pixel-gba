package dev.pixelgba;

import android.test.InstrumentationTestCase;
import android.content.Intent;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;

public class UiTest extends InstrumentationTestCase {
    public void testTouchLayoutPersistenceGraphicsAndLifecycle() throws Exception {
        MainActivity a=(MainActivity)getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        File rom;
        try(InputStream in=a.getAssets().open("pixel-lab.gba")){rom=a.storeRom(in,"Pixel Lab • Homebrew");}
        getInstrumentation().runOnMainSync(()->a.launch(rom));
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(()->{
            a.stop();for(int frame=0;frame<120;frame++)Core.frame(a.bitmap,new short[4096],0,1);a.prefs.edit().remove("layout.portrait").remove("layout.landscape").apply();a.game.layoutKey="";a.game.layout();
            for(int level=1;level<=5;level++){
                RectF chip=a.game.toolbarBounds(10+level);
                MotionEvent tap=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,chip.centerX(),chip.centerY(),0);a.game.onTouchEvent(tap);tap.recycle();
                assertEquals("speed selected directly from playing screen",MainActivity.SPEEDS[level-1],a.speed);
                assertTrue(a.game.getAccessibilityNodeProvider().createAccessibilityNodeInfo(10+level).isSelected());
                a.game.toolbarAction(10+level);assertEquals("tap selected speed returns to normal",1f,a.speed);
            }
            a.speed=1;
            int originalW=a.game.getWidth(),originalH=a.game.getHeight();
            for(int orientation=0;orientation<2;orientation++){
                a.game.layout(0,0,orientation==0?1080:1920,orientation==0?1920:1080);
                a.gameWide=orientation==1;
                android.graphics.Bitmap output=android.graphics.Bitmap.createBitmap(a.game.getWidth(),a.game.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                float previous=0;
                a.prefs.edit().putInt("screenSize",0).apply();
                int levels=orientation==0?2:8;
                for(int size=0;size<levels;size++){
                    a.game.draw(new android.graphics.Canvas(output));
                    a.gameWide=orientation==1;
                    try(FileOutputStream shot=new FileOutputStream(new File(a.getFilesDir(),"v130-"+orientation+"-"+size+".png"))){output.compress(android.graphics.Bitmap.CompressFormat.PNG,100,shot);}catch(IOException ex){throw new AssertionError(ex);}
                    assertTrue("screen does not shrink",a.game.screen.width()>=previous);previous=a.game.screen.width();
                    if(orientation==1&&size==levels-1)assertEquals("landscape full screen uses device width",(float)a.game.getWidth(),a.game.screen.width(),.001f);
                    if(orientation==1&&size==levels-1){assertEquals("landscape full mode fills phone height",(float)a.game.getHeight(),a.game.screen.height(),.001f);assertTrue("A overlays game",android.graphics.RectF.intersects(a.game.screen,a.game.rects[4]));}
                    else assertEquals("preserves GBA aspect",1.5f,a.game.screen.width()/a.game.screen.height(),.001f);
                    assertTrue("screen inside viewport",a.game.screen.left>=-1&&a.game.screen.right<=a.game.getWidth()+1&&a.game.screen.bottom<=a.game.getHeight()+1);
                    a.game.getAccessibilityNodeProvider().performAction(16,AccessibilityNodeInfo.ACTION_CLICK,null);
                    assertEquals("size saved and cycles back",(size+1)%levels,a.prefs.getInt("screenSize",-1));
                }
                output.recycle();
            }
            a.game.layout(0,0,originalW,originalH);a.game.layout();
            RectF right=a.game.rects[3],up=a.game.rects[0],action=a.game.rects[4];
            assertEquals("default diagonal pad",80,a.game.hitKeys(right.centerX(),up.centerY()));
            MotionEvent.PointerProperties[] p={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};
            MotionEvent.PointerCoords[] c={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
            p[0].id=0;p[1].id=1;p[0].toolType=p[1].toolType=MotionEvent.TOOL_TYPE_FINGER;
            c[0].x=right.centerX();c[0].y=right.centerY();c[1].x=action.centerX();c[1].y=action.centerY();
            MotionEvent e=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,2,p,c,0,0,1,1,0,0,0,0);
            a.game.onTouchEvent(e);e.recycle();assertEquals("multitouch dispatch",17,a.touchKeys);
            e=MotionEvent.obtain(0,0,MotionEvent.ACTION_CANCEL,0,0,0);a.game.onTouchEvent(e);e.recycle();assertEquals(0,a.touchKeys);
            a.editing=true;float old=a.game.pos[4][0];long now=SystemClock.uptimeMillis();
            e=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,action.centerX(),action.centerY(),0);a.game.onTouchEvent(e);e.recycle();
            e=MotionEvent.obtain(now,now+10,MotionEvent.ACTION_MOVE,action.centerX()-60,action.centerY(),0);a.game.onTouchEvent(e);e.recycle();
            e=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,action.centerX()-60,action.centerY(),0);a.game.onTouchEvent(e);e.recycle();
            assertTrue("drag changed button",a.game.pos[4][0]<old);
            float moved=a.game.pos[4][0];a.game.layoutKey="";a.game.layout();assertEquals("layout restored",moved,a.game.pos[4][0]);
            a.prefs.edit().remove("layout.portrait").remove("layout.landscape").apply();a.game.layoutKey="";a.editing=false;
            for(int color=0;color<7;color++){a.prefs.edit().putInt("shellColor",color).apply();android.graphics.Bitmap shell=android.graphics.Bitmap.createBitmap(a.game.getWidth(),a.game.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);a.game.draw(new android.graphics.Canvas(shell));shell.recycle();}
            a.prefs.edit().putInt("shellColor",2).apply();
            for(int filter=0;filter<3;filter++){a.prefs.edit().putInt("filter",filter).putBoolean("color",true).apply();android.graphics.Bitmap target=android.graphics.Bitmap.createBitmap(a.game.getWidth(),a.game.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);a.game.draw(new android.graphics.Canvas(target));target.recycle();}
            a.prefs.edit().putInt("filter",0).putBoolean("color",false).apply();
            assertNotNull("accessible A button",a.game.getAccessibilityNodeProvider().createAccessibilityNodeInfo(4));
            try{a.saveState("slot1");}catch(IOException ex){throw new AssertionError(ex);}
            a.checkpoint();assertTrue("lifecycle checkpoint exists",a.saveFile("auto").exists());
            a.start();assertTrue(a.running);
            a.stop();assertFalse(a.running);a.checkpoint();a.finish();
        });
    }
}
