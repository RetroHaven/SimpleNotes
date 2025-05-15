package org.eleanorsilly.mc.notes;

import org.bukkit.event.Listener;

public class TemplateListener implements Listener {
    private NotePlugin plugin;
    private TemplateConfig config;

    // Constructor to link the plugin instance
    public TemplateListener(NotePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
}
