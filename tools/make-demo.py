"""Build the original Pixel Lab GBA ROM using the Android NDK's LLVM tools."""
from pathlib import Path
import subprocess
import struct

ROOT = Path(__file__).resolve().parents[1]
LLVM = ROOT / '.tools/android/ndk/27.2.12479018/toolchains/llvm/prebuilt/windows-x86_64/bin'
OUT = ROOT / '.tools/demo'
OUT.mkdir(parents=True, exist_ok=True)
pixels = [0] * (240 * 160)
def color(r,g,b): return (r>>3) | ((g>>3)<<5) | ((b>>3)<<10)
def rect(x,y,w,h,c):
    for j in range(max(0,y),min(160,y+h)):
        for i in range(max(0,x),min(240,x+w)): pixels[j*240+i]=c
font={
 'A':['01110','10001','11111','10001','10001'], 'B':['11110','10001','11110','10001','11110'],
 'C':['01111','10000','10000','10000','01111'], 'D':['11110','10001','10001','10001','11110'],
 'E':['11111','10000','11110','10000','11111'], 'F':['11111','10000','11110','10000','10000'],
 'G':['01111','10000','10111','10001','01111'], 'H':['10001','10001','11111','10001','10001'],
 'I':['11111','00100','00100','00100','11111'], 'K':['10001','10010','11100','10010','10001'],
 'L':['10000','10000','10000','10000','11111'], 'M':['10001','11011','10101','10001','10001'],
 'N':['10001','11001','10101','10011','10001'], 'O':['01110','10001','10001','10001','01110'],
 'P':['11110','10001','11110','10000','10000'], 'R':['11110','10001','11110','10010','10001'],
 'S':['01111','10000','01110','00001','11110'], 'T':['11111','00100','00100','00100','00100'],
 'U':['10001','10001','10001','10001','01110'], 'V':['10001','10001','10001','01010','00100'],
 'W':['10001','10001','10101','11011','10001'], 'X':['10001','01010','00100','01010','10001'],
 'Y':['10001','01010','00100','00100','00100'], '/':['00001','00010','00100','01000','10000'],
 '-':['00000','00000','11111','00000','00000'], ' ':['00000']*5,
}
def text(s,y,scale,c):
    x=(240-len(s)*6*scale+scale)//2
    for ch in s:
        for j,row in enumerate(font[ch]):
            for i,v in enumerate(row):
                if v=='1':rect(x+i*scale,y+j*scale,scale,scale,c)
        x+=6*scale
navy=color(17,28,37); lime=color(200,240,120);muted=color(153,173,174)
rect(0,0,240,160,navy)
for x,y in [(12,64),(38,81),(198,66),(224,91),(73,72),(172,79)]:rect(x,y,2,2,lime)
for x in range(0,240,16):rect(x,118-(x*7%37),16,45,color(28,49,54))
rect(0,128,240,2,lime)
text('PIXEL LAB',16,3,lime)
text('A HOMEBREW PLAYGROUND',42,1,muted)
text('D-PAD MOVE / A B COLOR',138,1,lime)
text('L R SOUND',149,1,muted)
(OUT/'demo-background.h').write_text('static const unsigned short background[38400]={'+','.join(map(str,pixels))+'};')
clang=str(LLVM/'clang.exe')
subprocess.run([clang,'--target=arm-none-eabi','-mcpu=arm7tdmi','-marm','-O2','-ffreestanding','-fno-builtin','-nostdlib','-I'+str(OUT),'-Wl,-T,'+str(ROOT/'tools/demo.ld'),str(ROOT/'tools/demo-start.s'),str(ROOT/'tools/demo.c'),'-o',str(OUT/'demo.elf')],check=True)
subprocess.run([str(LLVM/'llvm-objcopy.exe'),'-O','binary',str(OUT/'demo.elf'),str(OUT/'demo.gba')],check=True)
rom=bytearray((OUT/'demo.gba').read_bytes())
rom[0xa0:0xac]=b'PIXEL LAB   '
rom[0xac:0xb0]=b'PXLB';rom[0xb0:0xb2]=b'00';rom[0xb2]=0x96
rom[0xbd]=(-sum(rom[0xa0:0xbd])-0x19)&255
assets=ROOT/'app/src/main/assets';assets.mkdir(parents=True,exist_ok=True)
(assets/'pixel-lab.gba').write_bytes(rom)
print('Original Pixel Lab ROM:',len(rom),'bytes')
