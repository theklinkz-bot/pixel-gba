package dev.pixelgba;
import java.io.*;
import java.nio.file.*;
class CheckStorage {
    public static void main(String[] args) throws Exception {
        byte[] valid=new byte[192];valid[178]=(byte)0x96;
        if(Storage.readRom(new ByteArrayInputStream(valid)).length!=192)throw new AssertionError();
        try{Storage.readRom(new ByteArrayInputStream(new byte[192]));throw new AssertionError("accepted invalid ROM");}catch(IOException expected){}
        try{Storage.readRom(new ByteArrayInputStream(new byte[191]));throw new AssertionError("accepted short ROM");}catch(IOException expected){}
        try{Storage.readRom(new InputStream(){long remaining=Storage.MAX_ROM+1L;public int read(){return remaining-->0?0:-1;}public int read(byte[] b,int o,int l){if(remaining<=0)return -1;int n=(int)Math.min(l,remaining);remaining-=n;return n;}});throw new AssertionError("accepted oversized ROM");}catch(IOException expected){}
        if(!Storage.hash(valid).equals(Storage.hash(valid))||Storage.hash(valid).length()!=64)throw new AssertionError();
        File dir=Files.createTempDirectory("pixelgba-check").toFile(),f=new File(dir,"save");Storage.write(f,new byte[]{1});Storage.write(f,new byte[]{2,3});if(Files.readAllBytes(f.toPath())[1]!=3)throw new AssertionError();f.delete();dir.delete();
        System.out.println("PASS: ROM validation, size limit, identity, atomic replacement");
    }
}
