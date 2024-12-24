package com.patulus.huffmancoding.compressor;

import java.util.ArrayList;
import java.util.List;

public class CompressorData {
    public static int getTotalChars(Compressor compressor) {
        return compressor.totalChars;
    }

    public static int getUsedChars(Compressor compressor) {
        return compressor.frequency.size();
    }

    public static long getSrcVolume(Compressor compressor) {
        return compressor.src.length();
    }

    public static long getOutVolume(Compressor compressor) {
        return compressor.out.length();
    }

    public static String getFrequency(Compressor compressor) {
        StringBuilder res = new StringBuilder();

        List<Integer> keys = new ArrayList<>(compressor.frequency.keySet());
        keys.sort((o1, o2) -> (compressor.frequency.get(o2) - compressor.frequency.get(o1)));
        for (int i = 0; i < keys.size(); ++i) {
            res.append((char) (keys.get(i) - 0)).append(": ").append(compressor.frequency.get(keys.get(i))).append('\n');
            res.append("부호화 코드: ").append(compressor.huffmanCodes.get((char) (keys.get(i) - 0))).append('\n');
        }

        return res.toString();
    }

    public static double getElapsedTime(Compressor compressor) {
        return compressor.elapsedTime;
    }

    public static String getResult(Compressor compressor) {
        return compressor.getResult();
    }
}
