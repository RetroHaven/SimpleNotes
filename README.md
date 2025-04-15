# Poseidon-Plugin-Template

This repository serves as a template to assist with creating plugins for Project Poseidon.

It includes examples of:
- A configuration file.
- A listener.
- A command.

## Steps to Use This Template

1. **Clone the Repository**
    - Clone this repository to your local machine.

2. **Modify `pom.xml`**
    - Update the following fields to reflect your plugin:
        - `name`
        - `version`
        - `description`
    - **Note:** Removing `-SNAPSHOT` from the version will trigger the `release.yml` GitHub Action to create a GitHub release.

3. **Refactor Package Structure**
    - Refactor the package `org.retromc.templateplugin` to a unique package name for your plugin to avoid conflicts.

4. **Update `plugin.yml`**
    - Update the `plugin.yml` file to match the refactored package name and plugin metadata.

5. **Modify the Code**
    - Customize the code as required for your plugin.
    - **Important:**
        - Remove the player greeting example in the listener.
        - Remove the test command.

## GitHub Actions

This repository includes a pre-configured GitHub Action:

1. **`build-and-test.yml`**:
    - Runs tests on every push to ensure code quality.
    - Uploads an artifact for each commit, allowing others to download the plugin for testing.

2. **`release.yml`**:
    - Automatically creates a GitHub release if the `-SNAPSHOT` suffix is removed from the version in `pom.xml`.

With this template, you can kickstart your plugin development for Project Poseidon quickly and efficiently.
