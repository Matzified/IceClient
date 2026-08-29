package net.matzified.iceclient.setting;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String id, String name, String description, double defaultValue, double min, double max, double step) {
        super(id, name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public void increment() {
        setValue(Math.min(max, getValue() + step));
    }

    public void decrement() {
        setValue(Math.max(min, getValue() - step));
    }
}
