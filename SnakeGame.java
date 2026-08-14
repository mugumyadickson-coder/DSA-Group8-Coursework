

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

public class SnakeGame extends JPanel implements ActionListener {
    // Game constants
    private final int BOARD_WIDTH = 600;
    private final int BOARD_HEIGHT = 600;
    private final int UNIT_SIZE = 25;
    
    // Wall boundaries
    private final int WALL_THICKNESS = 2;
    private final int PLAY_AREA_X = WALL_THICKNESS;
    private final int PLAY_AREA_Y = WALL_THICKNESS;
    private final int PLAY_AREA_WIDTH = (BOARD_WIDTH / UNIT_SIZE) - (2 * WALL_THICKNESS);
    private final int PLAY_AREA_HEIGHT = (BOARD_HEIGHT / UNIT_SIZE) - (2 * WALL_THICKNESS);
    
    // Difficulty settings
    private int DELAY = 100;
    private String difficulty = "Normal";
    

    private final LinkedList<Point> snake = new LinkedList<>();
    private int snakeLength = 3;
    private char direction = 'R';
    
  
    private final Stack<Point> tailHistory = new Stack<>();
    
    // Apple properties
    private Point apple;
    private final Random random = new Random();
    
    // Game state
    private boolean running = false;
    private boolean gameStarted = false;
    private javax.swing.Timer timer;
    private JButton retryButton, easyButton, normalButton, hardButton;
    
  
    private static class BSTNode {
        int score;
        BSTNode left, right;
        BSTNode(int score) { this.score = score; }
    }
    
    private BSTNode highScoreRoot = null;
    
    private BSTNode insertScore(BSTNode root, int score) {
        if (root == null) return new BSTNode(score);
        if (score < root.score) {
            root.left = insertScore(root.left, score);
        } else {
            root.right = insertScore(root.right, score);
        }
        return root;
    }
    
    private int getHighScore(BSTNode root) {
        if (root == null) return 0;
        while (root.right != null) {
            root = root.right;
        }
        return root.score;
    }

    private boolean isReachable(Point start, Point target) {
        boolean[][] visited = new boolean[PLAY_AREA_WIDTH][PLAY_AREA_HEIGHT];
        Queue<Point> queue = new ArrayDeque<>();
        
        // Mark snake segments as visited/blocked
        for (Point segment : snake) {
            int gridX = segment.x - PLAY_AREA_X;
            int gridY = segment.y - PLAY_AREA_Y;
            if (gridX >= 0 && gridX < PLAY_AREA_WIDTH && gridY >= 0 && gridY < PLAY_AREA_HEIGHT) {
                visited[gridX][gridY] = true;
            }
        }
        
        queue.add(start);
        visited[start.x - PLAY_AREA_X][start.y - PLAY_AREA_Y] = true;
        
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};
        
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (current.equals(target)) return true;
            
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                
                if (nx >= PLAY_AREA_X && nx < PLAY_AREA_X + PLAY_AREA_WIDTH &&
                    ny >= PLAY_AREA_Y && ny < PLAY_AREA_Y + PLAY_AREA_HEIGHT) {
                    
                    int gx = nx - PLAY_AREA_X;
                    int gy = ny - PLAY_AREA_Y;
                    if (!visited[gx][gy]) {
                        visited[gx][gy] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
        return false;
    }

    public SnakeGame() {
        setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        setBackground(new Color(128, 128, 0));
        setFocusable(true);
        setLayout(null);
        addKeyListener(new MyKeyAdapter());
        
        createDifficultyButtons();
    }

    private void createDifficultyButtons() {
        easyButton = new JButton("Easy");
        easyButton.setBounds(BOARD_WIDTH/2 - 200, BOARD_HEIGHT/2 - 60, 120, 40);
        easyButton.setFont(new Font("Arial", Font.BOLD, 16));
        easyButton.setBackground(new Color(76, 175, 80));
        easyButton.setForeground(Color.WHITE);
        easyButton.setFocusPainted(false);
        easyButton.addActionListener(e -> setDifficulty("Easy", 150));
        add(easyButton);
        
        normalButton = new JButton("Normal");
        normalButton.setBounds(BOARD_WIDTH/2 - 60, BOARD_HEIGHT/2 - 60, 120, 40);
        normalButton.setFont(new Font("Arial", Font.BOLD, 16));
        normalButton.setBackground(new Color(255, 152, 0));
        normalButton.setForeground(Color.WHITE);
        normalButton.setFocusPainted(false);
        normalButton.addActionListener(e -> setDifficulty("Normal", 100));
        add(normalButton);
        
        hardButton = new JButton("Hard");
        hardButton.setBounds(BOARD_WIDTH/2 + 80, BOARD_HEIGHT/2 - 60, 120, 40);
        hardButton.setFont(new Font("Arial", Font.BOLD, 16));
        hardButton.setBackground(new Color(244, 67, 54));
        hardButton.setForeground(Color.WHITE);
        hardButton.setFocusPainted(false);
        hardButton.addActionListener(e -> setDifficulty("Hard", 50));
        add(hardButton);
        
        retryButton = new JButton("Retry");
        retryButton.setBounds(BOARD_WIDTH/2 - 60, BOARD_HEIGHT/2 + 60, 120, 40);
        retryButton.setFont(new Font("Arial", Font.BOLD, 16));
        retryButton.setBackground(new Color(33, 150, 243));
        retryButton.setForeground(Color.WHITE);
        retryButton.setFocusPainted(false);
        retryButton.setVisible(false);
        retryButton.addActionListener(e -> restartGame());
        add(retryButton);
    }

    private void setDifficulty(String diff, int speed) {
        difficulty = diff;
        DELAY = speed;
        startGame();
    }

    private void startGame() {
        easyButton.setVisible(false);
        normalButton.setVisible(false);
        hardButton.setVisible(false);
        retryButton.setVisible(false);
        
        snake.clear();
        tailHistory.clear();
        
        int startX = PLAY_AREA_WIDTH / 2;
        int startY = PLAY_AREA_HEIGHT / 2;
        
        for (int i = 0; i < snakeLength; i++) {
            snake.addLast(new Point(startX - i, startY));
        }
        
        placeApple();
        
        running = true;
        gameStarted = true;
        if (timer != null) timer.stop();
        timer = new javax.swing.Timer(DELAY, this);
        timer.start();
        
        requestFocusInWindow();
    }

    private void restartGame() {
        easyButton.setVisible(true);
        normalButton.setVisible(true);
        hardButton.setVisible(true);
        retryButton.setVisible(false);
        
        running = false;
        gameStarted = false;
        snakeLength = 3;
        direction = 'R';
        
        if (timer != null) timer.stop();
        repaint();
        requestFocusInWindow();
    }

    private void placeApple() {
        int x, y;
        boolean validPlacement;
        
        do {
            validPlacement = true;
            x = random.nextInt(PLAY_AREA_WIDTH) + PLAY_AREA_X;
            y = random.nextInt(PLAY_AREA_HEIGHT) + PLAY_AREA_Y;
            
            for (Point segment : snake) {
                if (segment.x == x && segment.y == y) {
                    validPlacement = false;
                    break;
                }
            }
            
            // Validate using Graph BFS traversal if apple is reachable from snake's head
            if (validPlacement && !snake.isEmpty()) {
                validPlacement = isReachable(snake.getFirst(), new Point(x, y));
            }
        } while (!validPlacement);
        
        apple = new Point(x, y);
    }

    private void move() {
        Point head = snake.getFirst();
        Point newHead = new Point(head);
        
        switch (direction) {
            case 'R': newHead.x++; break;
            case 'L': newHead.x--; break;
            case 'U': newHead.y--; break;
            case 'D': newHead.y++; break;
        }
        
        if (newHead.x < PLAY_AREA_X || newHead.x >= PLAY_AREA_X + PLAY_AREA_WIDTH ||
            newHead.y < PLAY_AREA_Y || newHead.y >= PLAY_AREA_Y + PLAY_AREA_HEIGHT) {
            gameOver();
            return;
        }
        
        for (Point segment : snake) {
            if (segment.equals(newHead)) {
                gameOver();
                return;
            }
        }
        
        snake.addFirst(newHead);
        
        if (newHead.equals(apple)) {
            snakeLength++;
            placeApple();
        } else {
            Point removedTail = snake.removeLast();
            tailHistory.push(removedTail); // Save removed tail to Stack
        }
    }

    private void undoMove() {
        if (snake.size() > 1 && !tailHistory.isEmpty()) {
            snake.removeFirst(); // Remove current head
            snake.addLast(tailHistory.pop()); // Restore previous tail from Stack
            repaint();
        }
    }

    private void gameOver() {
        running = false;
        highScoreRoot = insertScore(highScoreRoot, snakeLength - 3); // Record score in Tree
        showRetryButton();
    }

    private void showRetryButton() {
        retryButton.setVisible(true);
        if (timer != null) timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    private void draw(Graphics g) {
        if (!gameStarted) {
            drawStartScreen(g);
        } else if (running) {
            drawGameScreen(g);
        } else {
            drawGameScreen(g);
            drawGameOver(g);
        }
    }

    private void drawStartScreen(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics metrics = getFontMetrics(g.getFont());
        String title = "Snake Game";
        g.drawString(title, (BOARD_WIDTH - metrics.stringWidth(title)) / 2, BOARD_HEIGHT/2 - 120);
        
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        String instruction = "Select Difficulty:";
        g.drawString(instruction, (BOARD_WIDTH - metrics2.stringWidth(instruction)) / 2, BOARD_HEIGHT/2 - 80);
        
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        FontMetrics metrics3 = getFontMetrics(g.getFont());
        String controls = "Controls: Arrow Keys or WASD | Press 'U' to Undo";
        g.drawString(controls, (BOARD_WIDTH - metrics3.stringWidth(controls)) / 2, BOARD_HEIGHT/2 + 120);
    }

    private void drawGameScreen(Graphics g) {
        g.setColor(new Color(124, 180, 70));
        int meadowX = WALL_THICKNESS * UNIT_SIZE;
        int meadowY = WALL_THICKNESS * UNIT_SIZE;
        int meadowWidth = PLAY_AREA_WIDTH * UNIT_SIZE;
        int meadowHeight = PLAY_AREA_HEIGHT * UNIT_SIZE;
        g.fillRect(meadowX, meadowY, meadowWidth, meadowHeight);
        
        drawBrickBorder(g);
        
        // Draw apple
        g.setColor(Color.RED);
        g.fillOval(apple.x * UNIT_SIZE + 2, apple.y * UNIT_SIZE + 2, UNIT_SIZE - 4, UNIT_SIZE - 4);
        
        // Draw snake
        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);
            g.setColor(i == 0 ? Color.BLACK : Color.DARK_GRAY);
            g.fillRect(segment.x * UNIT_SIZE + 1, segment.y * UNIT_SIZE + 1, UNIT_SIZE - 2, UNIT_SIZE - 2);
        }
        
        // Score & High Score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        String scoreText = "Score: " + (snakeLength - 3) + " | High Score: " + getHighScore(highScoreRoot) + " | Difficulty: " + difficulty;
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString(scoreText, (BOARD_WIDTH - metrics.stringWidth(scoreText)) / 2, 20);
    }

    private void drawBrickBorder(Graphics g) {
        int brickWidth = UNIT_SIZE;
        int brickHeight = UNIT_SIZE / 2;
        g.setColor(new Color(70, 130, 180));
        
        for (int x = 0; x < BOARD_WIDTH / UNIT_SIZE; x++) {
            g.fillRect(x * brickWidth, 0, brickWidth, brickHeight);
            g.fillRect(x * brickWidth, BOARD_HEIGHT - UNIT_SIZE, brickWidth, brickHeight);
        }
        for (int y = 0; y < BOARD_HEIGHT / UNIT_SIZE; y++) {
            g.fillRect(0, y * UNIT_SIZE, brickWidth, brickHeight);
            g.fillRect(BOARD_WIDTH - UNIT_SIZE, y * UNIT_SIZE, brickWidth, brickHeight);
        }
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        String gameOverText = "Game Over";
        g.drawString(gameOverText, (BOARD_WIDTH - metrics1.stringWidth(gameOverText)) / 2, BOARD_HEIGHT / 2 - 40);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        String scoreText = "Final Score: " + (snakeLength - 3) + " | High Score: " + getHighScore(highScoreRoot);
        g.drawString(scoreText, (BOARD_WIDTH - metrics2.stringWidth(scoreText)) / 2, BOARD_HEIGHT / 2);
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!gameStarted) return;
            
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    if (direction != 'R') direction = 'L';
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    if (direction != 'L') direction = 'R';
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    if (direction != 'D') direction = 'U';
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    if (direction != 'U') direction = 'D';
                    break;
                case KeyEvent.VK_U:
                    if (running) undoMove(); // Press 'U' to undo last move using Stack
                    break;
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        SnakeGame game = new SnakeGame();
        
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
