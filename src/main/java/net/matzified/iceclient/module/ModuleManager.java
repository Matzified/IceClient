package net.matzified.iceclient.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.matzified.iceclient.module.impl.*;
import net.matzified.iceclient.setting.*;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class ModuleManager {

    private static ModuleManager instance;
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> moduleMap = new HashMap<>();
    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ModuleManager() {
        MinecraftClient mc = MinecraftClient.getInstance();
        File runDir = mc.runDirectory != null ? mc.runDirectory : new File(".");
        File configDir = new File(runDir, "config/iceclient");
        configDir.mkdirs();
        this.configFile = new File(configDir, "modules.json");

        registerAllModules();
        loadConfig();
    }

    public static synchronized ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private void register(Module m) {
        modules.add(m);
        moduleMap.put(m.getId().toLowerCase(), m);
    }

    private void registerAllModules() {
        register(new WatermarkModule());
        register(new FpsModule());
        register(new CpsModule());
        register(new PingModule());
        register(new CoordsModule());
        register(new KeystrokesModule());
        register(new ArmorModule());
        register(new PotionStatusModule());
        register(new MemoryModule());
        register(new SpeedometerModule());
        register(new ReachModule());
        register(new ComboModule());
        register(new ClockModule());
        register(new ServerInfoModule());
        register(new ToggleSprintModule());
        register(new FullbrightModule());
        register(new BiomeModule());
        register(new LightLevelModule());
        register(new DayCounterModule());
        register(new PlayerCountModule());
        register(new ArrowCounterModule());
        register(new TotemCounterModule());
        register(new ItemCountModule());
        register(new TargetHudModule());
        register(new SessionStatsModule());
        register(new BedWarsStatsModule());
        register(new MaceModule());
        register(new AttackCooldownModule());
        register(new WindChargeModule());
        register(new CrystalAnchorModule());
        register(new ShieldStatusModule());
        register(new ArmorWarningModule());
        register(new JumpResetModule());
        register(new BlockOverlayModule());
        register(new CustomCrosshairModule());
        register(new TimeChangerModule());
        register(new CustomFogModule());
        register(new ChatCustomizerModule());
        register(new OverlayModule());
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category cat) {
        if (cat == Category.ALL) return modules;
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == cat) list.add(m);
        }
        return list;
    }

    public Module getModule(String id) {
        return moduleMap.get(id.toLowerCase());
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onTick();
            }
        }
    }

    public void saveConfig() {
        try {
            Map<String, ModuleConfigData> map = new HashMap<>();
            for (Module m : modules) {
                Map<String, Object> settingsMap = new HashMap<>();
                for (Setting<?> s : m.getSettings()) {
                    settingsMap.put(s.getId(), s.getValue());
                }
                map.put(m.getId(), new ModuleConfigData(m.isEnabled(), m.getX(), m.getY(), settingsMap));
            }
            try (FileWriter w = new FileWriter(configFile)) {
                gson.toJson(map, w);
            }
        } catch (Exception e) {
            System.err.println("[IceClient] Failed to save modules config: " + e.getMessage());
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) return;
        try (FileReader r = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, ModuleConfigData>>(){}.getType();
            Map<String, ModuleConfigData> map = gson.fromJson(r, type);
            if (map != null) {
                for (Map.Entry<String, ModuleConfigData> entry : map.entrySet()) {
                    Module m = moduleMap.get(entry.getKey().toLowerCase());
                    if (m != null) {
                        ModuleConfigData data = entry.getValue();
                        m.setEnabled(data.enabled);
                        m.setX(data.x);
                        m.setY(data.y);
                        if (data.settings != null) {
                            for (Map.Entry<String, Object> setEntry : data.settings.entrySet()) {
                                Setting<?> s = m.getSetting(setEntry.getKey());
                                if (s instanceof BooleanSetting && setEntry.getValue() instanceof Boolean) {
                                    ((BooleanSetting) s).setValue((Boolean) setEntry.getValue());
                                } else if (s instanceof ColorSetting && setEntry.getValue() instanceof Number) {
                                    ((ColorSetting) s).setValue(((Number) setEntry.getValue()).intValue());
                                } else if (s instanceof ModeSetting && setEntry.getValue() instanceof String) {
                                    ((ModeSetting) s).setValue((String) setEntry.getValue());
                                } else if (s instanceof NumberSetting && setEntry.getValue() instanceof Number) {
                                    ((NumberSetting) s).setValue(((Number) setEntry.getValue()).doubleValue());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[IceClient] Failed to load modules config: " + e.getMessage());
        }
    }

    private static class ModuleConfigData {
        boolean enabled;
        int x;
        int y;
        Map<String, Object> settings;

        public ModuleConfigData(boolean enabled, int x, int y, Map<String, Object> settings) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.settings = settings;
        }
    }
}
