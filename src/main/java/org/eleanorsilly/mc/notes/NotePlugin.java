package org.eleanorsilly.mc.notes;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.eleanorsilly.mc.notes.commands.NoteCommand;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotePlugin extends JavaPlugin {
    private JavaPlugin plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;

    private NoteConfig configuration;

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] is loading, version: " + pdf.getVersion());

        // Load configuration
        configuration = new NoteConfig(this, new File(getDataFolder(), "config.yml")); // Load the configuration file from the plugin's data folder

        // Register the command and the aliases
        getCommand("note").setExecutor(new NoteCommand(this));

        // Register the listeners
        if (configuration.getConfigBoolean("settings.warns.showonlogin.value") || configuration.getConfigBoolean("settings.notes.showonlogin.value")) {
            NoteListener listener = new NoteListener(this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Event.Priority.Monitor, this);
        }

        log.info("[" + pluginName + "] Plugin loaded!");
    }

    @Override
    public void onDisable() {
        // Save configuration
        //config.save(); // Save the configuration file to disk. This should only be necessary if the configuration cam be modified during runtime.

        log.info("[" + pluginName + "] Plugin unloaded!");
    }

    public void logger(Level level, String message) {
        Bukkit.getLogger().log(level, "[" + plugin.getDescription().getName() + "] " + message);
    }

    public NoteConfig getConfig() {
        return configuration;
    }
}
