# SimpleNotes

This plugin aims to provide a note/warning system implemented easily for beta 1.7.3. No other Minecraft versions are targeted and no support is provided for those other versions.

This plugin currently assumes all players are paid accounts. Adding offline players support isn't planned for now.

Tested with Project Poseidon. Plain Bukkit should work since this project doesn't use any of Poseidon's APIs.

## Commands

The plugin uses one command, /note (with aliases /notes, /warn, /warns, /warning and /warnings).

This command is disabled by default to make you look through the config (it's located in plugins/SimpleNotes/config.yml!).

### Subcommands

- /note help: list the subcommands you have access to
- /note list: list your own notes/warnings (permission: simplenotes.see.self.notes/warns)
- /note list [player]: list another player's notes/warnings (permission: simplenotes.see.others.notes/warns)
- /note add [player] [content]: add a note to a player (permission: simplenotes.addnotes)
- /warn add [player] [content]: add a warn to a player (permission: simplenotes.addnotes)
  - This is the only case where the alias matter. You can use any of the warn* aliases for this.
- /note append [player] [id] [content]: append to a note. 
- /note remove [player] [id]: remove a note from a player (permission: simplenotes.removenotes)

## Permissions

- simplenotes.addnotes: add notes/warns (and append to them)
- simplenotes.removenotes: remove notes/warns
- simplenotes.see.self.notes: see your own notes
- simplenotes.see.self.warns: see your own warns
- simplenotes.see.others.notes: see other people's notes
- simplenotes.see.others.warns: see other people's warns

Permissions were tested with PermissionsEx, version 1.14. Notation with simplenotes.* and derivatives should work.

## Compiling

Clone the repository and run `mvn clean package`. The resulting jar should be in the `target` folder.

## Breaking changes through versions

None.

## Licensing

The repository was started as a fork of the sample plugin repo from RetroMC, available at https://github.com/retromcorg/Poseidon-Plugin-Template. This forking is licensed under the MIT license, see LICENSE-retromc.

All additional modifications are licensed under the RetroHaven Development Group, see LICENSE. If you are forking or taking code from this project, it is advised to provide both files.