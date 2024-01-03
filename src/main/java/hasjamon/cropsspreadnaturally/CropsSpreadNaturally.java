package hasjamon.cropsspreadnaturally;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CropsSpreadNaturally extends JavaPlugin implements Listener, CommandExecutor {
    final Set<String> allDirections = Arrays.stream(BlockFace.values()).map(BlockFace::toString).collect(Collectors.toSet());
    final Set<Material> airTypes = Set.of(
            Material.AIR,
            Material.VOID_AIR,
            Material.CAVE_AIR
    );

    @Override
    public void onEnable() {
        // Do... something that's required to make config work properly
        getConfig().options().copyDefaults(true);
        // Save config to default.yml
        saveConfigAsDefault();
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        List<String> directionsToSpread = this.getConfig().getStringList("directions-to-spread");
        ConfigurationSection spreadOnReachAge = this.getConfig().getConfigurationSection("spread-on-reach-age." + blockType.name());
        Optional<ConfigurationSection> agesToSpreadOpt = Optional.ofNullable(spreadOnReachAge);

        agesToSpreadOpt.ifPresent(agesToSpread -> {
                    if (event.getNewState().getBlockData() instanceof Ageable ageData) {
                        int chanceToSpread = agesToSpread.getInt(Integer.toString(ageData.getAge()));

                        directionsToSpread.stream()
                                .filter(allDirections::contains)
                                .map(BlockFace::valueOf)
                                .map(block::getRelative)
                                .filter(b -> airTypes.contains(b.getType()) && b.getRelative(BlockFace.DOWN).getType() == Material.FARMLAND)
                                .forEach(b -> {
                                    if (ThreadLocalRandom.current().nextInt(0, 100) < chanceToSpread)
                                        b.setType(blockType);
                                });
                    }
                }
        );
    }

    // Saves the default config; always overwrites. This file is purely for ease of reference; it is never loaded.
    private void saveConfigAsDefault() {
        if (!this.getDataFolder().exists())
            if (!this.getDataFolder().mkdir())
                Bukkit.getConsoleSender().sendMessage("Failed to create data folder.");

        File defaultFile = new File(this.getDataFolder(), "default.yml");
        InputStream cfgStream = this.getResource("config.yml");

        if (cfgStream != null) {
            try {
                Files.copy(cfgStream, defaultFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to save default.yml");
            }
        }
    }

}