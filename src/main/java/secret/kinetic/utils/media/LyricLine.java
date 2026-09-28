package secret.kinetic.utils.media;









public class LyricLine {

    public static final long MAX_DURATION_MILLIS = 8000L;
    public static final long LAST_DURATION_MILLIS = 5000L;
    private static final float SPACE_WEIGHT = 0.35f;

    private final long timeMillis;
    private final String text;
    
    private final long[] wordTimes;
    
    private final int[] wordOffsets;
    private final float totalWeight;
    private long endMillis;

    public LyricLine(long timeMillis, String text) {
        this(timeMillis, text, null, null);
    }

    public LyricLine(long timeMillis, String text, long[] wordTimes, int[] wordOffsets) {
        this.timeMillis = timeMillis;
        this.text = text;
        boolean tagged = wordTimes != null && wordOffsets != null && wordTimes.length > 0 && wordTimes.length == wordOffsets.length;
        this.wordTimes = tagged ? wordTimes : null;
        this.wordOffsets = tagged ? wordOffsets : null;
        this.endMillis = timeMillis + LAST_DURATION_MILLIS;

        float weight = 0f;
        for (int i = 0; i < text.length(); i++) weight += weightOf(text.charAt(i));
        this.totalWeight = weight;
    }

    public long getTimeMillis() { return timeMillis; }
    public String getText() { return text; }
    public long getEndMillis() { return endMillis; }
    public boolean hasWordTimes() { return wordTimes != null; }

    
    public void setEndMillis(long endMillis) {
        this.endMillis = Math.max(timeMillis + 1L, endMillis);
    }

    public long getDurationMillis() {
        return endMillis - timeMillis;
    }

    
    public float getProgress(long position) {
        long duration = getDurationMillis();
        if (duration <= 0L) return position >= timeMillis ? 1f : 0f;
        return clamp((position - timeMillis) / (float) duration);
    }

    



    public float getCharProgress(long position) {
        if (text.isEmpty()) return 0f;
        if (wordTimes != null) return wordCharProgress(position);
        return weightedCharProgress(getProgress(position));
    }

    private float weightedCharProgress(float progress) {
        if (progress <= 0f) return 0f;
        if (progress >= 1f || totalWeight <= 0f) return text.length();

        float target = progress * totalWeight;
        float accumulated = 0f;
        for (int i = 0; i < text.length(); i++) {
            float weight = weightOf(text.charAt(i));
            if (accumulated + weight >= target) {
                return i + (weight > 0f ? (target - accumulated) / weight : 1f);
            }
            accumulated += weight;
        }
        return text.length();
    }

    private float wordCharProgress(long position) {
        if (position < wordTimes[0]) return 0f;
        if (position >= endMillis) return text.length();

        int word = wordTimes.length - 1;
        for (int i = 0; i < wordTimes.length; i++) {
            if (wordTimes[i] > position) {
                word = i - 1;
                break;
            }
        }
        if (word < 0) return 0f;

        long start = wordTimes[word];
        long end = word + 1 < wordTimes.length ? wordTimes[word + 1] : endMillis;
        int from = Math.min(text.length(), Math.max(0, wordOffsets[word]));
        int to = word + 1 < wordOffsets.length ? Math.min(text.length(), Math.max(from, wordOffsets[word + 1])) : text.length();
        float fraction = end > start ? clamp((position - start) / (float) (end - start)) : 1f;
        return from + fraction * (to - from);
    }

    private static float weightOf(char c) {
        return Character.isWhitespace(c) ? SPACE_WEIGHT : 1f;
    }

    private static float clamp(float value) {
        return value < 0f ? 0f : value > 1f ? 1f : value;
    }
}
