package com.patulus.huffmancoding.decompressor;

import com.patulus.huffmancoding.compressor.Compressor;

public class DecompressorData {
    public static int getTotalChars(Decompressor decompressor) {
        return decompressor.totalChars;
    }

    public static long getSrcVolume(Decompressor decompressor) {
        return decompressor.src.length();
    }

    public static long getOutVolume(Decompressor decompressor) {
        return decompressor.out.length();
    }

    public static double getElapsedTime(Decompressor decompressor) {
        return decompressor.elapsedTime;
    }

    public static String getResult(Decompressor decompressor) {
        return decompressor.getResult();
    }
}
