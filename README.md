# Pixel GBA

แอป Android GBA emulator ธีม pixel สร้างด้วย Java + Android native UI + mGBA 0.10.5 ผ่าน JNI

## ติดตั้งและลองเล่น

1. ดาวน์โหลด APK รุ่นล่าสุดจาก GitHub Release แล้วเปิดไฟล์บน Android 8.0 ขึ้นไป (อนุญาตติดตั้งจากแอป Files เมื่อ Android ถาม)
2. เปิด **Pixel GBA** → **PIXEL LAB** เพื่อเล่น homebrew ที่เขียนให้โปรเจกต์นี้: D-pad เลื่อนตัวละคร, A/B เปลี่ยนสี, L/R เล่นเสียง
3. ใช้ **IMPORT CARTRIDGE** เลือก `.gba` หรือ ZIP ที่มี `.gba` เพียงเกมเดียว เกมที่นำเข้าเก็บในพื้นที่ส่วนตัวของแอป ไม่ต้องให้สิทธิ์เข้าถึงไฟล์ทั้งเครื่อง
4. ระหว่างเล่นกด **MENU** เพื่อเซฟ ใส่สูตร เปลี่ยนภาพ หรือแก้ปุ่ม

ไม่มี ROM เกมเชิงพาณิชย์หรือ BIOS ของ Nintendo รวมอยู่ในแอป

## OTA ผ่าน GitHub Releases

รุ่นที่ติดตั้งจะมีปุ่ม **CHECK FOR UPDATES** ในคลังเกม แอปอ่าน `update.json` จาก branch `main` ตรวจ `versionCode` แล้วดาวน์โหลด APK จาก GitHub Releases ผ่าน HTTPS ก่อนเปิดตัวติดตั้ง Android ให้ผู้ใช้กดยืนยันเอง เซฟและการตั้งค่าอยู่ในพื้นที่ข้อมูลแอปจึงไม่ถูกลบ การติดตั้งทับต้องใช้ใบรับรองเดียวกันทุกครั้ง

ก่อนปล่อย OTA ครั้งแรก ให้สร้าง keystore ถาวรเก็บไว้นอก repository แล้วเพิ่ม GitHub Actions secrets เหล่านี้: `PIXEL_GBA_KEYSTORE_B64`, `PIXEL_GBA_KEYSTORE_PASSWORD`, `PIXEL_GBA_KEY_ALIAS`, `PIXEL_GBA_KEY_PASSWORD` จากนั้น push tag เช่น `v1.3.0` workflow จะ build signed APK และแนบชื่อ `PixelGBA-1.3.0.apk` ใน Release ห้ามเปลี่ยน keystore หลังแจก APK ให้ผู้ใช้แล้ว

หลังปล่อยรุ่นใหม่ แก้ `update.json` ใน branch `main` ให้ `versionCode`, `versionName`, `apkUrl` และ `changelog` ตรงกับ Release ล่าสุด แล้ว commit ขึ้น GitHub ผู้ใช้จะเห็นอัปเดตเมื่อกดตรวจ OTA ครั้งถัดไป การให้ผู้ใช้ติดตั้ง APK รุ่น debug แล้วพยายาม OTA เป็น release จะถูก Android ปฏิเสธเพราะใบรับรองต่างกัน; แจก debug ใช้สำหรับทดสอบเท่านั้น

### สำรอง APK ขึ้น Google Drive อัตโนมัติ

โฟลเดอร์ `Pixel GBA Releases` ใน Google Drive ใช้เก็บ APK ทุกครั้งที่สร้าง GitHub Release สำหรับ My Drive ส่วนตัวต้องใช้ OAuth ของบัญชี Google (Service Account ไม่มี storage quota สำหรับ My Drive) จากนั้นเพิ่ม GitHub Actions secrets สี่ตัว:

- `GOOGLE_DRIVE_CLIENT_ID`
- `GOOGLE_DRIVE_CLIENT_SECRET`
- `GOOGLE_DRIVE_REFRESH_TOKEN`
- `GOOGLE_DRIVE_FOLDER_ID` = ID ของโฟลเดอร์ปลายทาง

ห้าม commit JSON key ลง repository เมื่อเพิ่ม secrets แล้ว release tag ถัดไปจะอัปโหลดไฟล์ชื่อเดียวกับ APK ไปยังโฟลเดอร์นี้อัตโนมัติ

## อัปเดต 1.3.1

- แนวตั้ง: ตัวเครื่องเรซินใสพร้อมลายวงจร เลือกสีแดง เหลือง น้ำเงิน เขียว ดำ ฟ้า ขาวได้ใน MENU → GRAPHICS & AUDIO; แนวนอนคงหน้าตาเดิม
- ไอคอนมุมฉากที่มุมขวาจอเกม: แนวตั้งมี 2 ระดับ (ปกติ / ขยาย); แนวนอนมี 8 ระดับไล่ต่อเนื่องจนเต็มจอ โดยระดับสุดท้ายใช้ปุ่มโปร่งใสทับภาพและซ่อนแถบระบบ
- แถบเร่งเกม 1.5×, 2×, 4×, 8×, 16×; กดค่าที่เลือกอีกครั้งกลับ 1×
- มีเสียงระหว่างเร่งเกม เสียงเร็วและสูงขึ้นตามเกม; ความเร็วที่ทำได้จริงขึ้นกับเครื่องและเกม
- จำสี ขนาดภาพ และตำแหน่งปุ่มแยกแนวตั้ง/แนวนอน; layout ที่ผู้ใช้จัดเองยังคงอยู่ ใช้ RESET POSITIONS หากต้องการตำแหน่งใหม่ตามภาพตัวอย่าง

## ฟีเจอร์

- จำลอง GBA จริงด้วย mGBA; ภาพ 240×160 และเสียง stereo
- สูตรแยกตามเกม: Auto detect, CodeBreaker, GameShark, Action Replay, VBA raw; เปิด/ปิด/ลบ; ตรวจไวยากรณ์และให้ core ยืนยัน
- ภาพ Pixel / Smooth / CRT scanlines, integer scaling, ลดความจัดของสี, รักษาสัดส่วน 3:2
- ลากปุ่มทุกปุ่ม ปรับขนาด/ความโปร่งใส บันทึกตำแหน่งแยกแนวตั้งและแนวนอน และ reset layout
- Multitouch; D-pad ค่าเริ่มต้นรองรับการลากทแยงด้วยนิ้วเดียว (เมื่อจัดปุ่มแยกเอง ใช้สองปุ่มพร้อมกัน)
- เซฟด่วน 3 ช่อง, auto state เมื่อพักแอป/กลับคลัง, battery save ทุก 15 วินาทีและเมื่อพักแอป
- แถบไอคอนบนหน้าเล่น เลือกความเร็ว 1.5× / 2× / 4× / 8× / 16× พร้อมเสียง กดซ้ำกลับ 1×
- จอย D-pad / left stick / A B L R Start Select; keyboard ลูกศร / Z X / A S / Enter Space
- ส่งออก ZIP เซฟ และนำเข้า battery `.sav`; ROM เดียวกันระบุด้วย SHA-256 เพื่อไม่สลับเซฟกับเกมชื่อเหมือนกัน
- ปุ่มบนจอมี accessibility nodes: แตะเพื่อกดสั้น, long click เพื่อกดค้าง/ปล่อย
- เล่นเกมออฟไลน์ ไม่มีโฆษณาหรือ analytics; ใช้ Internet สำหรับตรวจและดาวน์โหลด OTA

### สูตรสำหรับทดสอบ

เปิด Pixel Lab → MENU → CHEATS → ชนิด **VBA raw**, ชื่อ `Red marker`, รหัส:

```
02000004:001F
```

กด ADD & ENABLE → DONE สี่เหลี่ยมมุมขวาของฉากจะเป็นสีแดง ยืนยันว่ารหัสมีผลกับหน่วยความจำจริง สูตรสำหรับเกมอื่นต้องตรงรุ่น ROM และต้องเลือกชนิดรหัสให้ถูกต้อง การปิดสูตรหยุดการเขียนซ้ำ แต่ค่าที่เปลี่ยนไปแล้วอาจคงอยู่จนเริ่มเกมใหม่

### เซฟและสำรองข้อมูล

**BACKUP SAVES** ส่งออกไฟล์ `.sav`, `.auto`, `.slot1–3` ของเกมปัจจุบันไว้ใน ZIP แตก ZIP แล้วเลือกไฟล์ `.sav` ผ่าน **IMPORT .SAV** เพื่อเริ่มเกมจาก battery save เดิม (รองรับ 512 B, 8/32/64/128 KiB) ไฟล์ state ผูกกับแกน mGBA และไม่ได้รับประกันว่าใช้กับ emulator อื่นได้

การถอนแอปจะลบข้อมูลส่วนตัวของแอป ควรส่งออกเซฟก่อน ไม่มี cloud sync หรือ link-cable multiplayer ในรุ่นนี้

## แหล่งฟีเจอร์ที่ศึกษา

ศึกษาเมื่อ 13 กันยายน 2026:

| Emulator / แหล่งทางการ | สิ่งที่นำมาปรับใช้ |
|---|---|
| [mGBA](https://github.com/mgba-emu/mgba/tree/0.10.5) | แกนจำลอง, cheats, save states, battery save |
| [RetroArch / mGBA core](https://docs.libretro.com/library/mgba/) | แยก battery/state, scaling, controls และ core options |
| [Pizza Boy A Pro](https://play.google.com/store/apps/details?id=it.dbtecno.pizzaboygbapro) | ปรับ layout ปุ่ม, ปรับภาพ, เร่งความเร็ว และใช้จอย |

เลือกทำฟีเจอร์สำหรับเล่นบนเครื่องเดียวก่อน จึงไม่ได้ใส่ rewind, achievements, link multiplayer และ cloud sync

## Build

เปิดโฟลเดอร์นี้ด้วย Android Studio หรือติดตั้ง JDK 17, Gradle 8.10.2, SDK 35, NDK 27.2.12479018 และ CMake 3.22.1

บนเครื่องที่เตรียมให้แล้ว:

```powershell
./tools/build.ps1
# เมื่อมี Android device/emulator ต่อกับ adb:
./tools/build.ps1 -Test
```

บนเครื่องอื่น ตั้ง `sdk.dir` ใน `local.properties` แล้วใช้ Gradle 8.10.2 รัน `gradle :app:assembleDebug :app:lintDebug` ROM Pixel Lab ที่สร้างไว้รวมใน assets แล้ว; source ที่ `tools/demo.c` และ `tools/make-demo.py` ใช้ NDK LLVM สร้างใหม่ได้

APK นี้เป็น debug-signed สำหรับติดตั้งทดสอบ รองรับ arm64-v8a, armeabi-v7a และ x86_64; native library รองรับ page alignment 16 KB

## Tests

- `tools/check-storage.java`: ROM header, truncated/oversized ROM, identity, atomic file replacement
- `CoreTest`: CPU/video, simultaneous keys and release, invalid/valid cheats and actual RAM effect, state restore, SRAM persistence after closing and reopening
- `UiTest`: touch dispatch, default diagonal D-pad, cancel/release, drag layout persistence, drawing all graphics filters, accessibility node, state checkpoint and worker stop/start
- `:app:lintDebug`: Android static analysis

ดูผลทดสอบและข้อจำกัดจริงใน `dist/TEST-REPORT.md` ไม่ได้ยืนยันความเข้ากันได้ของเกมเชิงพาณิชย์ทุกเกมหรือ latency ของจอย Bluetooth บนมือถือจริง

## Source and licenses

โค้ดแอปใหม่เผยแพร่ MIT (`LICENSE`); Pixel Lab homebrew / graphics CC0. mGBA ใช้ MPL 2.0 ที่ commit `26b7884bc25a5933960f3cdcd98bac1ae14d42e2`; source อยู่ `vendor/mgba` โดยไม่ได้แก้ไขไฟล์ upstream. blip_buf เป็น LGPL 2.1-or-later; inih เป็น BSD. ข้อความ license รวมใน APK assets และใน source bundle เพื่อให้ rebuild/relink กับ library ที่แก้ไขได้ ดู `THIRD-PARTY-NOTICES.md`
