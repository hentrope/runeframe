# RuneFrame
RuneFrame is a minimalist client for Old School RuneScape written in Java. 

## Features
RuneFrame forgoes having an extensive number of features to focus on doing one thing well: loading the game.

**Speed** - RuneFrame is optimized to load as quickly as possible, decompressing and verifying the JAR as it is loaded into memory. Once a gamepack has been downloaded, it can be cached locally for nearly instant load times.

**Simplicity** - The client's GUI consists of only one single component: the game. By keeping the client simple, the game can be run using less memory and processing power, while reducing the impact on battery life for laptops and tablets.

**Polish** - RuneFrame provides a number of small tweaks that make it more convenient to use than the Jagex's official client:
- Built-in screenshot capture
- Fullscreen mode using Alt + Enter
- Portable mode
- Preserve window size + dimension between executions
- Confirmation prompt when closing client
- Easy to configure homeworld and hardware acceleration

## Releases
Currently, only an executable JAR is available for download, although you are free to wrap it as an .exe file on your own if you wish. It should be compatible with most operating systems, although it has only been significantly tested in Windows thus far.

[Link To Latest Release](https://github.com/hentrope/runeframe/releases)

The legality of the client has not been stated nor confirmed by Jagex, so use at your own risk.

## Arguments
Three different command line arguments can be provided to the client during startup, which change some settings which cannot be offered in the preferences file. All arguments begin with "--", and contain no spaces between each key-value pair. For example, "PORTABLE=false PATH=C:/" will put the client into non-portable mode with all the files stored directly within the C: drive.

`PORTABLE` changes the default location of all directories used by RuneFrame. See the Directories section below for details.
`PATH` changes the location of the user directory. By default, this path will be determined based on whether the client is in portable mode.
`DEBUG` determines where errors are printed. Default is false, but if set to true, errors will be printed to the console instead of to error.log.

## Directories
These are the default locations of all directories used by the client. The user directory can only be changed by providing a different directory to the client as a command-line argument. The other directories are all defined in the preferences file, which is described in the Preferences section below.

**User Directory** - Contains runeframe.pref, runeframe.state, and error.log
**Data Directory** - Contains the cached gamepack, as well as the game's own data files
**Screenshot Directory** - Contains all screenshots taken using the client

**Portable Mode:**
- User Directory = ./ (current working directory)
- Data Directory = ./data/
- Screenshot Directory = ./screenshots/

**Non-Portable Mode:**
- User Directory = $APPDATA/RuneFrame/ (/home/.config/RuneFrame/ on Linux)
- Data Directory = $PROGRAMDATA/.jagex/oldschool (/home/.jagex/oldschool on Linux)
- Screenshot Directory = $HOME/Pictures/RuneFrame

## Preferences
Preferences are stored in "runeframe.pref", which is located in the user directory. Documentation for each setting is included in the preferences file itself when it is automatically created. For reference, the default contents of the two different preferences files are given below.

### Portable
```
# Sets which world the client will attempt use on startup.
home-world=0

# If true, the client will store the gamepack locally for faster startup.
cache-gamepack=true

# If true, the client will save the window's size and position between sessions.
preserve-window-state=true

# Determines which rendering system the Java 2D system should use. If a certain
# system causes graphical issues, you may wish to try switching to another.
# Available options:
#   "Software"
#   "OpenGL"
#   "DirectDraw" (Windows only)
#   "Direct3D" (Windows only)
#   "XRender" (Linux/Solaris only)
graphics-acceleration=Software

# If enabled, borderless fullscreen can be toggled by using Alt + Enter.
fullscreen-enabled=true

# If enabled, a screenshot will automatically be saved when Print Screen is pressed.
screenshot-enabled=true

# Determines how screenshots will be sorted.
#   0 = screenshots will be saved directly into the screenshot folder
#   1 = screenshots will be separated based on what year they were taken
#   2 = screenshots will be separated based on both year and month
screenshot-sort=2

# If true, a sound effect will play when a screenshot is taken.
screenshot-sound=true

# Specifies the directory in which the game's cache and gamepack will be stored.
# Numerous "pseudo-environment variables" can be used at the beginning of the path name:
#   "." = folder in which the RuneFrame client is stored
#   "$HOME" = user profile on Windows, home directory on Unix
#   "$APPDATA" = appdata/roaming on Windows, $home/.config on Unix
#   "$PROGRAMDATA" = ProgramData on Windows, $home on Unix
data-directory=./data

# Specifies the directory in which the game's cache and gamepack will be stored.
# This folder will not be created unless the screenshot feature is enabled.
screenshot-directory=./screenshots
```

### Non-Portable
```
# Sets which world the client will attempt use on startup.
home-world=0

# If true, the client will store the gamepack locally for faster startup.
cache-gamepack=true

# If true, the client will save the window's size and position between sessions.
preserve-window-state=true

# Determines which rendering system the Java 2D system should use. If a certain
# system causes graphical issues, you may wish to try switching to another.
# Available options:
#   "Software"
#   "OpenGL"
#   "DirectDraw" (Windows only)
#   "Direct3D" (Windows only)
#   "XRender" (Linux/Solaris only)
graphics-acceleration=Software

# If enabled, borderless fullscreen can be toggled by using Alt + Enter.
fullscreen-enabled=true

# If enabled, a screenshot will automatically be saved when Print Screen is pressed.
screenshot-enabled=true

# Determines how screenshots will be sorted.
#   0 = screenshots will be saved directly into the screenshot folder
#   1 = screenshots will be separated based on what year they were taken
#   2 = screenshots will be separated based on both year and month
screenshot-sort=2

# If true, a sound effect will play when a screenshot is taken.
screenshot-sound=true

# Specifies the directory in which the game's cache and gamepack will be stored.
# Numerous "pseudo-environment variables" can be used at the beginning of the path name:
#   "." = folder in which the RuneFrame client is stored
#   "$HOME" = user profile on Windows, home directory on Unix
#   "$APPDATA" = appdata/roaming on Windows, $home/.config on Unix
#   "$PROGRAMDATA" = ProgramData on Windows, $home on Unix
data-directory=$PROGRAMDATA/.jagex/oldschool

# Specifies the directory in which the game's cache and gamepack will be stored.
# This folder will not be created unless the screenshot feature is enabled.
screenshot-directory=$HOME/Pictures/RuneFrame
```
