package org.eleanorsilly.mc.notes;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.eleanorsilly.mc.notes.commands.NoteCommand;

public class NoteListener implements Listener {
    private NotePlugin plugin;
    private NoteConfig config;

    // Constructor to link the plugin instance
    public NoteListener(NotePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        NoteCommand commands = new NoteCommand(plugin);
        if (!player.hasPermission("simplenotes.see.self.warns") || config.getConfigBoolean("")) {
            return;
        }
        boolean b = commands.NoteList(player, new String[] {""}, true);
    }
}
