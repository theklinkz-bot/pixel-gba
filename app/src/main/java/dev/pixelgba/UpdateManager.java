package dev.pixelgba;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;

final class UpdateManager {
    static final String MANIFEST="https://raw.githubusercontent.com/theklinkz-bot/pixel-gba/main/update.json";
    final MainActivity activity;
    UpdateManager(MainActivity activity){this.activity=activity;}

    void check(boolean announce){
        new Thread(()->{
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(MANIFEST).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestProperty("Accept","application/json");
                if(c.getResponseCode()!=200)throw new IOException("Update server returned HTTP "+c.getResponseCode());
                String body=new String(read(c.getInputStream(),128*1024),"UTF-8"); JSONObject u=new JSONObject(body);
                int code=u.optInt("versionCode",0);String name=u.optString("versionName","");String url=u.optString("apkUrl","");String sha=u.optString("sha256","").toLowerCase(Locale.ROOT);
                if(code<=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionCode){if(announce)activity.runOnUiThread(()->activity.toast("Pixel GBA is up to date"));return;}
                if(!url.startsWith("https://github.com/")||!url.endsWith(".apk"))throw new IOException("Update manifest has an unsafe APK URL");
                activity.runOnUiThread(()->offer(code,name,url,sha,u.optString("changelog","")));
            }catch(Exception e){if(announce)activity.runOnUiThread(()->activity.toast("Update check failed: "+e.getMessage()));}
        },"ota-check").start();
    }
    void offer(int code,String name,String url,String sha,String notes){
        String message="เวอร์ชันใหม่ Pixel GBA "+name+"\n\n"+(notes.isEmpty()?"มีการปรับปรุงและแก้ไขข้อผิดพลาด":notes)+"\n\nดาวน์โหลดจาก GitHub Releases แล้วติดตั้งทับ โดยเซฟจะยังอยู่";
        new AlertDialog.Builder(activity).setTitle("UPDATE AVAILABLE").setMessage(message).setNegativeButton("LATER",null).setPositiveButton("DOWNLOAD",(d,w)->download(code,name,url,sha)).show();
    }
    void download(int code,String name,String url,String expectedSha){
        try{
            File dir=new File(activity.getCacheDir(),"updates");dir.mkdirs();for(File f:dir.listFiles()==null?new File[0]:dir.listFiles())if(!f.delete())activity.toast("Old update could not be removed");
            activity.toast("Downloading update…");
            new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(true);if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());File apk=new File(dir,"pixelgba-update.apk");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(apk)){byte[] b=new byte[16384];int n;long total=0;while((n=in.read(b))!=-1){total+=n;if(total>100*1024*1024)throw new IOException("APK exceeds 100 MB");out.write(b,0,n);}out.getFD().sync();}if(!expectedSha.isEmpty()&&!expectedSha.equals(sha256(apk)))throw new IOException("APK checksum does not match update manifest");activity.runOnUiThread(()->install(apk));}catch(Exception e){activity.runOnUiThread(()->activity.toast("Update download failed: "+e.getMessage()));}},"ota-download").start();
        }catch(Exception e){activity.toast("Could not start download: "+e.getMessage());}
    }
    void install(File apk) {
        try{
            if(!apk.isFile()||apk.length()<100000)throw new IOException("Downloaded APK is incomplete");
            if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));activity.toast("Allow Pixel GBA to install updates, then tap UPDATE again");return;
            }
            Uri uri=FileProvider.getUriForFile(activity,"dev.pixelgba.fileprovider",apk);
            Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);activity.startActivity(i);
        }catch(Exception e){activity.toast("Update install failed: "+e.getMessage());}
    }
    static byte[] read(InputStream in,int max)throws IOException{try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=x.read(b))!=-1){if(out.size()+n>max)throw new IOException("Update manifest is too large");out.write(b,0,n);}return out.toByteArray();}}
    static String sha256(File file)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
}
