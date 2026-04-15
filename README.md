# SimpleFreeze

SimpleFreeze is a Minecraft Paper/Folia plugin and API that provides quick, simple, and reliable player freezing.

When frozen, a player is unable to do any action, including running commands or using their inventory. When unfrozen 
player data which could have changed (such as velocity or fall distance) is restored, putting the player into the exact
state they were frozen in.

A player can be frozen by multiple different "freeze entries", allowing for multiple plugins/sources to use
player freezing without causing any collisions. Each freeze entry has a Minecraft resource style Key in the form of
"namespace:value" which is used to identify a freeze entry. All SimpleFreeze commands use the `SimpleFreeze:command`
key.

## Usage

### Plugin
Supports Minecraft Paper & Purpur 1.28–26.1

Download the latest version of the plugin from [releases](https://github.com/hboyd2003/SimpleFreeze).

#### Commands

|             Command             | Description                                                                                                           |
|:-------------------------------:|:----------------------------------------------------------------------------------------------------------------------|
|  `/freeze <players> [<title>]`  | Freezes one or more players with an optional MiniMessage title displayed                                              |
|      `/unfreeze <players>`      | Unfreezes one or more players frozen with the freeze command                                                          |
|  `/unfreeze <players> <force>`  | Forcibly unfreezes one or more players removing all freeze entries and reseting their player state to a default state |
|     `/simplefreeze version`     | Displays the plugin version                                                                                           |
|     `/simplefreeze status`      | Shows details on how many players are frozen and by which keys                                                        |
| `/simplefreeze status <player>` | Shows freeze details for a specific frozen player.                                                                    |
|      `/simplefreeze list`       | Lists currently frozen players.                                                                                       |

**Permissions**

|                  Permission Key                   | Description                                          |
|:-------------------------------------------------:|:-----------------------------------------------------|
|                  `simplefreeze`                   | Root SimpleFreeze permission                         |
|              `simplefreeze.command`               | All SimpleFreeze commands                            |
|           `simplefreeze.command.freeze`           | Gives ability to use `/freeze`                       |
|          `simplefreeze.command.unfreeze`          | Gives ability to use `/unfreeze`                     |
|        `simplefreeze.command.simplefreeze`        | Gives ability to use `/simplefreeze`                 |
|    `simplefreeze.command.simplefreeze.version`    | Gives ability to use `/simplefreeze version`         |
|     `simplefreeze.command.simplefreeze.list`      | Gives ability to use `/simplefreeze list`            |
|    `simplefreeze.command.simplefreeze.status`     | Gives ability to use `/simplefreeze status`          |
| `simplefreeze.command.simplefreeze.status.player` | Gives ability to use `/simplefreeze status <player>` |


### Library/API

#### Dependency Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.hboyd.dev/releases/")
    maven("https://repo.hboyd.dev/snapshots/")
}

dependencies {
    compileOnly("dev.hboyd:simplefreeze-api:1.0.0")
}
```

#### Basic Usage

```java
Plugin plugin = (ISimpleFreeze) Bukkit.getPluginManager().getPlugin("SimpleFreeze");

IFreezeManager freezeManager = simpleFreeze.freezeManager();

Key key = Key.key("yourplugin", "purpose/source");

freezeManager.addFreezeEntry(targetOfflinePlayer, key);
freezeManager.removeFreezeEntry(targetOfflinePlayer, key);
```

## License

This project is licensed under the [GNU General Lesser Public License v3.0](LICENSE.md).


