package org.eleanorsilly.mc.notes;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TemplateListener implements Listener {
    private TemplatePlugin plugin;
    private TemplateConfig config;

    // Constructor to link the plugin instance
    public TemplateListener(TemplatePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
}
