package org.retrohaven.mc.notes;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.retrohaven.mc.notes.commands.NoteCommand;

public class NoteListener extends PlayerListener {
    private NotePlugin plugin;
    private NoteConfig config;

    // Constructor to link the plugin instance
    public NoteListener(NotePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if ((!player.hasPermission("simplenotes.see.self.warns") && !player.isOp() && !config.getConfigBoolean("settings.warns.showonlogin.value"))
                && (!player.hasPermission("simplenotes.see.self.notes") && !player.isOp() && !config.getConfigBoolean("settings.notes.showonlogin.value"))) {
            return;
        }
        NoteCommand commands = new NoteCommand(plugin);
        boolean b = commands.NoteList(player, new String[] {""}, true);
    }
}
