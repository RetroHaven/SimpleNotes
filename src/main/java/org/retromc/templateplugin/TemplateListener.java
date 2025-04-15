package org.retromc.templateplugin;

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

    // Handle player join event
    @EventHandler(priority = Event.Priority.Normal)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Example action: Send a welcome message configured in the plugin's configuration file to the player when they join the server

        String welcomeMessage = config.getConfigString("settings.welcome-message.value");
        welcomeMessage = welcomeMessage.replace("%player%", event.getPlayer().getName());

        event.getPlayer().sendMessage(welcomeMessage);
    }
}
