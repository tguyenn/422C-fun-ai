# ASCII Snake (Java)

A dependency-free terminal game with food, a growing snake, scoring, progressively
faster movement, bonus food, MP3 sound effects, pause, restart, and a session-best score.

## Requirements

- JDK 17 or newer (`java -version` and `javac -version`).
- An interactive ANSI-capable terminal at least 52 columns wide and 24 rows tall.
  On Windows, use Windows Terminal with PowerShell or Command Prompt;
  Windows PowerShell (`powershell.exe`) must be available.
- On Linux/macOS, the standard `stty` command must be available.
- MP3 playback uses Windows' native media API through PowerShell, macOS's
  built-in `afplay`, or `ffplay` (from FFmpeg) on Linux. Without a supported
  player or the sound files, the game remains playable and shows `Sound: unavailable`.

Use a real terminal or an IDE's integrated terminal, rather than its output/debug
console. No Maven, Gradle, or external Java libraries are needed.

## Compile and run

From this directory, run these commands in PowerShell, Command Prompt, or a
Linux/macOS terminal:

```sh
javac -d out src/*.java
java -cp out Snake
```

For a controls reminder: `java -cp out Snake --help`.

## How to play

| Key | Action |
| --- | --- |
| W / A / S / D | Move up / left / down / right |
| P | Pause or resume |
| R | Restart, including during a game |
| Q | Quit |

Keys respond immediately; do not press Enter. Uppercase keys also work.
The snake starts moving right after a one-second delay. `@` is its head, `o` is
its body, and `*` is normal food worth 10 points. Bonus food appears as `$` and
is worth 50 points (5x normal). Each new food has a 20% chance of being a bonus;
one food is on the board at a time. Either type grows the snake by one segment.
Each meal reduces the movement interval by 8 ms, from 150 ms down to a minimum
of 65 ms. Hitting a wall or your body ends the game. You cannot reverse directly
into your neck. Filling the board wins. Best score lasts until you quit.

Eating either food plays the burp in `assets/eat.mp3`; a fatal collision plays
`assets/death.mp3` once. Playback does not stop movement. A new effect replaces
the previous one, and quitting stops playback. Run from the project directory
so the game can find `assets/` and the Windows helper `scripts/sounds.ps1`.

The game uses ASCII characters and ANSI screen controls. It restores the cursor
and the previous screen on normal exit. On Windows, a small PowerShell child
process reads console keys; on Linux/macOS, Java reads keys after `stty` disables
line buffering. If forced termination leaves a Unix terminal without echo,
type `stty sane` and press Enter.

## Run the rules tests

```sh
javac -d out src/*.java test/SnakeGameTest.java
java -cp out SnakeGameTest
```

The source separates the game rules (`SnakeGame`), native terminal input
(`TerminalInput`), sound playback (`SoundEffects`), and the game loop / ASCII renderer (`Snake`).
