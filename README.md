# SimpleNotes

This plugin aims to provide a note/warning system implemented easily for beta 1.7.3. No other Minecraft versions are targeted and no support is provided for those other versions.

## Commands

The plugin uses one command, /note (with aliases /notes, /warning and /warnings).

### Subcommands

- /note help: list the subcommands you have access to
- /note list: list your own notes/warnings (permission: simplenotes.seenotes.self)
- /note list <player>: list other player's notes/warnings (permission: simplenotes.seenotes.others)
- /note add <player> <type (note/warn)> <content>: add a note to a player (permission: simplenotes.addnotes)
- /note remove <player> <id>: remove a note from a player (permission: simplenotes.removenotes)

## Compiling

Clone the repository and run `mvn clean package`. The resulting jar should be in the `target` folder.
