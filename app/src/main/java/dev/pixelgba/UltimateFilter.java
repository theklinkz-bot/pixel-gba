package dev.pixelgba;

final class UltimateFilter {
    // Scale2x preserves flat pixels and rounds matching edges without blending colors.
    static void upscale(int[] source, int width, int height, int[] target) {
        for (int y=0;y<height;y++) for (int x=0;x<width;x++) {
            int e=source[y*width+x], b=source[Math.max(0,y-1)*width+x];
            int d=source[y*width+Math.max(0,x-1)], f=source[y*width+Math.min(width-1,x+1)];
            int h=source[Math.min(height-1,y+1)*width+x], out=y*4*width+x*2;
            boolean edge=b!=h && d!=f;
            target[out]=edge && d==b?d:e;
            target[out+1]=edge && b==f?f:e;
            target[out+width*2]=edge && d==h?d:e;
            target[out+width*2+1]=edge && h==f?f:e;
        }
    }
}
