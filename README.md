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
- /note remove [player] [id]: remove a note from a player (permission: simplenotes.removenotes)

## Compiling

Clone the repository and run `mvn clean package`. The resulting jar should be in the `target` folder.