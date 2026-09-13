package dev.pixelgba;

import java.io.*;
import java.nio.file.*;
import java.security.*;

final class Storage {
    static final int MAX_ROM = 32 * 1024 * 1024;
    static byte[] readRom(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384]; int n;
        while ((n = in.read(buf)) != -1) {
            if (out.size() + n > MAX_ROM) throw new IOException("ROM exceeds 32 MB");
            out.write(buf, 0, n);
        }
        byte[] data = out.toByteArray();
        if (data.length < 192 || data[0xb2] != (byte)0x96) throw new IOException("This is not a valid .gba ROM");
        return data;
    }
    static String hash(byte[] data) {
        try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(data); StringBuilder s = new StringBuilder(); for (byte b : digest) s.append(String.format("%02x", b & 255)); return s.toString(); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static void write(File file, byte[] bytes) throws IOException {
        File tmp = new File(file + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(bytes); out.getFD().sync(); }
        replace(tmp, file);
    }
    static void replace(File from, File to) throws IOException {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
