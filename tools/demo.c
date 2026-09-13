// Original CC0 homebrew test cartridge. No commercial game or BIOS data.
typedef unsigned short u16;
typedef unsigned int u32;
#define REG(a) (*(volatile u16*)(a))
#include "demo-background.h"
__attribute__((used)) const char save_type[]="SRAM_V113";
void main(void) {
    volatile u16* video=(volatile u16*)0x06000000;
    volatile u16* state=(volatile u16*)0x02000000;
    REG(0x04000000)=0x0403;
    REG(0x04000084)=0x80;
    REG(0x04000080)=0x2277;
    REG(0x04000082)=2;
    for(int i=0;i<240*160;i++)video[i]=background[i];
    int x=116,y=94; state[0]=0;
    volatile unsigned char* battery=(volatile unsigned char*)0x0e000000;
    state[3]=battery[0];
    while(1){
        while(REG(0x04000006)>=160){}
        while(REG(0x04000006)<160){}
        for(int j=0;j<8;j++)for(int i=0;i<8;i++)video[(y+j)*240+x+i]=background[(y+j)*240+x+i];
        u16 keys=~REG(0x04000130)&1023;
        state[0]++;state[1]=keys;
        if(keys&1){battery[0]=42;state[3]=42;}
        if(keys&16)x++;if(keys&32)x--;if(keys&64)y--;if(keys&128)y++;
        if(x<8)x=8;if(x>224)x=224;if(y<66)y=66;if(y>120)y=120;
        if(keys&0x300){REG(0x04000068)=0xf080;REG(0x0400006c)=(keys&0x100)?0x8700:0x8500;}
        u16 color=keys&1?0x001f:keys&2?0x7c00:0x3fff;
        for(int j=0;j<8;j++)for(int i=0;i<8;i++)video[(y+j)*240+x+i]=((i==1||i==5)&&j==2)?0x1084:color;
        // Exposed writable marker for real cheat tests: 02000004:001F turns this red.
        for(int j=0;j<4;j++)for(int i=0;i<4;i++)video[(70+j)*240+228+i]=state[2];
    }
}
