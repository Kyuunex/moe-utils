package moe.kyuunex.moe_utils.utility;

public class RenderWrap {
    private int fadeTime;
    private int breath;

    public RenderWrap(int fadeTime, int breath) {
        this.fadeTime = fadeTime;
        this.breath = breath;
    }

    public int fadeTime() {
        return fadeTime;
    }

    public void fadeTime(int fadeTime) {
        this.fadeTime = fadeTime;
    }

    public int breath() {
        return breath;
    }

    public void breath(int breath) {
        this.breath = breath;
    }
}
