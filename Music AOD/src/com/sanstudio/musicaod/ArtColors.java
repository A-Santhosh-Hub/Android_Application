package com.sanstudio.musicaod;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Picks two vivid colours from album art, bright enough to read on a black screen. */
final class ArtColors {
    private static final int SIZE = 32;
    private static final int BUCKETS = 12; // 30 degrees of hue each

    private ArtColors() {
    }

    /** Returns {main, accent}, or null when the art has no colourful pixels. */
    static int[] extract(Bitmap src) {
        Bitmap small;
        try {
            Bitmap b = src.getConfig() == Bitmap.Config.HARDWARE ? src.copy(Bitmap.Config.ARGB_8888, false) : src;
            small = Bitmap.createScaledBitmap(b, SIZE, SIZE, true);
        } catch (RuntimeException e) {
            return null;
        }
        int[] px = new int[SIZE * SIZE];
        small.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE);

        float[] weight = new float[BUCKETS];
        float[][] sum = new float[BUCKETS][3];
        float[] hsv = new float[3];
        for (int p : px) {
            Color.colorToHSV(p, hsv);
            if (hsv[1] < 0.25f || hsv[2] < 0.25f) continue; // skip greys, whites and near-black
            int bucket = (int) (hsv[0] / 30f) % BUCKETS;
            float w = hsv[1] * hsv[2];
            weight[bucket] += w;
            sum[bucket][0] += hsv[0] * w;
            sum[bucket][1] += hsv[1] * w;
            sum[bucket][2] += hsv[2] * w;
        }

        int best = 0;
        for (int i = 1; i < BUCKETS; i++) if (weight[i] > weight[best]) best = i;
        if (weight[best] == 0) return null;

        int second = -1;
        for (int i = 0; i < BUCKETS; i++) {
            int d = Math.abs(i - best);
            if (Math.min(d, BUCKETS - d) < 2) continue; // must be a clearly different hue
            if (second < 0 || weight[i] > weight[second]) second = i;
        }

        int main = vivid(sum[best], weight[best]);
        int accent = second >= 0 && weight[second] > weight[best] * 0.15f
                ? vivid(sum[second], weight[second])
                : shiftHue(main, 50);
        return new int[]{main, accent};
    }

    private static int vivid(float[] s, float w) {
        return Color.HSVToColor(new float[]{s[0] / w, Math.max(0.55f, s[1] / w), Math.max(0.9f, s[2] / w)});
    }

    private static int shiftHue(int color, float degrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + degrees) % 360f;
        return Color.HSVToColor(hsv);
    }
}
