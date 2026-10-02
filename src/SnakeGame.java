import java.util.*;

/** Game rules, independent of the terminal. */
final class SnakeGame {
    record Cell(int x, int y) { }
    enum Direction {
        UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
        final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }
    final int width, height;
    final Deque<Cell> snake = new ArrayDeque<>();
    private final Random random;
    private Direction direction = Direction.RIGHT, pending = direction;
    Cell food;
    boolean bonusFood;
    int score, meals;
    boolean over, won;

    SnakeGame(int width, int height, Random random) {
        if (width < 6 || height < 4) throw new IllegalArgumentException("Board too small");
        this.width = width; this.height = height; this.random = random;
        for (int i = 0; i < 3; i++) snake.addLast(new Cell(width / 2 - i, height / 2));
        placeFood();
    }
    void turn(Direction next) {
        // Compare with the executed move, preventing reversal between ticks.
        if (next.dx != -direction.dx || next.dy != -direction.dy) pending = next;
    }
    int delayMillis() { return Math.max(65, 150 - meals * 8); }
    void tick() {
        if (over) return;
        direction = pending;
        Cell head = snake.getFirst();
        Cell next = new Cell(head.x + direction.dx, head.y + direction.dy);
        boolean eating = next.equals(food);
        boolean hitsBody = snake.contains(next) && (eating || !next.equals(snake.getLast()));
        if (next.x < 0 || next.x >= width || next.y < 0 || next.y >= height || hitsBody) {
            over = true;
            return;
        }
        snake.addFirst(next);
        if (eating) { score += bonusFood ? 50 : 10; meals++; placeFood(); }
        else snake.removeLast();
    }
    private void placeFood() {
        List<Cell> free = new ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            Cell cell = new Cell(x, y);
            if (!snake.contains(cell)) free.add(cell);
        }
        if (free.isEmpty()) { food = null; won = over = true; }
        else {
            food = free.get(random.nextInt(free.size()));
            bonusFood = random.nextInt(5) == 0;
        }
    }
}
