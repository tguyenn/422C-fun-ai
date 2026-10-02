import java.util.Random;

/** Dependency-free rules regression suite. */
public final class SnakeGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        SnakeGame game = new SnakeGame(10, 8, new Random(42));
        check(!game.snake.contains(game.food), "Food starts in empty cell");
        game.turn(SnakeGame.Direction.UP); game.turn(SnakeGame.Direction.LEFT); game.tick();
        check(game.snake.getFirst().equals(new SnakeGame.Cell(5, 3)), "Prevent between-tick reversal");
        game.food = new SnakeGame.Cell(5, 2); game.tick();
        check(game.score == 10 && game.snake.size() == 4, "Eating grows snake and scores");
        check(game.delayMillis() == 212, "Eating increases speed");
        check(!game.snake.contains(game.food), "New food avoids body");
        game.tick(); game.tick(); game.tick();
        check(game.over, "Wall collision ends game");
        var head = game.snake.getFirst(); game.tick();
        check(game.snake.getFirst().equals(head), "Game over freezes movement");
        game.score = 10000;
        check(game.delayMillis() == 65, "Speed is capped");

        game = new SnakeGame(6, 4, new Random(1));
        game.snake.clear();
        game.snake.add(new SnakeGame.Cell(2, 1));
        game.snake.add(new SnakeGame.Cell(2, 2));
        game.snake.add(new SnakeGame.Cell(3, 2));
        game.snake.add(new SnakeGame.Cell(3, 1));
        game.food = new SnakeGame.Cell(0, 0); game.tick();
        check(!game.over, "Moving into vacating tail is legal");
        // Add a segment so the target below the head is not the vacating tail.
        game.snake.addLast(new SnakeGame.Cell(4, 2));
        game.turn(SnakeGame.Direction.DOWN); game.tick();
        check(game.over, "Body collision ends game");

        game = new SnakeGame(6, 4, new Random(2));
        game.snake.clear();
        game.snake.add(new SnakeGame.Cell(4, 0));
        for (int x = 3; x >= 0; x--) game.snake.add(new SnakeGame.Cell(x, 0));
        for (int y = 1; y < 4; y++) for (int i = 0; i < 6; i++) {
            int x = y % 2 == 1 ? i : 5 - i;
            game.snake.add(new SnakeGame.Cell(x, y));
        }
        game.food = new SnakeGame.Cell(5, 0); game.tick();
        check(game.won && game.over && game.food == null && game.snake.size() == 24,
                "Full board wins without food-placement loop");
        System.out.println("All Snake rules tests passed.");
    }
}
