package net.matzified.iceclient.setting;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    protected T value;
    protected final T defaultValue;

    public Setting(String id, String name, String description, T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }
}
