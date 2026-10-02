import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Asynchronous native MP3 playback; no Java audio dependencies. */
final class SoundEffects implements AutoCloseable {
    private Process helper, playing;
    private BufferedWriter commands;
    private String player;
    private final Path assets = Path.of("assets").toAbsolutePath();
    private boolean available;

    SoundEffects() {
        try {
            for (String name : new String[]{"eat", "death"}) {
                if (!Files.isRegularFile(assets.resolve(name + ".mp3")))
                    throw new IOException("Missing assets/" + name + ".mp3");
            }
            String os = System.getProperty("os.name");
            if (os.startsWith("Windows")) {
                helper = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile",
                        "-ExecutionPolicy", "Bypass", "-File",
                        Path.of("scripts", "sounds.ps1").toAbsolutePath().toString(),
                        "-Assets", assets.toString())
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                BufferedReader response = new BufferedReader(new InputStreamReader(helper.getInputStream()));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!response.ready() && helper.isAlive() && System.nanoTime() < deadline)
                    Thread.sleep(20);
                if (!response.ready() || !"READY".equals(response.readLine()))
                    throw new IOException("Windows MP3 player did not initialize");
                commands = new BufferedWriter(new OutputStreamWriter(helper.getOutputStream()));
            } else {
                player = os.startsWith("Mac") ? "afplay" : "ffplay";
                Process probe = new ProcessBuilder("which", player)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                if (probe.waitFor() != 0) throw new IOException("Install " + player + " for sound");
            }
            available = true;
        } catch (IOException | InterruptedException e) {
            close();
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    synchronized boolean available() {
        return available && (helper == null || helper.isAlive());
    }

    synchronized void play(String name) {
        if (!available() || !(name.equals("eat") || name.equals("death"))) return;
        try {
            if (commands != null) {
                commands.write(name); commands.newLine(); commands.flush();
            } else {
                if (playing != null) playing.destroy();
                String file = assets.resolve(name + ".mp3").toString();
                ProcessBuilder launch = player.equals("afplay")
                        ? new ProcessBuilder(player, file)
                        : new ProcessBuilder(player, "-nodisp", "-autoexit", "-loglevel", "quiet", file);
                playing = launch.redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            }
        } catch (IOException e) { available = false; }
    }

    @Override public synchronized void close() {
        available = false;
        if (commands != null) {
            try { commands.write("quit\n"); commands.flush(); commands.close(); }
            catch (IOException ignored) { }
        }
        if (helper != null) {
            try { if (!helper.waitFor(500, TimeUnit.MILLISECONDS)) helper.destroy(); }
            catch (InterruptedException e) { helper.destroy(); Thread.currentThread().interrupt(); }
        }
        if (playing != null) playing.destroy();
    }
}
