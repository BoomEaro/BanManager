package me.confuser.banmanager.common.configs;

import lombok.Value;
import me.confuser.banmanager.common.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class AdvancedCooldownsConfig {

    private final Map<String, Map<String, GroupCooldown>> cooldowns = new HashMap<>();

    public AdvancedCooldownsConfig(ConfigurationSection config) {
        if (config == null) {
            return;
        }

        for (String command : config.getKeys(false)) {
            Map<String, GroupCooldown> groupCooldown = new HashMap<>();
            ConfigurationSection groupSection = config.getConfigurationSection(command);
            if (groupSection != null) {
                for (String group : groupSection.getKeys(false)) {
                    int threshold = groupSection.getInt(group + ".threshold");
                    int time = groupSection.getInt(group + ".time");
                    int cooldown = groupSection.getInt(group + ".cooldown");
                    groupCooldown.put(group, new GroupCooldown(group, new Cooldown(threshold, time, cooldown)));
                }
            }

            this.cooldowns.put(command, groupCooldown);
        }
    }

    public GroupCooldown getCommand(String name, String group) {
        return cooldowns.getOrDefault(name, new HashMap<>()).getOrDefault(group, new GroupCooldown("default", new Cooldown(0, 0, 0)));
    }

    @Value
    public static class GroupCooldown {
        String group;
        Cooldown cooldown;
    }

    @Value
    public static class Cooldown {
        int threshold;
        int time;
        int cooldown;
    }
}
