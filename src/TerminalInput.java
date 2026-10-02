import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Native console keys without third-party Java libraries. */
final class TerminalInput implements AutoCloseable {
    private final ConcurrentLinkedQueue<Integer> keys = new ConcurrentLinkedQueue<>();
    private Process reader;
    private String savedMode;
    private volatile boolean closed, ended;

    TerminalInput() throws IOException, InterruptedException {
        if (System.console() == null)
            throw new IOException("Use an interactive terminal, not an IDE output pane or redirected input.");
        Runnable read;
        if (System.getProperty("os.name").startsWith("Windows")) {
            String script = "$ErrorActionPreference='Stop'; while ($true) { "
                    + "$k=[Console]::ReadKey($true); [Console]::Out.WriteLine([int][char]$k.KeyChar); "
                    + "[Console]::Out.Flush() }";
            reader = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-Command", script)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            read = () -> {
                try (var lines = new BufferedReader(new InputStreamReader(reader.getInputStream()))) {
                    String line;
                    while (!closed && (line = lines.readLine()) != null) {
                        try { keys.offer(Integer.parseInt(line)); }
                        catch (NumberFormatException ignored) { }
                    }
                } catch (IOException ignored) { }
                finally { ended = true; }
            };
        } else {
            savedMode = stty("-g").trim();
            try { stty("-icanon", "-echo", "min", "1", "time", "0"); }
            catch (IOException | InterruptedException e) { close(); throw e; }
            read = () -> {
                try {
                    int key;
                    while (!closed && (key = System.in.read()) != -1) keys.offer(key);
                } catch (IOException ignored) { }
                finally { ended = true; }
            };
        }
        Thread thread = new Thread(read, "console-input");
        thread.setDaemon(true);
        thread.start();
    }
    Integer poll() { return keys.poll(); }
    boolean ended() { return ended; }
    private static String stty(String... args) throws IOException, InterruptedException {
        var command = new java.util.ArrayList<String>();
        command.add("stty"); command.addAll(java.util.List.of(args));
        Process process = new ProcessBuilder(command).redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IOException("Could not configure terminal using stty.");
        return result;
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (reader != null) reader.destroy();
        if (savedMode != null) {
            try { stty(savedMode); }
            catch (IOException e) { System.err.println("Restore terminal echo with: stty sane"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
