package demo;

/** A deliberately tiny business rule so the only variable is test discovery. */
public final class Price {

    private Price() {
    }

    /** The base revision has an off-by-one: the boundary should qualify. */
    public static boolean qualifies(int cents) {
        return cents > 10000;
    }
}
