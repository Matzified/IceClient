package net.matzified.iceclient.setting;

public class ModeSetting extends Setting<String> {
    private final String[] modes;
    private int index = 0;

    public ModeSetting(String id, String name, String description, String[] modes, String defaultMode) {
        super(id, name, description, defaultMode);
        this.modes = modes;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(defaultMode)) {
                this.index = i;
                break;
            }
        }
    }

    public String[] getModes() { return modes; }
    public int getIndex() { return index; }

    public void cycleNext() {
        index = (index + 1) % modes.length;
        setValue(modes[index]);
    }

    public void cyclePrev() {
        index = (index - 1 + modes.length) % modes.length;
        setValue(modes[index]);
    }
}
