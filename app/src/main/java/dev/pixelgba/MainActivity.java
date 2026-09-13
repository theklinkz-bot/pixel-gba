package dev.pixelgba;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    static final int BG=0xff111c25, PANEL=0xff20313d, INK=0xffedf5e2, MUTED=0xff99adae, LIME=0xffc8f078, ORANGE=0xffffae70;
    SharedPreferences prefs;
    File games, saves, current;
    String gameId;
    GameView game;
    volatile boolean running;
    volatile int touchKeys, hardwareKeys, axisKeys;
    volatile float speed=1;
    volatile boolean gameWide;
    static final float[] SPEEDS={1.5f,2,4,8,16};
    static final String[] SPEED_LABELS={"1.5","2","4","8","16"};
    static final int[] SHELL_COLORS={0xffe73950,0xffffc928,0xff344de5,0xff27cc73,0xff363943,0xff28c8ed,0xffe9edf3};
    static final String[] SHELL_NAMES={"RED / แดงใส","YELLOW / เหลืองใส","BLUE / น้ำเงินใส","GREEN / เขียวใส","BLACK / ดำใส","CYAN / ฟ้าใส","WHITE / ขาวใส"};
    Thread worker;
    boolean foreground, overlay, editing, screenEditing;
    final UpdateManager updates = new UpdateManager(this);
    int slot=1;
    final Bitmap bitmap=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888);
    final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs=getSharedPreferences("pixelgba",MODE_PRIVATE);
        games=new File(getFilesDir(),"games"); saves=new File(getFilesDir(),"saves"); games.mkdirs(); saves.mkdirs();
        library();
    }
    int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    TextView text(String s, int size, int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.MONOSPACE); t.setPadding(dp(4),dp(7),dp(4),dp(7)); return t;
    }
    String appVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "?";}}
    GradientDrawable box(int color) { GradientDrawable b=new GradientDrawable(); b.setColor(color); b.setStroke(dp(1),0xff42535b); return b; }
    Button button(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(INK); b.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); b.setBackground(box(PANEL)); b.setPadding(dp(12),dp(10),dp(12),dp(10)); b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(5),0,dp(5)); b.setLayoutParams(p); return b;
    }
    LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20),dp(16),dp(20),dp(16)); l.setBackgroundColor(BG); return l; }
    void display(View v) {
        v.setOnApplyWindowInsetsListener((view,insets)-> { view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this); frame.setBackgroundColor(BG); frame.addView(v); frame.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets();});
        v.setOnApplyWindowInsetsListener(null); setContentView(frame);
    }
    String title(File f) { return prefs.getString("title."+f.getName().replace(".gba",""),"Untitled cartridge"); }
    void library() {
        getWindow().getDecorView().setSystemUiVisibility(0);
        game=null; current=null; gameId=null; editing=false;
        ScrollView scroll=new ScrollView(this); LinearLayout root=column(); scroll.addView(root); display(scroll);
        root.addView(text("P  /  G     •     HANDHELD CLUB",12,LIME));
        root.addView(text("PIXEL\nADVANCE",38,INK));
        root.addView(text("YOUR POCKET. YOUR PLAY.",12,MUTED));
        TextView hero=text(" ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n █   ░  ▄▀▄  ░   █\n █  ░  ▀▄▀▄▀  ░  █\n ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n      +      B  A",19,LIME); hero.setGravity(Gravity.CENTER); hero.setBackground(box(PANEL)); hero.setPadding(0,dp(22),0,dp(22)); root.addView(hero);
        root.addView(button("+  IMPORT CARTRIDGE  (.gba / .zip)",this::importRom));
        root.addView(text("ไฟล์เกมเก็บในเครื่อง • เล่นแบบออฟไลน์",12,MUTED));
        File[] files=games.listFiles((d,n)->n.endsWith(".gba"));
        if(files==null) files=new File[0];
        Arrays.sort(files,(a,b)->Long.compare(prefs.getLong("played."+b.getName(),0),prefs.getLong("played."+a.getName(),0)));
        root.addView(text("COLLECTION  /  "+String.format(Locale.US,"%02d",files.length),15,LIME));
        for(File f:files) {
            String id=f.getName().replace(".gba","");
            Button b=button("▣  "+title(f)+"\n"+(new File(saves,id+".auto").exists()?"CONTINUE  →":"PLAY CARTRIDGE  →"),()->launch(f));
            b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setMinHeight(dp(80)); b.setOnLongClickListener(v->{ new AlertDialog.Builder(this).setTitle(title(f)).setMessage("นำเกมออกจากคลัง? ไฟล์เซฟยังเก็บไว้ในแอป").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{if(!f.delete()) toast("Cannot remove cartridge"); library();}).show(); return true;}); root.addView(b);
        }
        root.addView(button("▶  PIXEL LAB  /  ทดลองปุ่มและเซฟ",()->{try(InputStream in=getAssets().open("pixel-lab.gba")){File f=storeRom(in,"Pixel Lab • Homebrew");launch(f);}catch(Exception e){error(e);}}));
        root.addView(button("SETTINGS  /  ภาพและเสียง",this::settings));
        root.addView(button("↻  CHECK FOR UPDATES  /  ตรวจ OTA",()->updates.check(true)));
        root.addView(button("ABOUT  /  วิธีใช้งาน",()-> new AlertDialog.Builder(this).setTitle("PIXEL GBA "+appVersion()).setMessage("นำเข้าไฟล์ .gba หรือ ZIP ที่มีเกมเดียว\nระหว่างเล่น แตะ MENU เพื่อใส่สูตร ปรับภาพ จัดปุ่ม และจัดการเซฟ\nจอย: D-pad / left stick, A/B, L/R, Start/Select\nKeyboard: arrows, Z/X, A/S, Enter/Space\n\nPixel Lab: เลื่อนสี่เหลี่ยมด้วย D-pad; A/B เปลี่ยนสี; L/R เปลี่ยนเสียง\n\nPowered by mGBA 0.10.5 (MPL 2.0). Source: github.com/mgba-emu/mgba/tree/0.10.5\nมี license ใน APK; source และวิธี build อยู่ในโปรเจกต์\nไม่รวมเกมเชิงพาณิชย์").setPositiveButton("OK",null).show()));
        root.addView(text("● OFFLINE READY      mGBA / 0.10.5",11,MUTED));
    }
    void importRom() { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,10); }
    File storeRom(InputStream in,String name) throws Exception {
        byte[] data=Storage.readRom(in); String id=Storage.hash(data); File dest=new File(games,id+".gba");
        if(!dest.exists()) Storage.write(dest,data);
        prefs.edit().putString("title."+id,name.replaceFirst("(?i)\\.gba$","")).apply(); return dest;
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if(result!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        if(request==10) {
            toast("Importing cartridge…");
            new Thread(()-> { try {
                String name="Cartridge.gba";
                try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}
                try(InputStream raw=getContentResolver().openInputStream(uri); BufferedInputStream in=new BufferedInputStream(raw)) {
                    in.mark(4); int a=in.read(),b=in.read();in.reset();
                    if(a==0x50&&b==0x4b) {
                        // ponytail: one cartridge per ZIP keeps selection and import unambiguous.
                        try(ZipInputStream zip=new ZipInputStream(in)) {
                            ZipEntry e; byte[] rom=null; String entryName=null; int entries=0;long skipped=0;
                            while((e=zip.getNextEntry())!=null) {
                                if(++entries>256)throw new IOException("ZIP contains too many entries");
                                if(!e.isDirectory()&&e.getName().toLowerCase(Locale.ROOT).endsWith(".gba")) {if(rom!=null)throw new IOException("Please use a ZIP containing one .gba game");rom=Storage.readRom(zip);entryName=new File(e.getName()).getName();}
                                else { byte[] skip=new byte[8192];int n;while((n=zip.read(skip))!=-1){skipped+=n;if(skipped>Storage.MAX_ROM)throw new IOException("ZIP contains too much unrelated data");} }
                            }
                            if(rom==null)throw new IOException("No .gba game found in ZIP"); storeRom(new ByteArrayInputStream(rom),entryName);
                        }
                    } else storeRom(in,name);
                }
                runOnUiThread(()->{library();toast("Cartridge added");});
            } catch(Exception e){runOnUiThread(()->error(e));} },"rom-import").start();
        } else if(request==20) {
            try(OutputStream out=getContentResolver().openOutputStream(uri); ZipOutputStream zip=new ZipOutputStream(out)) {
                for(File f:Objects.requireNonNull(saves.listFiles((d,n)->n.startsWith(gameId+".")&&!n.endsWith(".tmp")))) {zip.putNextEntry(new ZipEntry(f.getName()));Files.copy(f.toPath(),zip);zip.closeEntry();}
                toast("Save backup exported");
            }catch(Exception e){error(e);}
        } else if(request==21) {
            try(InputStream in=getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1024*1024)throw new IOException("Save too large");out.write(buf,0,n);}
                byte[] bytes=out.toByteArray();if(bytes.length!=512&&bytes.length!=8192&&bytes.length!=32768&&bytes.length!=65536&&bytes.length!=131072)throw new IOException("Unsupported .sav size");
                stop(); Storage.write(saveFile("sav"),bytes); Core.close();
                if(!Core.open(current.getPath(),saveFile("sav").getPath()))throw new IOException("Could not reload game");
                applyCheats(); toast("Battery save imported; game restarted"); start();
            }catch(Exception e){error(e);}
        }
    }
    File saveFile(String suffix) {return new File(saves,gameId+"."+suffix);}
    void launch(File file) {
        stop();current=file;gameId=file.getName().replace(".gba","");
        if(!Core.open(file.getPath(),saveFile("sav").getPath())) {Core.close();library();toast("Unable to load this GBA ROM");return;}
        if(saveFile("auto").exists()&&!Core.state(saveFile("auto").getPath(),true))toast("Auto-save could not load; starting cartridge");
        applyCheats(); prefs.edit().putLong("played."+file.getName(),System.currentTimeMillis()).apply();
        game=new GameView(this);display(game);updateImmersive();start();
    }
    void start() {
        if(running||current==null||!foreground||overlay||editing)return;
        running=true;
        worker=new Thread(()-> {
            AudioTrack track=null;
            try {
                int min=AudioTrack.getMinBufferSize(32768,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
                track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(32768).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(8192,min)).build();track.play();
                short[] audio=new short[4096];long next=System.nanoTime(), checkpoint=System.nanoTime();
                while(running) {
                    int n; synchronized(bitmap){n=Core.frame(bitmap,audio,touchKeys|hardwareKeys|axisKeys,speed);}
                    if(game!=null)game.postInvalidate();
                    if(prefs.getBoolean("sound",true)&&n>0){int written=track.write(audio,0,n,AudioTrack.WRITE_BLOCKING);if(written<0)throw new IOException("Audio output failed: "+written);}
                    next+=16742706L;long delay=next-System.nanoTime();
                    if(delay>0) java.util.concurrent.locks.LockSupport.parkNanos(delay);else if(delay < -100000000L)next=System.nanoTime();
                    if(System.nanoTime()-checkpoint>15000000000L){checkpoint=System.nanoTime();try{battery();}catch(Exception e){runOnUiThread(()->error(e));}}
                }
            } catch(Exception e) {running=false;runOnUiThread(()->error(e));}
            finally{if(track!=null){track.pause();track.flush();track.release();}}
        },"gba-emulation");worker.start();
    }
    void stop() {
        running=false;
        if(worker!=null){try{worker.join();}catch(InterruptedException e){Thread.currentThread().interrupt();}worker=null;}
        touchKeys=hardwareKeys=axisKeys=0;
    }
    void battery() throws IOException {
        File tmp=saveFile("sav.tmp");if(!Core.flush(tmp.getPath()))throw new IOException("Could not write battery save");if(tmp.exists())Storage.replace(tmp,saveFile("sav"));
    }
    void saveState(String suffix) throws IOException {
        File tmp=saveFile(suffix+".tmp");if(!Core.state(tmp.getPath(),false))throw new IOException("Save state failed");Storage.replace(tmp,saveFile(suffix));battery();
    }
    void checkpoint() {if(current!=null)try{saveState("auto");}catch(Exception e){error(e);}}
    @Override protected void onResume(){super.onResume();foreground=true;start();}
    @Override protected void onPause(){foreground=false;stop();checkpoint();super.onPause();}
    @Override protected void onDestroy(){stop();Core.close();super.onDestroy();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);updateImmersive();if(game!=null){game.layoutKey="";game.invalidate();}}
    @Override public void onBackPressed(){if(screenEditing){screenEditing=false;game.invalidate();start();}else if(editing){editing=false;game.saveLayout();game.invalidate();start();}else if(current!=null)menu();else super.onBackPressed();}
    void updateImmersive(){
        getWindow().getDecorView().setSystemUiVisibility(0);
    }
    int screenSizeLimit(){
        boolean wide=game!=null?gameWide:getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        return wide?7:2;
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    void error(Exception e){toast(e.getMessage()==null?e.toString():e.getMessage());}
    void dialog(String title,View body) {
        stop();overlay=true;
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("DONE",null).create();
        d.setOnDismissListener(v->{overlay=false;start();});d.show();
    }
    void menu() {
        if(overlay)return;stop();overlay=true;
        String[] items={"RESUME  /  เล่นต่อ","QUICK SAVE  /  Slot "+slot,"QUICK LOAD  /  Slot "+slot,"SAVE SLOTS  /  จัดการเซฟ","CHEATS  /  สูตรโกง","GRAPHICS & AUDIO  /  ภาพและเสียง","ADJUST SCREEN  /  ปรับขนาดและตำแหน่งจอ","EDIT CONTROLS  /  จัดตำแหน่งปุ่ม","SPEED  /  "+speed+"×","BACKUP SAVES  /  ส่งออกเซฟ","IMPORT .SAV  /  นำเข้าเซฟ","LIBRARY  /  กลับคลังเกม"};
        AlertDialog d=new AlertDialog.Builder(this).setTitle("PAUSED  /  "+title(current)).setItems(items,(di,w)->{
            overlay=false;
            switch(w){
                case 0:break;
                case 1:try{saveState("slot"+slot);toast("Saved to slot "+slot);}catch(Exception e){error(e);}break;
                case 2:toast(Core.state(saveFile("slot"+slot).getPath(),true)?"State loaded":"No valid save in this slot");break;
                case 3:handler.post(this::slots);break;
                case 4:handler.post(this::cheats);break;
                case 5:handler.post(this::settings);break;
                case 6:screenEditing=true;handler.post(this::screenSettings);break;
                case 7:editing=true;handler.post(this::controlSettings);break;
                case 8:handler.post(()->new AlertDialog.Builder(this).setTitle("FAST FORWARD").setItems(new String[]{"1× / Normal","1.5×","2×","4×","8×","16×"},(v,i)->{speed=i==0?1:SPEEDS[i-1];if(game!=null)game.invalidate();}).show());break;
                case 9:checkpoint();startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"pixelgba-saves.zip"),20);break;
                case 10:handler.post(()-> new AlertDialog.Builder(this).setTitle("Replace battery save?").setMessage("เลือก .sav ของเกมนี้ จะเริ่มเกมใหม่จากเซฟที่นำเข้า ควร Export backup ก่อน").setNegativeButton("Cancel",null).setPositiveButton("Choose .sav",(a,b)->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),21)).show());break;
                case 11:checkpoint();Core.close();library();break;
            }
        }).create();d.setOnDismissListener(v->{overlay=false;start();});d.show();
    }
    void slots() {
        LinearLayout l=column();l.addView(text("เลือกรายการเพื่อบันทึกหรือโหลด",12,MUTED));
        for(int i=1;i<=3;i++){final int s=i;File f=saveFile("slot"+i);String stamp=f.exists()?android.text.format.DateFormat.format("dd MMM HH:mm",f.lastModified()).toString():"EMPTY";
            l.addView(text("SLOT "+i+"   "+stamp,15,LIME));
            l.addView(button("SAVE → "+i,()->{slot=s;try{saveState("slot"+s);toast("Saved slot "+s);}catch(Exception e){error(e);}}));
            l.addView(button("LOAD ← "+i,()->{slot=s;toast(Core.state(saveFile("slot"+s).getPath(),true)?"Loaded slot "+s:"Slot empty or invalid");}));
        }dialog("SAVE STATION",l);
    }
    void settings() {
        LinearLayout l=column(); l.addView(text("DISPLAY FILTER",13,LIME));
        RadioGroup group=new RadioGroup(this);String[] labels={"PIXEL • crisp nearest-neighbor","SMOOTH • bilinear","CRT • scanlines"};
        for(int i=0;i<3;i++){RadioButton r=new RadioButton(this);r.setText(labels[i]);r.setTextColor(INK);r.setId(100+i);group.addView(r);}group.check(100+prefs.getInt("filter",0));group.setOnCheckedChangeListener((g,id)->{prefs.edit().putInt("filter",id-100).apply();if(game!=null)game.invalidate();});l.addView(group);
        check(l,"INTEGER SCALE • พิกเซลขนาดเท่ากัน","integer",false);
        check(l,"COLOR • ลดความจัดของสี","color",false);
        check(l,"SOUND • เปิดเสียง","sound",true);
        check(l,"SHOW FPS • แสดงเฟรมเรต","fps",false);
        l.addView(text("PORTRAIT SHELL / สีตัวเครื่องแนวตั้ง",13,LIME));
        RadioGroup shells=new RadioGroup(this);
        for(int i=0;i<SHELL_NAMES.length;i++){RadioButton r=new RadioButton(this);r.setId(200+i);r.setText(SHELL_NAMES[i]);r.setTextColor(INK);shells.addView(r);}
        shells.check(200+prefs.getInt("shellColor",2));shells.setOnCheckedChangeListener((g,id)->{prefs.edit().putInt("shellColor",id-200).apply();if(game!=null)game.invalidate();});l.addView(shells);
        l.addView(text("GBA native: 240 × 160 / 3:2\nFast-forward 1.5×–16× keeps audio on.\nปรับการขยายภาพ ไม่เปลี่ยนความละเอียดภายในเกม",12,MUTED));dialog("PIXEL / DISPLAY",l);
    }
    void check(LinearLayout l,String label,String key,boolean fallback){CheckBox c=new CheckBox(this);c.setText(label);c.setTextColor(INK);c.setChecked(prefs.getBoolean(key,fallback));c.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean(key,v).apply();if(game!=null)game.invalidate();});l.addView(c);}
    JSONArray cheatList(){try{return new JSONArray(prefs.getString("cheats."+gameId,"[]"));}catch(JSONException e){return new JSONArray();}}
    void applyCheats(){Core.clearCheats();JSONArray a=cheatList();for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null&&c.optBoolean("enabled")&&!Core.cheat(c.optString("name"),c.optString("code"),c.optInt("type")))toast("Invalid cheat: "+c.optString("name"));}}
    void cheatRows(LinearLayout rows,JSONArray list){
        rows.removeAllViews();
        for(int i=0;i<list.length();i++){
            JSONObject c=list.optJSONObject(i);if(c==null)continue;final int index=i;
            CheckBox cb=new CheckBox(this);cb.setText(c.optString("name"));cb.setTextColor(INK);cb.setChecked(c.optBoolean("enabled"));
            cb.setOnCheckedChangeListener((b,v)->{try{c.put("enabled",v);prefs.edit().putString("cheats."+gameId,list.toString()).apply();applyCheats();}catch(JSONException e){error(e);}});rows.addView(cb);
            rows.addView(text(c.optString("code"),12,MUTED));
            rows.addView(button("DELETE / "+c.optString("name"),()->{list.remove(index);prefs.edit().putString("cheats."+gameId,list.toString()).apply();applyCheats();cheatRows(rows,list);}));
        }
    }
    void cheats() {
        LinearLayout l=column();l.addView(text("บันทึกสูตรแยกตามเกม\nใช้รหัสที่ตรงกับเกมและเวอร์ชัน ROM",12,MUTED));
        JSONArray list=cheatList();
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);l.addView(rows);cheatRows(rows,list);
        EditText name=new EditText(this);name.setHint("Cheat name");name.setTextColor(INK);name.setHintTextColor(MUTED);l.addView(name);
        Spinner type=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Auto detect","CodeBreaker","GameShark","Action Replay","VBA raw"});type.setAdapter(adapter);l.addView(type);
        EditText code=new EditText(this);code.setHint("XXXXXXXX XXXXXXXX\nOne code per line");code.setTextColor(INK);code.setHintTextColor(MUTED);code.setMinLines(3);code.setMaxLines(8);code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);l.addView(code);
        l.addView(button("+ ADD & ENABLE",()->{
            String n=name.getText().toString().trim(),raw=code.getText().toString().trim().toUpperCase(Locale.ROOT);
            if(n.isEmpty()||raw.isEmpty()||raw.length()>8192||list.length()>=64){toast("Enter name and code (up to 64 cheats / 8 KB each)");return;}
            for(String line:raw.split("\\R")){if(!line.trim().matches("[0-9A-F]{8}[ :][0-9A-F]{2,8}")){toast("Use 8 hex digits, space or colon, then 2–8 hex digits per line");return;}}
            if(!Core.cheat(n,raw,type.getSelectedItemPosition())){toast("mGBA rejected this code; check format and type");return;}
            try{JSONObject c=new JSONObject().put("name",n).put("code",raw).put("type",type.getSelectedItemPosition()).put("enabled",true);list.put(c);prefs.edit().putString("cheats."+gameId,list.toString()).apply();name.setText("");code.setText("");cheatRows(rows,list);toast("Cheat enabled");}catch(JSONException e){error(e);}
        }));dialog("CHEAT CARTRIDGE",l);
    }
    void screenSettings() {
        LinearLayout l=column();
        if(game!=null&&game.getWidth()>game.getHeight())game.manualScale=game.screen.width()/240f;
        l.addView(text("แนวนอนเท่านั้น: แตะ DONE แล้วลากมุมกรอบเพื่อขยาย/ย่อ\nลากตรงกลางภาพเพื่อเลื่อนตำแหน่ง\nภาพคงสัดส่วน 3:2 และขยายได้เต็มพื้นที่รวมแถบด้านบน",13,LIME));
        l.addView(button("RESET SCREEN POSITION",()->{manualScreenReset();if(game!=null)game.invalidate();}));
        dialog("SCREEN WORKSHOP",l);
    }
    void manualScreenReset(){if(game!=null){game.manualScale=0;game.panX=0;game.panY=0;}}
    void controlSettings() {
        LinearLayout l=column();l.addView(text("แตะ DONE แล้วลากปุ่มบนจอ\nแตะ FINISH เพื่อบันทึกตำแหน่ง\nจัดแยกตามแนวตั้ง / แนวนอน",13,LIME));
        l.addView(text("BUTTON SIZE",13,INK));SeekBar size=new SeekBar(this);size.setMax(100);size.setProgress(prefs.getInt("size",40));size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){prefs.edit().putInt("size",p).apply();game.invalidate();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});l.addView(size);
        l.addView(text("BUTTON OPACITY",13,INK));SeekBar opacity=new SeekBar(this);opacity.setMax(75);opacity.setProgress(prefs.getInt("opacity",65)-25);opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){prefs.edit().putInt("opacity",p+25).apply();game.invalidate();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});l.addView(opacity);
        l.addView(button("RESET POSITIONS",()->{prefs.edit().remove("layout.portrait").remove("layout.landscape").apply();game.layoutKey="";game.invalidate();}));dialog("CONTROL WORKSHOP",l);
    }
    int mapKey(int code){switch(code){case KeyEvent.KEYCODE_BUTTON_A:case KeyEvent.KEYCODE_Z:return 1;case KeyEvent.KEYCODE_BUTTON_B:case KeyEvent.KEYCODE_X:return 2;case KeyEvent.KEYCODE_BUTTON_SELECT:case KeyEvent.KEYCODE_SPACE:return 4;case KeyEvent.KEYCODE_BUTTON_START:case KeyEvent.KEYCODE_ENTER:return 8;case KeyEvent.KEYCODE_DPAD_RIGHT:return 16;case KeyEvent.KEYCODE_DPAD_LEFT:return 32;case KeyEvent.KEYCODE_DPAD_UP:return 64;case KeyEvent.KEYCODE_DPAD_DOWN:return 128;case KeyEvent.KEYCODE_BUTTON_R1:case KeyEvent.KEYCODE_S:return 256;case KeyEvent.KEYCODE_BUTTON_L1:case KeyEvent.KEYCODE_A:return 512;default:return 0;}}
    @Override public boolean dispatchKeyEvent(KeyEvent e){int mask=mapKey(e.getKeyCode());if(current!=null&&!overlay&&!editing&&mask!=0){if(e.getAction()==KeyEvent.ACTION_DOWN)hardwareKeys|=mask;else if(e.getAction()==KeyEvent.ACTION_UP)hardwareKeys&=~mask;return true;}return super.dispatchKeyEvent(e);}
    @Override public boolean onGenericMotionEvent(MotionEvent e){if(current!=null&&!overlay&&!editing&&(e.getSource()&InputDevice.SOURCE_JOYSTICK)!=0){float x=e.getAxisValue(MotionEvent.AXIS_HAT_X)+e.getAxisValue(MotionEvent.AXIS_X),y=e.getAxisValue(MotionEvent.AXIS_HAT_Y)+e.getAxisValue(MotionEvent.AXIS_Y);axisKeys=(x>.4?16:x<-.4?32:0)|(y>.4?128:y<-.4?64:0);return true;}return super.onGenericMotionEvent(e);}

    final class GameView extends View {
        final Paint paint=new Paint(); final RectF screen=new RectF();
        final ColorMatrixColorFilter correction;
        final String[] labels={"↑","↓","←","→","A","B","L","R","SELECT","START"};
        final int[] masks={64,128,32,16,1,2,512,256,4,8};
        final float[][] pos=new float[10][2];final RectF[] rects=new RectF[10];
        String layoutKey="";int drag=-1,dragPointer=-1;float dx,dy;int resizeGesture;float manualScale,panX,panY,gesturePanX,gesturePanY;long fpsTime=System.nanoTime();int draws;String fps="60";
        int accessibilityFocus=-1,hover=-1;
        GameView(Context c){super(c);ColorMatrix colors=new ColorMatrix();colors.setSaturation(.72f);correction=new ColorMatrixColorFilter(colors);setFocusable(true);setContentDescription("GBA screen and touch controls. Use Menu for controls and settings.");for(int i=0;i<10;i++)rects[i]=new RectF();}
        void layout(){
            boolean wide=getWidth()>getHeight();String key=wide?"landscape":"portrait";
            if(!key.equals(layoutKey)){
                layoutKey=key;
                float[][] defaults=wide?new float[][]{{.13f,.57f},{.13f,.83f},{.065f,.70f},{.195f,.70f},{.91f,.61f},{.81f,.79f},{.085f,.22f},{.915f,.22f},{.39f,.91f},{.61f,.91f}}:new float[][]{{.23f,.77f},{.23f,.95f},{.10f,.84f},{.36f,.84f},{.85f,.79f},{.67f,.91f},{.13f,.65f},{.87f,.65f},{.43f,.65f},{.57f,.65f}};
                try{JSONArray a=new JSONArray(prefs.getString("layout."+key,"[]"));for(int i=0;i<10;i++){JSONArray p=a.optJSONArray(i);pos[i][0]=p==null?defaults[i][0]:(float)p.getDouble(0);pos[i][1]=p==null?defaults[i][1]:(float)p.getDouble(1);}}catch(JSONException e){for(int i=0;i<10;i++)pos[i]=defaults[i].clone();}
            }
            float size=dp(38+prefs.getInt("size",40)*.34f);
            if(!wide&&!prefs.contains("layout.portrait")){
                float arm=Math.min(size*.80f,getWidth()*.13f);
                pos[0][1]=.84f-arm/getHeight();pos[1][1]=.84f+arm/getHeight();
                pos[2][0]=.23f-arm/getWidth();pos[3][0]=.23f+arm/getWidth();pos[2][1]=pos[3][1]=.84f;
            }
            for(int i=0;i<10;i++){float w=i>=8?(wide?size*1.25f:size*.52f):!wide&&i>=6?size*1.45f:size,h=i>=8&&!wide?size*.52f:i>=6?size*.62f:size;float x=Math.max(w/2,Math.min(getWidth()-w/2,pos[i][0]*getWidth())),y=Math.max(dp(65)+h/2,Math.min(getHeight()-h/2,pos[i][1]*getHeight()));rects[i].set(x-w/2,y-h/2,x+w/2,y+h/2);}
        }
        void saveLayout(){try{JSONArray a=new JSONArray();for(float[] p:pos)a.put(new JSONArray().put((double)p[0]).put((double)p[1]));prefs.edit().putString("layout."+layoutKey,a.toString()).apply();}catch(JSONException e){error(e);}}
        int hitKeys(float x,float y){
            int result=0;for(int i=0;i<10;i++)if(rects[i].contains(x,y))result|=masks[i];
            // A continuous pad permits diagonal movement with one thumb in the default layout.
            if(!prefs.contains("layout."+layoutKey)){
                float cx=rects[0].centerX(),cy=rects[2].centerY();
                if(x>=rects[2].left&&x<=rects[3].right&&y>=rects[0].top&&y<=rects[1].bottom){
                    float nx=(x-cx)/(rects[3].centerX()-cx),ny=(y-cy)/(rects[1].centerY()-cy);
                    result|=(nx>.3?16:nx<-.3?32:0)|(ny>.3?128:ny<-.3?64:0);
                }
            }
            return result;
        }
        String controlName(int id){return id==10?(editing?"Finish control layout":"Pause and open menu"):id==16?"Screen size level "+(prefs.getInt("screenSize",0)+1)+" of "+screenSizeLimit():id>=11?"Speed "+SPEED_LABELS[id-11]+" times; tap again for normal speed":labels[id]+". Long press to hold or release.";}
        final RectF[] toolbarRects={new RectF(),new RectF(),new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        RectF toolbarBounds(int id){
            float w=getWidth();
            RectF r=toolbarRects[id-10];
            if(id==10)r.set(w-dp(72),0,w,dp(48));
            else if(id==16){if(getHeight()>w){float right=Math.min(w,screen.right);r.set(right-dp(48),Math.max(dp(48),screen.top),right,Math.max(dp(48),screen.top)+dp(48));}else r.set(w-dp(128),0,w-dp(72),dp(48));}
            else {float cell=Math.min(dp(48),(w-dp(getHeight()>w?80:136))/5);r.set(dp(4)+(id-11)*cell,0,dp(4)+(id-10)*cell,dp(48));}
            return r;
        }
        int toolbarHit(float x,float y){for(int id=10;id<=16;id++)if(toolbarBounds(id).contains(x,y))return id;return -1;}
        void toolbarAction(int id){
            if(id==10){if(screenEditing){screenEditing=false;start();}else if(editing){saveLayout();editing=false;start();}else menu();}
            else if(id==16){int size=(prefs.getInt("screenSize",0)+1)%screenSizeLimit();prefs.edit().putInt("screenSize",size).apply();updateImmersive();announceForAccessibility(controlName(id));}
            else if(id>=11&&id<=15&&!editing){speed=speed==SPEEDS[id-11]?1:SPEEDS[id-11];announceForAccessibility(controlName(id));}
            invalidate();
        }
        void accessibleEvent(int id,int type){if(!((AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE)).isEnabled())return;AccessibilityEvent event=AccessibilityEvent.obtain(type);event.setPackageName(getPackageName());event.setClassName("android.widget.Button");event.setSource(this,id);event.setContentDescription(controlName(id));if(getParent()!=null)getParent().requestSendAccessibilityEvent(this,event);}
        @Override public AccessibilityNodeProvider getAccessibilityNodeProvider(){return new AccessibilityNodeProvider(){
            @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
                AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain();n.setPackageName(getPackageName());n.setSource(GameView.this,id);
                if(id==View.NO_ID){GameView.this.onInitializeAccessibilityNodeInfo(n);for(int i=0;i<=16;i++)n.addChild(GameView.this,i);return n;}
                if(id<0||id>16)return null;
                n.setParent(GameView.this);n.setClassName("android.widget.Button");n.setContentDescription(controlName(id));n.setEnabled(true);n.setVisibleToUser(isShown());n.setFocusable(true);n.setClickable(true);n.setLongClickable(id<10);n.setSelected(id>=11&&id<=15&&speed==SPEEDS[id-11]);
                Rect bounds=new Rect();if(id>=10)toolbarBounds(id).roundOut(bounds);else rects[id].roundOut(bounds);n.setBoundsInParent(bounds);int[] offset=new int[2];getLocationOnScreen(offset);bounds.offset(offset[0],offset[1]);n.setBoundsInScreen(bounds);
                n.addAction(AccessibilityNodeInfo.ACTION_CLICK);if(id<10)n.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);n.setAccessibilityFocused(accessibilityFocus==id);n.addAction(accessibilityFocus==id?AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);return n;
            }
            @Override public boolean performAction(int id,int action,Bundle args){
                if(id<0||id>16)return false;
                if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){accessibilityFocus=id;accessibleEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);return true;}
                if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibilityFocus=-1;accessibleEvent(id,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);return true;}
                if(action==AccessibilityNodeInfo.ACTION_CLICK){if(id>=10){toolbarAction(id);}else if(!editing){touchKeys|=masks[id];handler.postDelayed(()->{touchKeys&=~masks[id];invalidate();},150);}accessibleEvent(id,AccessibilityEvent.TYPE_VIEW_CLICKED);return true;}
                if(action==AccessibilityNodeInfo.ACTION_LONG_CLICK&&id<10&&!editing){touchKeys^=masks[id];invalidate();return true;}return false;
            }
        };}
        @Override public boolean dispatchHoverEvent(MotionEvent e){
            AccessibilityManager manager=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
            if(manager.isTouchExplorationEnabled()){
                int target=-1;if(e.getActionMasked()!=MotionEvent.ACTION_HOVER_EXIT){target=toolbarHit(e.getX(),e.getY());if(target<0)for(int i=0;i<10;i++)if(rects[i].contains(e.getX(),e.getY()))target=i;}
                if(target!=hover){if(hover>=0)accessibleEvent(hover,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);hover=target;if(hover>=0)accessibleEvent(hover,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);}return true;
            }return super.dispatchHoverEvent(e);
        }
        void label(Canvas c,String t,float x,float y,float size,int color){paint.setColor(color);paint.setTextSize(dp(size));paint.setTypeface(Typeface.MONOSPACE);paint.setTextAlign(Paint.Align.CENTER);paint.setAntiAlias(true);c.drawText(t,x,y,paint);paint.setAntiAlias(false);}
        void drawShell(Canvas c,float w,float h){
            float top=h*.58f;
            int tint=SHELL_COLORS[Math.max(0,Math.min(6,prefs.getInt("shellColor",2)))];
            paint.setAntiAlias(true);
            Path shell=new Path();shell.moveTo(0,top);shell.quadTo(w*.5f,top+dp(24),w,top);shell.lineTo(w,h);shell.lineTo(0,h);shell.close();
            c.save();c.clipPath(shell);
            paint.setShader(new LinearGradient(0,top,w,h,new int[]{tint,0xff141724,tint},null,Shader.TileMode.CLAMP));c.drawPath(shell,paint);paint.setShader(null);
            // Translucent resin reveals a deterministic circuit pattern beneath the shell.
            paint.setColor(0x355dedd8);paint.setStrokeWidth(dp(2));paint.setStyle(Paint.Style.STROKE);
            for(int i=0;i<9;i++){float x=w*(i+1)/10f,y=top+(h-top)*(i%4+1)/5f;Path trace=new Path();trace.moveTo(x,h);trace.lineTo(x,y+dp(18));trace.lineTo(x+dp(18),y);trace.lineTo(w,y);c.drawPath(trace,paint);c.drawCircle(x,y+dp(18),dp(5),paint);}
            paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<4;i++){RectF chip=new RectF(w*(.15f+i*.2f),top+(h-top)*.48f,w*(.25f+i*.2f),top+(h-top)*.64f);paint.setColor(0x44202738);c.drawRoundRect(chip,dp(3),dp(3),paint);}
            paint.setShader(new LinearGradient(0,top,w,top,new int[]{0x66ffffff,0x08ffffff,0x44ffffff},null,Shader.TileMode.CLAMP));c.drawPath(shell,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(0x88ffffff);c.drawPath(shell,paint);paint.setStyle(Paint.Style.FILL);c.restore();
            label(c,"PIXEL GBA",w/2,top-dp(9),14,INK);
            paint.setColor(0xffa3ffc2);c.drawCircle(w*.07f,top+dp(16),dp(3),paint);
            for(int i=0;i<5;i++){paint.setColor(0x55000000);c.drawRoundRect(w*.47f+i*dp(6),h-dp(35),w*.47f+i*dp(6)+dp(2),h-dp(13),dp(1),dp(1),paint);}
        }
        void drawPortraitControls(Canvas c,boolean full){
            int alpha=editing?240:full?Math.min(170,(int)(prefs.getInt("opacity",65)*2.55f)):(int)(prefs.getInt("opacity",65)*2.55f);
            boolean joined=!prefs.contains("layout.portrait");
            if(joined){RectF up=rects[0],down=rects[1],left=rects[2],right=rects[3];
                Path cross=new Path();cross.moveTo(up.left,up.top);cross.lineTo(up.right,up.top);cross.lineTo(up.right,right.top);cross.lineTo(right.right,right.top);cross.lineTo(right.right,right.bottom);cross.lineTo(up.right,right.bottom);cross.lineTo(up.right,down.bottom);cross.lineTo(up.left,down.bottom);cross.lineTo(up.left,left.bottom);cross.lineTo(left.left,left.bottom);cross.lineTo(left.left,left.top);cross.lineTo(up.left,left.top);cross.close();
                paint.setAntiAlias(true);paint.setColor(0xff252330);paint.setAlpha(alpha);c.drawPath(cross,paint);paint.setColor(0xffa7a1b8);paint.setAlpha(alpha);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setStrokeJoin(Paint.Join.ROUND);c.drawPath(cross,paint);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
            }
            for(int i=0;i<10;i++){RectF r=rects[i];boolean pressed=((touchKeys|hardwareKeys|axisKeys)&masks[i])!=0;
                if(i>=4||!joined){
                    paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{pressed?0xff849776:0xff565262,0xff24212f},null,Shader.TileMode.CLAMP));paint.setAlpha(alpha);float radius=i==4||i==5||i>=8?r.width()/2:dp(14);c.drawRoundRect(r,radius,radius,paint);paint.setShader(null);
                    paint.setColor(pressed?LIME:0xffaea6c0);paint.setAlpha(alpha);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawRoundRect(r,radius,radius,paint);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
                }
                if(i<4&&joined){paint.setColor(pressed?LIME:0xffccc5dc);paint.setStrokeWidth(dp(4));paint.setStrokeCap(Paint.Cap.ROUND);if(i<2)c.drawLine(r.centerX(),r.centerY()-dp(7),r.centerX(),r.centerY()+dp(7),paint);else c.drawLine(r.centerX()-dp(7),r.centerY(),r.centerX()+dp(7),r.centerY(),paint);paint.setStrokeCap(Paint.Cap.BUTT);}
                else if(i>=8)label(c,labels[i],r.centerX(),r.bottom+dp(13),8,full?INK:0xffc6c1dc);
                else label(c,labels[i],r.centerX(),r.centerY()+dp(i<6?9:5),i<6?28:14,pressed?LIME:INK);
            }
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.drawColor(BG);layout();float w=getWidth(),h=getHeight();boolean wide=w>h;gameWide=wide;
            float areaW=wide?w*.61f:w-dp(28),areaH=wide?h-dp(125):h*.40f;
            float scale=Math.min(areaW/240,areaH/160);if(prefs.getBoolean("integer",false)&&scale>=1)scale=(float)Math.floor(scale);
            float sw=240*scale,sh=160*scale,top=wide?dp(52)+(areaH-sh)/2:dp(65)+(areaH-sh)/2;
            int screenSize=Math.min(prefs.getInt("screenSize",0),screenSizeLimit()-1);
            if(screenSize>0){
                float full=Math.min(w/240,h/160),amount=screenSize/(float)(screenSizeLimit()-1);
                scale=scale+(full-scale)*amount;sw=240*scale;sh=160*scale;
                float fullTop=wide?dp(52)+(h-dp(52)-160*full)/2:dp(52);
                top=top+(fullTop-top)*amount;
            }
            if(!wide){
                c.drawColor(Color.BLACK);
                float availableHeight=h*.55f-dp(54);
                scale=Math.min(w*(screenSize==0?.88f:screenSize==1?.96f:1f)/240,(screenSize==2?h:availableHeight)/160);
                if(prefs.getBoolean("integer",false)&&screenSize==0&&scale>=1)scale=(float)Math.floor(scale);
                sw=240*scale;sh=160*scale;top=screenSize==2?(h-sh)/2:dp(48)+(availableHeight-sh)/2;
            }
            if(wide&&(manualScale>0||panX!=0||panY!=0)){if(manualScale<=0)manualScale=scale;scale=manualScale;sw=240*scale;sh=160*scale;top=(h-sh)/2+panY;screen.set((w-sw)/2+panX,top,(w+sw)/2+panX,top+sh);}
            else screen.set((w-sw)/2,top,(w+sw)/2,top+sh);
            paint.setColor(0xff43535a);c.drawRect(screen.left-dp(4),screen.top-dp(4),screen.right+dp(4),screen.bottom+dp(4),paint);
            int filter=prefs.getInt("filter",0);paint.setFilterBitmap(filter==1);
            if(prefs.getBoolean("color",false)){paint.setColorFilter(correction);}
            synchronized(bitmap){c.drawBitmap(bitmap,null,screen,paint);}paint.setColorFilter(null);paint.setFilterBitmap(false);
            if(filter==2){paint.setColor(0x40000000);for(int y=0;y<160;y++)c.drawRect(screen.left,screen.top+y*scale,screen.right,screen.top+y*scale+Math.max(1,scale*.25f),paint);}
            if(!wide&&screenSize!=2)drawShell(c,w,h);
            if(editing||screenEditing){paint.setColor(0xff30424b);for(int x=0;x<w;x+=dp(24))for(int y=0;y<h;y+=dp(24))c.drawRect(x,y,x+2,y+2,paint);}
            if(screenEditing&&wide){paint.setColor(LIME);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));c.drawRect(screen,paint);paint.setStyle(Paint.Style.FILL);for(float[] p:new float[][]{{screen.left,screen.top},{screen.right,screen.top},{screen.left,screen.bottom},{screen.right,screen.bottom}})c.drawCircle(p[0],p[1],dp(12),paint);}
            if(!wide)drawPortraitControls(c,false);
            else for(int i=0;i<10;i++){RectF r=rects[i];boolean pressed=((touchKeys|hardwareKeys|axisKeys)&masks[i])!=0;int color=pressed?LIME:i==4?LIME:i==5?ORANGE:PANEL;
                paint.setColor(color);paint.setAlpha(editing?230:(int)(prefs.getInt("opacity",65)*2.55f));c.drawRect(r,paint);paint.setAlpha(255);paint.setColor(pressed?INK:0xff66787a);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawRect(r,paint);paint.setStyle(Paint.Style.FILL);label(c,labels[i],r.centerX(),r.centerY()+dp(5),i>=8?11:18,(i==4||i==5||pressed)?BG:INK);}
            if(prefs.getBoolean("fps",false)){draws++;long now=System.nanoTime();if(now-fpsTime>1000000000L){fps=String.valueOf(Math.round(draws*1e9/(now-fpsTime)));draws=0;fpsTime=now;}label(c,fps+" FPS",w/2,screen.top+dp(16),11,LIME);}
            paint.setColor(!wide?0xb0000000:(screenEditing||screenSize==screenSizeLimit()-1)?0x880d1b24:BG);c.drawRect(0,0,w,dp(48),paint);
            for(int id=11;id<=15;id++){RectF r=toolbarBounds(id);boolean selected=speed==SPEEDS[id-11];paint.setColor(selected?LIME:PANEL);c.drawRect(r.left+dp(2),dp(8),r.right-dp(2),dp(40),paint);label(c,"▶"+SPEED_LABELS[id-11]+"×",r.centerX(),dp(29),10,selected?BG:MUTED);}
            RectF sizeButton=toolbarBounds(16);
            if(wide)label(c,"▣ "+(screenSize+1),sizeButton.centerX(),dp(29),14,LIME);
            else {paint.setColor(0xb0000000);c.drawRoundRect(sizeButton,dp(8),dp(8),paint);paint.setColor(INK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));
                float x=sizeButton.centerX(),y=sizeButton.centerY(),a=dp(10),b=dp(4);
                c.drawLines(new float[]{x-a,y-b,x-a,y-a,x-a,y-a,x-b,y-a,x+b,y+a,x+a,y+a,x+a,y+a,x+a,y+b},paint);paint.setStyle(Paint.Style.FILL);label(c,""+(screenSize+1),x,y+dp(4),9,INK);}
            label(c,editing?"DONE ✓":"MENU ≡",toolbarBounds(10).centerX(),dp(29),12,LIME);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked(),idx=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){int tool=toolbarHit(e.getX(idx),e.getY(idx));if(tool>=0&&(!screenEditing||tool==10)){toolbarAction(tool);performClick();return true;}}
            boolean wide=getWidth()>getHeight();
            if(wide&&screenEditing){
                float x=e.getX(idx),y=e.getY(idx),hit=dp(72);
                if(action==MotionEvent.ACTION_DOWN){
                    boolean corner=(Math.abs(x-screen.left)<hit||Math.abs(x-screen.right)<hit)&&(Math.abs(y-screen.top)<hit||Math.abs(y-screen.bottom)<hit);
                    if(corner){resizeGesture=1;manualScale=Math.max(.5f,screen.width()/240);return true;}
                    if(screen.contains(x,y)){resizeGesture=2;gesturePanX=panX;gesturePanY=panY;dx=x-screen.centerX();dy=y-screen.centerY();return true;}
                }else if(action==MotionEvent.ACTION_MOVE&&resizeGesture!=0){
                    if(resizeGesture==1){float s=Math.max(Math.abs(x-screen.centerX())*2/240f,Math.abs(y-screen.centerY())*2/160f);manualScale=Math.max(.5f,Math.min(Math.min(getWidth()/240f,getHeight()/160f),s));}
                    else {panX=gesturePanX+(x-screen.centerX()-dx);panY=gesturePanY+(y-screen.centerY()-dy);}
                    invalidate();return true;
                }else if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&resizeGesture!=0){resizeGesture=0;invalidate();return true;}
            }
            if(editing){
                if(action==MotionEvent.ACTION_DOWN){for(int i=9;i>=0;i--)if(rects[i].contains(e.getX(),e.getY())){drag=i;dragPointer=e.getPointerId(0);dx=rects[i].centerX()-e.getX();dy=rects[i].centerY()-e.getY();break;}}
                if(action==MotionEvent.ACTION_MOVE&&drag>=0){int p=e.findPointerIndex(dragPointer);if(p>=0){pos[drag][0]=Math.max(.04f,Math.min(.96f,(e.getX(p)+dx)/getWidth()));pos[drag][1]=Math.max(.14f,Math.min(.96f,(e.getY(p)+dy)/getHeight()));invalidate();}}
                if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){drag=-1;saveLayout();}return true;
            }
            int keys=0;if(action!=MotionEvent.ACTION_CANCEL&&action!=MotionEvent.ACTION_UP)for(int p=0;p<e.getPointerCount();p++){if(action==MotionEvent.ACTION_POINTER_UP&&p==idx)continue;if(toolbarHit(e.getX(p),e.getY(p))<0)keys|=hitKeys(e.getX(p),e.getY(p));}
            touchKeys=keys;invalidate();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
