package com.ludwici.anchorui.api.layout;

import java.util.ArrayList;
import java.util.List;

public final class NineSlice {
    public record Insets(int left, int top, int right, int bottom) {
        public Insets {
            if (left < 0 || top < 0 || right < 0 || bottom < 0) {
                throw new IllegalArgumentException("Nine-slice margins cannot be negative");
            }
        }
    }

    public record Tile(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
    }

    private NineSlice() {
    }

    public static List<Tile> tiles(int destWidth, int destHeight, int sourceWidth, int sourceHeight, Insets insets) {
        if (destWidth < 0 || destHeight < 0 || sourceWidth <= 0 || sourceHeight <= 0
                || insets.left() + insets.right() >= sourceWidth || insets.top() + insets.bottom() >= sourceHeight) {
            throw new IllegalArgumentException("Nine-slice requires a positive source center and nonnegative destination");
        }
        int destLeft = border(insets.left(), insets.right(), destWidth);
        int destRight = destWidth - Math.min(destWidth, destLeft + Math.max(0, destWidth - insets.left() - insets.right()));
        if (destWidth >= insets.left() + insets.right()) {
            destRight = insets.right();
        } else {
            destRight = destWidth - destLeft;
        }
        int destTop = border(insets.top(), insets.bottom(), destHeight);
        int destBottom = destHeight >= insets.top() + insets.bottom() ? insets.bottom() : destHeight - destTop;
        int[] sx = {0, insets.left(), sourceWidth - insets.right()};
        int[] sy = {0, insets.top(), sourceHeight - insets.bottom()};
        int[] sw = {insets.left(), sourceWidth - insets.left() - insets.right(), insets.right()};
        int[] sh = {insets.top(), sourceHeight - insets.top() - insets.bottom(), insets.bottom()};
        int[] dx = {0, destLeft, destWidth - destRight};
        int[] dy = {0, destTop, destHeight - destBottom};
        int[] dw = {destLeft, destWidth - destLeft - destRight, destRight};
        int[] dh = {destTop, destHeight - destTop - destBottom, destBottom};
        List<Tile> result = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dw[col] > 0 && dh[row] > 0 && sw[col] > 0 && sh[row] > 0) {
                    result.add(new Tile(dx[col], dy[row], dw[col], dh[row], sx[col], sy[row], sw[col], sh[row]));
                }
            }
        }
        return List.copyOf(result);
    }

    private static int border(int first, int last, int total) {
        if (total >= first + last) {
            return first;
        }
        if (first + last == 0) {
            return 0;
        }
        return (int) ((long) total * first / (first + last));
    }
}
