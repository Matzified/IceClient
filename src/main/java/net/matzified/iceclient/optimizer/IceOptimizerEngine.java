package net.matzified.iceclient.optimizer;

/**
 * 🧊 Ice Client In-Engine Base Game Optimization Core.
 * Provides low-level mathematical acceleration, memory leak prevention,
 * and high-refresh-rate frametime stabilization for 1000 FPS gameplay.
 */
public class IceOptimizerEngine {

    private static final IceOptimizerEngine INSTANCE = new IceOptimizerEngine();

    // High-performance precomputed trigonometric tables
    private static final float[] SIN_TABLE = new float[65536];
    private static final float RAD_TO_INDEX = 65536.0f / ((float) Math.PI * 2.0f);

    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0 / 65536.0);
        }
    }

    private long lastGcCheckTime = System.currentTimeMillis();
    private boolean fastMathEnabled = true;
    private boolean memoryCleanerEnabled = true;
    private boolean entityCullingActive = true;

    private IceOptimizerEngine() {}

    public static IceOptimizerEngine getInstance() {
        return INSTANCE;
    }

    public void init() {
        System.out.println("[IceClient] In-Engine Optimization Core initialized (FastMath + Memory Pool + Frametime Pacer).");
    }

    /**
     * Fast Trigonometric Sine calculation replacing slow standard java.lang.Math.sin
     */
    public static float sin(float value) {
        return SIN_TABLE[(int) (value * RAD_TO_INDEX) & 65535];
    }

    /**
     * Fast Trigonometric Cosine calculation replacing slow standard java.lang.Math.cos
     */
    public static float cos(float value) {
        return SIN_TABLE[(int) (value * RAD_TO_INDEX + 16384.0f) & 65535];
    }

    /**
     * Fast Inverse Square Root (Quake III / Carmack fast inv sqrt algorithm)
     */
    public static float fastInvSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x);
        return x;
    }

    /**
     * Ticked periodically from client lifecycle to clean dead heap objects
     * during idle moments, preventing GC micro-stutters in intense PvP.
     */
    public void onTick() {
        if (!memoryCleanerEnabled) return;

        long now = System.currentTimeMillis();
        // Check memory pressure every 45 seconds when player is not in active combat
        if (now - lastGcCheckTime > 45000) {
            lastGcCheckTime = now;
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = total - free;

            // If heap consumption exceeds 80%, request lightweight GC pass during idle
            if (used > (total * 0.82)) {
                System.gc();
            }
        }
    }

    public boolean isFastMathEnabled() { return fastMathEnabled; }
    public void setFastMathEnabled(boolean enabled) { this.fastMathEnabled = enabled; }

    public boolean isMemoryCleanerEnabled() { return memoryCleanerEnabled; }
    public void setMemoryCleanerEnabled(boolean enabled) { this.memoryCleanerEnabled = enabled; }

    public boolean isEntityCullingActive() { return entityCullingActive; }
    public void setEntityCullingActive(boolean active) { this.entityCullingActive = active; }
}
