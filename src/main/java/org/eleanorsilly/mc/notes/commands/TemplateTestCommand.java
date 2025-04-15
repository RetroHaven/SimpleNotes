package org.retromc.templateplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.retromc.templateplugin.TemplateConfig;
import org.retromc.templateplugin.TemplatePlugin;

public class TemplateTestCommand implements CommandExecutor {

    private final TemplatePlugin plugin;

    private final TemplateConfig config;

    public TemplateTestCommand(TemplatePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        if (!sender.hasPermission("myplugin.testcommand") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to execute this command.");
            return true;
        }

        // Check if the command is enabled
        Boolean isEnabled = config.getConfigBoolean("settings.test-command.enabled.value");
        if (!isEnabled) {
            sender.sendMessage("This command is currently disabled.");
            return true;
        }

        // Get the response message from the config
        String response = config.getConfigString("settings.test-command.response.value");

        // Send the response message to the player
        sender.sendMessage(response);
        return true;
    }
}
