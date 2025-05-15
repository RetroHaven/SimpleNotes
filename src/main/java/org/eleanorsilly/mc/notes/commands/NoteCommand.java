package org.eleanorsilly.mc.notes.commands;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.eleanorsilly.mc.notes.TemplateConfig;
import org.eleanorsilly.mc.notes.NotePlugin;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.Bukkit.getPlayer;

public class NoteCommand implements CommandExecutor {

    private final NotePlugin plugin;

    private final TemplateConfig config;

    public NoteCommand(NotePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean CheckFileExists(File file) {
        if (file.exists()) {
            return true;
        } else {
            // we create the folders above
            List<File> parentList = Arrays.asList(file.getParentFile());
            while (parentList.get(parentList.size() - 1).exists()) {
                parentList.add(parentList.get(parentList.size() - 1).getParentFile());
            }
            for (int i = parentList.size()-1; i > -1; i--) {
                // create all parents, going through the list in reverse
                try {
                    parentList.get(i).mkdirs();
                } catch (Exception e) {
                    // probably a permission issue, failed to create the dirs
                    return false;
                }
            }
            // then we return false, now that all parents exist.
            return false;
        }
    }

    public Object getUUIDFromName(String playerName) {
        try {
            String urlString = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            JSONObject jsonobj = new JSONObject(content.toString());
            if (jsonobj.has("errorMessage")) {
                // couldn't find a profile with this name
                return null;
            }
            return jsonobj.getString("id");
        } catch (Exception e) {
            // probably network error. nothing we can do.
            // unlikely to happen realistically though
            return null;
        }
    }

    public boolean NoteAdd(CommandSender sender, String[] args, String type) {
        if (!sender.hasPermission("simplenotes.addnotes") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this subcommand.");
            return false;
        }
        if (args.length == 1) {
            sender.sendMessage("Please provide a player name.");
            return false;
        }
        String requestSubject = args[1];
        Object subjectUUID = this.getUUIDFromName(requestSubject);
        if (subjectUUID == null) {
            sender.sendMessage("Player doesn't exist.");
            return false;
        }

        String filename = plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv";

        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
                System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to create "+dataFile.getAbsolutePath());
                System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
                return false;
            }
        }
        dataFile.delete();

        // the following is copied from opencsv's documentation
        CSVReader reader = null;
        try {
            reader = new CSVReaderBuilder(new FileReader(filename)).build();
        } catch (FileNotFoundException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        String [] nextLine;
        while (true) {
            try {
                if ((nextLine = reader.readNext()) == null) {
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CsvValidationException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (nextLine == null) {
            nextLine[0] = "0";
        }
        Integer id = Integer.valueOf(nextLine[0])+1; // we use the last ID + 1
        String id_str = id.toString();
        type = type.substring(0,3);
        char separator = '°';
        String author;
        if (sender instanceof Player) {
            author = sender.getName();
        } else {
            author = "CONSOLE";
        }
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename),
                    separator,
                    '\"',
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeNext(new String[] {id_str, type, author, String.join(" ", Arrays.copyOfRange(args, 2, args.length))});
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        return true;
    }

    public boolean NoteRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simplenotes.removenotes") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to use this subcommand.");
            return false;
        }
        if (args.length <= 1) {
            sender.sendMessage("Please provide a player name and an id.");
            return false;
        }
        String requestSubject = args[0];
        Object subjectUUID = this.getUUIDFromName(requestSubject);
        if (subjectUUID == null) {
            sender.sendMessage("Player doesn't exist.");
            return false;
        }
        File dataFile = new File(plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv");
        if (!this.CheckFileExists(dataFile)) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
                System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to create "+dataFile.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    public boolean NoteList(CommandSender sender, String[] args) {
        String RequestSubject;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You need to provide a player name when running this in the console.");
                return false;
            }
            RequestSubject = sender.getName();
        }
        else RequestSubject = args[1];
        if (RequestSubject.equals(sender.getName()) && !sender.hasPermission("simplenotes.see.self.notes") && !sender.hasPermission("simplenotes.see.self.warns") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to check your own notes/warns.");
            return false;
        }
        if (!RequestSubject.equals(sender.getName()) && !sender.hasPermission("simplenotes.see.others.notes") && !sender.hasPermission("simplenotes.see.others.warns") && !sender.isOp()) {
            sender.sendMessage("You do not have permission to check other people's notes/warns.");
            return false;
        }

        OfflinePlayer subject = getPlayer(RequestSubject);
        Object uuid = this.getUUIDFromName(RequestSubject);
        if (uuid == null) {
            sender.sendMessage("Player doesn't exist.");
            return false;
        }

        String playerDataFolder = plugin.getDataFolder().getAbsolutePath()+File.separator+"data";
        return true;
    }

    public boolean NoteHelp(CommandSender sender) {
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if the command is enabled
        Boolean isEnabled = config.getConfigBoolean("settings.test-command.enabled.value");
        if (!isEnabled) {
            sender.sendMessage("This command is currently disabled.");
            return true;
        }
        if (args.length == 0 || args[0] == "help" ) {
            return this.NoteHelp(sender);
        } else if (args[0] == "add") {
            // label is needed because of the differentiation between notes and warnings
            return this.NoteAdd(sender, args, label);
        } else if (args[0] == "remove") {
            return this.NoteRemove(sender, args);
        } else if (args[0] == "list") {
            return this.NoteList(sender, args);
        } else {
            sender.sendMessage("Unrecognized argument.");
            return this.NoteHelp(sender);
        }
    }
}
