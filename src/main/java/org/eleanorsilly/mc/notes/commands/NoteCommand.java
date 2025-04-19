package org.eleanorsilly.mc.notes.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.eleanorsilly.mc.notes.TemplateConfig;
import org.eleanorsilly.mc.notes.TemplatePlugin;

public class NoteCommand implements CommandExecutor {

    private final TemplatePlugin plugin;

    private final TemplateConfig config;

    public NoteCommand(TemplatePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean NoteAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simplenotes.addnotes") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this subcommand.");
            return true;
        }
        return false;
    }

    public boolean NoteRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simplenotes.removenotes") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this subcommand.");
            return true;
        }
        return false;
    }

    public boolean NoteList(CommandSender sender, String[] args) {
        return false;
    }

    public boolean NoteHelp(CommandSender sender) {
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if the command is enabled
        Boolean isEnabled = config.getConfigBoolean("settings.test-command.enabled.value");
        if (!isEnabled) {
            sender.sendMessage("This command is currently disabled.");
            return true;
        }
        // todo: logic to call the different functions above
        return true;
    }
}
