package org.eleanorsilly.mc.notes.commands;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.eleanorsilly.mc.notes.NoteConfig;
import org.eleanorsilly.mc.notes.NotePlugin;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class NoteCommand implements CommandExecutor {

    private final NotePlugin plugin;

    private final NoteConfig config;

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

    public Object getUUIDFromName(String playerName) {
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
    }

    public boolean NoteAdd(CommandSender sender, String[] args, String type) {
        // done. not checked for bugs
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
        subjectUUID = subjectUUID.toString();

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
        int i = 0;
        while (true) {
            try {
                i = i + 1;
                if ((nextLine = reader.readNext()) == null) {
                    break;
                }
            } catch (IOException e) {
                sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
                System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
                System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
                return false;
            } catch (CsvValidationException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            reader.close();
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }

        Integer id = i+1; // we use the last ID + 1
        String id_str = id.toString();
        type = type.substring(0,3).toUpperCase(); // we keep the first 4 char, so that we have note or warn
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = dateFormat.format(date);

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
            writer.writeNext(new String[] {id_str, type, dateStr, author, String.join(" ", Arrays.copyOfRange(args, 2, args.length))});
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        // success!
        sender.sendMessage(type.substring(0, 1).toUpperCase() + type.substring(1)+" added.");
        return true;
    }

    public boolean NoteRemove(CommandSender sender, String[] args) {
        // done. not checked for bugs
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
        subjectUUID = subjectUUID.toString();
        String filename = "plugin.getDataFolder()"+File.separator+"data"+File.separator+subjectUUID+".csv";
        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            // we do not try to create the file, since if it doesn't exist, the warn/note doesn't exist either
            sender.sendMessage("Note/warn not found.");
            return false;
        }

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        List<String[]> allElements = null;
        try {
            allElements = reader.readAll();
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        try {
            // we delete the file, since we recreate the contents entirely later.
            dataFile.delete();
            dataFile.createNewFile();
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
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
            sender.sendMessage("Note/warn not found.");
            return false;
        }
        allElements.remove(rowNumber);

        FileWriter sw = null;
        try {
            sw = new FileWriter(dataFile);
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        CSVWriter writer = new CSVWriter(sw);
        writer.writeAll(allElements);
        try {
            writer.close();
        } catch (IOException e) {
            sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        // success!
        sender.sendMessage("Note/warn removed.");
        return true;
    }

    public boolean NoteList(CommandSender sender, String[] args, boolean dontShowErrorMessage) {
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
            if (!dontShowErrorMessage) sender.sendMessage("You do not have permission to check your own notes/warns.");
            return false;
        }
        if (!RequestSubject.equals(sender.getName()) && !sender.hasPermission("simplenotes.see.others.notes") && !sender.hasPermission("simplenotes.see.others.warns") && !sender.isOp()) {
            if (!dontShowErrorMessage) sender.sendMessage("You do not have permission to check other people's notes/warns.");
            return false;
        }

        Object subjectUUID = this.getUUIDFromName(RequestSubject);
        if (subjectUUID == null) {
            if (!dontShowErrorMessage) sender.sendMessage("Player doesn't exist.");
            return false;
        }
        subjectUUID = subjectUUID.toString();

        String filename = plugin.getDataFolder()+File.separator+"data"+File.separator+subjectUUID+".csv";
        File dataFile = new File(filename);
        if (!this.CheckFileExists(dataFile)) {
            // we do not try to create the file, since if it doesn't exist, there are no warns/notes
            if (!dontShowErrorMessage) sender.sendMessage("No notes or warns to show.");
            return false;
        }

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            if (!dontShowErrorMessage) sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }
        List<String[]> allElements = null;
        try {
            allElements = reader.readAll();
        } catch (IOException e) {
            if (!dontShowErrorMessage) sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            if (!dontShowErrorMessage) sender.sendMessage("Internal error. Ask your local sys-admin to check the console.");
            System.out.println("["+this.plugin.getDescription().getName()+"] ERROR: Failed to access "+filename);
            System.out.println("["+this.plugin.getDescription().getName()+"] Check the permissions of the folders.");
            return false;
        }

        int shownCounter = 0;
        for (int i = 0; i < allElements.size(); i++) {
            if (!RequestSubject.equals(sender.getName()) && sender.hasPermission("simplenotes.see.others.notes") && Objects.equals(allElements.get(i)[1], "NOTE")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("| "+String.join(" | ", allElements.get(i)));
            } else if (RequestSubject.equals(sender.getName()) && sender.hasPermission("simplenotes.see.self.notes") && Objects.equals(allElements.get(i)[1], "NOTE")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("| "+String.join(" | ", allElements.get(i)));
            } else if (!RequestSubject.equals(sender.getName()) && sender.hasPermission("simplenotes.see.others.warns") && Objects.equals(allElements.get(i)[1], "WARN")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("| "+String.join(" | ", allElements.get(i)));
            } else if (RequestSubject.equals(sender.getName()) && sender.hasPermission("simplenotes.see.self.warns") && Objects.equals(allElements.get(i)[1], "WARN")) {
                shownCounter = shownCounter + 1;
                sender.sendMessage("| "+String.join(" | ", allElements.get(i)));
            }
        }
        if (shownCounter == 1) {
            sender.sendMessage("No notes or warnings to show.");
        }
        return true;
    }

    // the following should be used in all cases.
    // the only reason we have dontShowErrorMessage is because of the NoteListener
    public boolean NoteList(CommandSender sender, String[] args) {
        return NoteList(sender, args, false);
    }

    public boolean NoteHelp(CommandSender sender) {
        int removedCommands = 0;
        sender.sendMessage("=== Command list ===");
        sender.sendMessage("- /note help: show this");
        if (sender.hasPermission("simplenotes.see.self.notes") || sender.hasPermission("simplenotes.see.self.warns") || sender.isOp()) {
            sender.sendMessage("- /note list: show your notes");
            removedCommands = removedCommands + 1;
        }
        if (sender.hasPermission("simplenotes.see.others.notes") || sender.hasPermission("simplenotes.see.others.warns") || sender.isOp()) {
            sender.sendMessage("- /note list <player>: show another player's notes");
            removedCommands = removedCommands + 1;
        }
        if (sender.hasPermission("simplenotes.addnotes") || sender.isOp()) {
            sender.sendMessage("- /note add <player> <content>: add a note");
            removedCommands = removedCommands + 1;
        }
        if (sender.hasPermission("simplenotes.removenotes") || sender.isOp()) {
            sender.sendMessage("- /note remove <id>: remove a note (you can get the id via /note list!)");
            removedCommands = removedCommands + 1;
        }
        if (removedCommands > 0) {
            sender.sendMessage("You do not have access to any other subcommand.");
        }
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
        // objects.equals is used to shut the IDE. it's not gonna be null let's be realistic.
        if (args.length == 0 || Objects.equals(args[0], "help")) {
            return this.NoteHelp(sender);
        } else if (Objects.equals(args[0], "add")) {
            // label is needed because of the differentiation between notes and warnings
            return this.NoteAdd(sender, args, label);
        } else if (Objects.equals(args[0], "remove")) {
            return this.NoteRemove(sender, args);
        } else if (Objects.equals(args[0], "list")) {
            return this.NoteList(sender, args);
        } else {
            sender.sendMessage("Unrecognized argument.");
            return this.NoteHelp(sender);
        }
    }
}
