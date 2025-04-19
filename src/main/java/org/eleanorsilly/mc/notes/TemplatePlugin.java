package org.eleanorsilly.mc.notes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.eleanorsilly.mc.notes.commands.NoteCommand;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TemplatePlugin extends JavaPlugin {
    private JavaPlugin plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;

    private TemplateConfig configuration;

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());

        // Load configuration
        configuration = new TemplateConfig(this, new File(getDataFolder(), "config.yml")); // Load the configuration file from the plugin's data folder

        // Register the command and the aliases
        getCommand("note").setExecutor(new NoteCommand(this));
        getCommand("notes").setExecutor(new NoteCommand(this));
        getCommand("warn").setExecutor(new NoteCommand(this));
        getCommand("warning").setExecutor(new NoteCommand(this));
        getCommand("warnings").setExecutor(new NoteCommand(this));

        // Register the listeners
        TemplateListener listener = new TemplateListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        log.info("[" + pluginName + "] Is Loaded, Version: " + pdf.getVersion());
    }

    @Override
    public void onDisable() {
        log.info("[" + pluginName + "] Is Unloading, Version: " + pdf.getVersion());

        // Save configuration
        //config.save(); // Save the configuration file to disk. This should only be necessary if the configuration cam be modified during runtime.

        log.info("[" + pluginName + "] Is Unloaded, Version: " + pdf.getVersion());
    }

    public void logger(Level level, String message) {
        Bukkit.getLogger().log(level, "[" + plugin.getDescription().getName() + "] " + message);
    }

    public TemplateConfig getConfig() {
        return configuration;
    }

    public boolean WriteToFile(String filename, String content) {
        return false
    }
}
