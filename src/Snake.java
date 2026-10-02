import java.util.Random;

/** ASCII Snake for Java 17+ and ANSI terminals. */
public final class Snake {
    private static final String ESC = "\u001b[";
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println("ASCII Snake: W/A/S/D move, P pauses, R restarts, Q quits. Java 17+.");
            return;
        }
        try {
            TerminalInput input = new TerminalInput();
            Thread cleanup = new Thread(() -> { input.close(); restoreScreen(); });
            Runtime.getRuntime().addShutdownHook(cleanup);
            System.out.print(ESC + "?1049h" + ESC + "?25l" + ESC + "2J");
            try { play(input); }
            finally {
                input.close(); restoreScreen();
                Runtime.getRuntime().removeShutdownHook(cleanup);
            }
        } catch (Exception e) {
            System.err.println("Unable to run Snake: " + e.getMessage());
            System.exit(1);
        }
    }
    private static void restoreScreen() {
        System.out.print(ESC + "?25h" + ESC + "?1049l");
        System.out.flush();
    }
    private static void play(TerminalInput input) throws InterruptedException {
        SnakeGame game = new SnakeGame(30, 16, new Random());
        boolean paused = false;
        int best = 0;
        long nextTick = System.nanoTime() + 1_000_000_000L;
        render(game, paused, best);
        while (true) {
            if (input.ended()) throw new IllegalStateException("Terminal input closed unexpectedly.");
            boolean dirty = false;
            Integer key;
            while ((key = input.poll()) != null) {
                switch (Character.toLowerCase((char) key.intValue())) {
                    case 'q': return;
                    case 'w': if (!paused) game.turn(SnakeGame.Direction.UP); break;
                    case 's': if (!paused) game.turn(SnakeGame.Direction.DOWN); break;
                    case 'a': if (!paused) game.turn(SnakeGame.Direction.LEFT); break;
                    case 'd': if (!paused) game.turn(SnakeGame.Direction.RIGHT); break;
                    case 'p':
                        if (!game.over) {
                            paused = !paused;
                            nextTick = System.nanoTime() + game.delayMillis() * 1_000_000L;
                            dirty = true;
                        }
                        break;
                    case 'r':
                        game = new SnakeGame(30, 16, new Random()); paused = false;
                        nextTick = System.nanoTime() + 1_000_000_000L; dirty = true;
                        break;
                    default: break;
                }
            }
            if (!paused && !game.over && System.nanoTime() >= nextTick) {
                game.tick(); best = Math.max(best, game.score);
                nextTick = System.nanoTime() + game.delayMillis() * 1_000_000L;
                dirty = true;
            }
            if (dirty) render(game, paused, best);
            Thread.sleep(5);
        }
    }
    private static void render(SnakeGame game, boolean paused, int best) {
        StringBuilder out = new StringBuilder(ESC + "H");
        line(out, "ASCII SNAKE | Score: " + game.score + " | Best: " + best);
        line(out, "Move interval: " + game.delayMillis() + " ms");
        line(out, "+" + "-".repeat(game.width) + "+");
        for (int y = 0; y < game.height; y++) {
            StringBuilder row = new StringBuilder("|");
            for (int x = 0; x < game.width; x++) {
                SnakeGame.Cell cell = new SnakeGame.Cell(x, y);
                row.append(cell.equals(game.snake.getFirst()) ? '@'
                        : game.snake.contains(cell) ? 'o' : cell.equals(game.food) ? '*' : ' ');
            }
            line(out, row.append('|').toString());
        }
        line(out, "+" + "-".repeat(game.width) + "+");
        line(out, "W/A/S/D: move | P: pause | R: restart | Q: quit");
        line(out, game.won ? "YOU WIN! Board filled. Press R to play again."
                : game.over ? "GAME OVER! Press R to play again."
                : paused ? "PAUSED - press P to resume."
                : "Eat * for 10 points. Avoid walls and your body!");
        out.append(ESC).append("J");
        System.out.print(out); System.out.flush();
    }
    private static void line(StringBuilder out, String text) {
        out.append(text).append(ESC).append("K\r\n");
    }
}
