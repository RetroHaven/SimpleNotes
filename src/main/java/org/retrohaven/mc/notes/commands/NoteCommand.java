package org.retrohaven.mc.notes.commands;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.retrohaven.mc.notes.NoteConfig;
import org.retrohaven.mc.notes.NotePlugin;
import org.json.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class NoteCommand implements CommandExecutor {

    private final NotePlugin plugin;
    private final NoteConfig config;
    private final String errorColorCode = "§e";
    private final String permissionColorCode = "§3";

    public NoteCommand(NotePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean CheckFileExists(File file) {
        if (file.exists()) {
            return true;
        } else {
            // we create the folders above
            List<File> parentListInit = Arrays.asList(file.getParentFile());
            List<File> parentList = new ArrayList<File>(parentListInit);
            while (parentList.get(parentList.size() - 1).getParentFile() != null && !parentList.get(parentList.size() - 1).getParentFile().exists()) {
                parentList.add(parentList.get(parentList.size() - 1).getParentFile());
            }
            for (int i = parentList.size()-1; i > -1; i--) {
                // create all parents, going through the list in reverse
                try {
                    boolean b = parentList.get(i).mkdirs();
                    if (!b) {
                        throw new RuntimeException();
                    }
                } catch (Exception e) {
                    // probably a permission issue, failed to create the dirs
                    return false;
                }
            }
            // then we return false, now that all parents exist.
            return false;
        }
    }

    // caching for UUIDs, to prevent unnecessary requests to the API
    private final Cache<String, Object> uuidCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.DAYS)
                .build();
    public Object getUUIDFromName(String playerName) {
        try {
            return uuidCache.get(playerName, () -> {
                try {
                    String urlString = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
                    URL url = URI.create(urlString).toURL();
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
            });
        } catch (ExecutionException e) {
            return null;
        }
    }

    public boolean NoteAdd(CommandSender sender, String[] args, String type) {
        if (!sender.hasPermission("simplenotes.addnotes") && !sender.isOp()) {
            sender.sendMessage(permissionColorCode+"You do not have permission to use this subcommand.");
            return false;
        }
        if (args.length == 1) {
            sender.sendMessage(errorColorCode+"Please provide a player name.");
            return false;
        }
        String requestSubject = args[1];
        Object subjectUUID = this.getUUIDFromName(requestSubject);
        if (subjectUUID == null) {
            sender.sendMessage(errorColorCode+"Player doesn't exist.");
            return false;
        }
        subjectUUID = subjectUUID.toString();

        String filename = plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv";
        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            try {
                if (!dataFile.createNewFile()) {
                    throw new IOException("this didn't work");
                }
            } catch (IOException e) {
                sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
                this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to create "+dataFile.getAbsolutePath());
                this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
                return false;
            }
        }

        // the following is copied from opencsv's documentation
        CSVReader reader = null;
        try {
            reader = new CSVReaderBuilder(new FileReader(filename)).build();
        } catch (FileNotFoundException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        String [] nextLine;
        int i = 1;
        try {
            while (true) {
                if ((nextLine = reader.readNext()) != null) {
                    i = Integer.parseInt(nextLine[0]) + 1;
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }

        Integer id = i; // we use the last ID + 1
        String id_str = id.toString();
        type = type.substring(0,4).toUpperCase(); // we keep the first 4 char, so that we have note or warn

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = dateFormat.format(date);

        String author;
        if (sender instanceof Player) {
            author = sender.getName();
        } else {
            author = "CONSOLE";
        }
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename, true),
                    CSVWriter.DEFAULT_SEPARATOR,
                    '\"',
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            String[] line = {
                    id_str,
                    type,
                    dateStr,
                    author,
                    String.join(" ",
                            Arrays.copyOfRange(args, 2, args.length)
                    )
            };
            writer.writeNext(line, true);
            writer.close();
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        // success!
        sender.sendMessage(type.charAt(0) + type.substring(1).toLowerCase()+" added.");
        return true;
    }

    public boolean NoteRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("simplenotes.removenotes") && !sender.isOp()) {
            sender.sendMessage(permissionColorCode+"You do not have permission to use this subcommand.");
            return false;
        }
        if (args.length <= 2) {
            sender.sendMessage(errorColorCode+"Please provide a player name and an id.");
            return true;
        }

        String requestSubject = args[1];
        Object subjectUUID = this.getUUIDFromName(requestSubject);
        if (subjectUUID == null) {
            sender.sendMessage(errorColorCode+"Player doesn't exist.");
            return true;
        }
        subjectUUID = subjectUUID.toString();

        String filename = plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv";
        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            // we do not try to create the file, since if it doesn't exist, the warn/note doesn't exist either
            sender.sendMessage(errorColorCode+"Note/warn not found.");
            return false;
        }

        // We dump the entire CSV file into allElements
        CSVReader reader = null;
        try {
            reader = new CSVReaderBuilder(new FileReader(filename))
                    .withCSVParser(new CSVParserBuilder().build())
                    .build();
        } catch (FileNotFoundException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        List<String[]> allElements = null;
        try {
            allElements = reader.readAll();
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }

        /*
        we delete the file, since we recreate the contents entirely later.
        this is the recommended way to remove a specific line in a CSV file
         */
        try {
            if (!dataFile.delete() || !dataFile.createNewFile()) {
                // something went wrong. we do not do data loss here
                // so everything is printed to console
                this.plugin.logger(Level.INFO,"Content of "+filename+":");
                this.plugin.logger(Level.INFO,allElements.stream().map(Arrays::toString).reduce((a, b) -> a + "\n" + b).orElse(""));
                throw new IOException("this didn't work");
            }
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        int rowNumber = -1;
        for (int i = 0; i < allElements.size(); i++) {
            // we iterate through the list, and check the id of each row
            if (Integer.parseInt(allElements.get(i)[0]) == Integer.parseInt(args[2])) {
                rowNumber = i;
                break;
            }
        }
        if (rowNumber == -1) {
            sender.sendMessage(errorColorCode+"Note/warn not found.");
            return false;
        }
        allElements.remove(rowNumber);

        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(filename),
                    CSVWriter.DEFAULT_SEPARATOR,
                    '\"',
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        writer.writeAll(allElements);
        try {
            writer.close();
        } catch (IOException e) {
            sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        // success!
        sender.sendMessage("Note/warn removed.");
        return true;
    }

    public boolean NoteList(CommandSender sender, String[] args, boolean isListener) {
        String RequestSubject;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You need to provide a player name when running this in the console.");
                return true;
            }
            RequestSubject = sender.getName();
        }
        else RequestSubject = args[1];
        if (RequestSubject.equals(sender.getName()) && !sender.hasPermission("simplenotes.see.self.notes") && !sender.hasPermission("simplenotes.see.self.warns") && !sender.isOp()) {
            if (!isListener) sender.sendMessage(permissionColorCode+"You do not have permission to check your own notes/warns.");
            return true;
        }
        if (!RequestSubject.equals(sender.getName()) && !sender.hasPermission("simplenotes.see.others.notes") && !sender.hasPermission("simplenotes.see.others.warns") && !sender.isOp()) {
            if (!isListener) sender.sendMessage(permissionColorCode+"You do not have permission to check other people's notes/warns.");
            return true;
        }

        Object subjectUUID = this.getUUIDFromName(RequestSubject);
        if (subjectUUID == null) {
            if (!isListener) sender.sendMessage(errorColorCode+"Player doesn't exist.");
            return true;
        }
        subjectUUID = subjectUUID.toString();

        String filename = plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv";
        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            // we do not try to create the file, since if it doesn't exist, there are no warns/notes
            if (!isListener) sender.sendMessage(errorColorCode+"No notes or warns to show.");
            return true;
        }

        CSVReader reader = null;
        try {
            reader = new CSVReaderBuilder(new FileReader(filename))
                    .withCSVParser(new CSVParserBuilder().build())
                    .build();
        } catch (FileNotFoundException e) {
            if (!isListener) sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        List<String[]> allElements = null;
        try {
            allElements = reader.readAll();
        } catch (IOException e) {
            if (!isListener) sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            if (!isListener) sender.sendMessage(errorColorCode+"Internal error. Ask your local sys-admin to check the console.");
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            this.plugin.logger(Level.SEVERE,"["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }

        int shownCounter = 0;
        for (String[] line : allElements) {
            // some transformation first
            if (Objects.equals(line[1], "NOTE")) line[1] = "§eNOTE";
            if (Objects.equals(line[1], "WARN")) line[1] = "§cWARN";
            line[2] = "§9"+line[2];

            if (!RequestSubject.equals(sender.getName()) && (sender.hasPermission("simplenotes.see.others.notes") || sender.isOp()) && Objects.equals(line[1].substring(line[1].length() -4), "NOTE")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("§8| " + String.join(" §8|§f ", line));
            } else if (RequestSubject.equals(sender.getName()) && (sender.hasPermission("simplenotes.see.self.notes") || sender.isOp()) && Objects.equals(line[1].substring(line[1].length() -4), "NOTE") && (!isListener || config.getConfigBoolean("settings.notes.showonlogin.value"))) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("§8| " + String.join(" §8|§f ", line));
            } else if (!RequestSubject.equals(sender.getName()) && (sender.hasPermission("simplenotes.see.others.warns") || sender.isOp()) && Objects.equals(line[1].substring(line[1].length() -4), "WARN")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("§8| " + String.join(" §8|§f ", line));
            } else if (RequestSubject.equals(sender.getName()) && (sender.hasPermission("simplenotes.see.self.warns") || sender.isOp()) && Objects.equals(line[1].substring(line[1].length() -4), "WARN") && (!isListener || config.getConfigBoolean("settings.warns.showonlogin.value"))) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("§8| " + String.join(" §8|§f ", line));
            }
        }
        if (shownCounter == 0) {
            sender.sendMessage("No notes or warns to show.");
        }
        return true;
    }

    // the following should be used in all cases.
    // the only reason we have isListener is because of the NoteListener
    public boolean NoteList(CommandSender sender, String[] args) {
        return NoteList(sender, args, false);
    }

    public boolean NoteHelp(CommandSender sender, String alias) {
        int removedCommands = 4;
        sender.sendMessage("=== Command list ===");
        sender.sendMessage("- /"+alias+" help: show this");
        if ((sender.hasPermission("simplenotes.see.self.notes") && sender.hasPermission("simplenotes.see.self.warns")) || sender.isOp()) {
            sender.sendMessage("- /"+alias+" list: show your notes/warns");
            removedCommands = removedCommands - 1;
        } else if (sender.hasPermission("simplenotes.see.self.notes")) {
            sender.sendMessage("- /"+alias+" list: show your notes");
            removedCommands = removedCommands - 1;
        } else if (sender.hasPermission("simplenotes.see.self.warns")) {
            sender.sendMessage("- /"+alias+" list: show your warns");
            removedCommands = removedCommands - 1;
        }
        if (sender.hasPermission("simplenotes.see.others.notes") || sender.hasPermission("simplenotes.see.others.warns") || sender.isOp()) {
            sender.sendMessage("- /"+alias+" list <player>: show another player's notes");
            removedCommands = removedCommands - 1;
        }
        if (sender.hasPermission("simplenotes.addnotes") || sender.isOp()) {
            sender.sendMessage("- /"+alias+" add <player> <content>: add a "+alias.substring(0, 4));
            removedCommands = removedCommands - 1;
        }
        if (sender.hasPermission("simplenotes.removenotes") || sender.isOp()) {
            sender.sendMessage("- /"+alias+" remove <player> <id>: remove a note/warn (you can get the id via /"+alias+" list!)");
            removedCommands = removedCommands - 1;
        }
        if (removedCommands > 0) {
            sender.sendMessage(permissionColorCode+"You do not have access to any other subcommand.");
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command is enabled
        Boolean isEnabled = config.getConfigBoolean("settings.plugin.enabled.value");
        if (!isEnabled) {
            sender.sendMessage("This command is currently disabled. Please check the config.");
            return true;
        }
        // objects.equals is used to shut the IDE. it's not gonna be null let's be realistic.
        if (args.length == 0 || Objects.equals(args[0], "help")) {
            return this.NoteHelp(sender, label);
        } else if (Objects.equals(args[0], "add")) {
            // label is needed because of the differentiation between notes and warnings
            return this.NoteAdd(sender, args, label);
        } else if (Objects.equals(args[0], "remove")) {
            return this.NoteRemove(sender, args);
        } else if (Objects.equals(args[0], "list")) {
            return this.NoteList(sender, args);
        } else {
            sender.sendMessage(errorColorCode+"Unrecognized argument.");
            return this.NoteHelp(sender, label);
        }
    }
}
