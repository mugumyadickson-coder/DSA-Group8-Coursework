
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


public class TicTacToe extends JFrame {

    private final char[][] board = new char[3][3];
    private final JButton[][] cells = new JButton[3][3];
    private static final char EMPTY = '-';
    private static final char HUMAN = 'X';
    private static final char AI = 'O';

    //  Stack for undo history 
    private final Deque<int[]> moveHistory = new ArrayDeque<>(); // {row, col, wasAiMove(0/1)}

    // Winning patterns (rows, cols, diagonals) as index triples 
    private static final int[][] WIN_PATTERNS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},   // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},   // columns
            {0, 4, 8}, {2, 4, 6}               // diagonals
    };

    private boolean vsAi = true;
    private char currentPlayer = HUMAN;
    private boolean gameOver = false;

    private final JLabel statusLabel = new JLabel("Player X's turn", SwingConstants.CENTER);
    private final JButton undoButton = new JButton("Undo");
    private final JButton newGameButton = new JButton("New Game");
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"Human vs AI", "Human vs Human"});

    public TicTacToe() {
        super("Tic Tac Toe");
        for (char[] row : board) Arrays.fill(row, EMPTY);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        //  Board panel 
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 6, 6));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font cellFont = new Font("SansSerif", Font.BOLD, 48);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton btn = new JButton("");
                btn.setFont(cellFont);
                btn.setFocusPainted(false);
                final int row = r, col = c;
                btn.addActionListener(e -> onCellClicked(row, col));
                cells[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        //  Top control panel
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Mode:"));
        topPanel.add(modeBox);
        modeBox.addActionListener(e -> {
            vsAi = modeBox.getSelectedIndex() == 0;
            resetGame();
        });
        topPanel.add(newGameButton);
        topPanel.add(undoButton);
        newGameButton.addActionListener(e -> resetGame());
        undoButton.addActionListener(e -> undoLastMove());

        //  Status bar 
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 10, 6));

        add(topPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setSize(420, 500);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    //  Cell click handling 
    private void onCellClicked(int r, int c) {
        if (gameOver || board[r][c] != EMPTY) return;
        if (vsAi && currentPlayer == AI) return; // ignore clicks during AI's turn

        makeMove(r, c, currentPlayer, false);
        refreshBoard();

        if (checkGameEnd()) return;

        currentPlayer = (currentPlayer == HUMAN) ? AI : HUMAN;
        updateStatus();

        if (vsAi && currentPlayer == AI && !gameOver) {
            // Let the AI "think" briefly off the click thread so the UI paints first.
            undoButton.setEnabled(false);
            SwingUtilities.invokeLater(this::aiTurn);
        }
    }

    private void aiTurn() {
        int[] mv = bestAiMove();
        if (mv[0] != -1) {
            makeMove(mv[0], mv[1], AI, true);
            refreshBoard();
        }
        if (!checkGameEnd()) {
            currentPlayer = HUMAN;
            updateStatus();
        }
        undoButton.setEnabled(true);
    }

    //  Move application / undo (Stack) 
    private void makeMove(int r, int c, char player, boolean wasAi) {
        board[r][c] = player;
        moveHistory.push(new int[]{r, c, wasAi ? 1 : 0}); // push onto stack
    }

    private void undoLastMove() {
        if (gameOver || moveHistory.isEmpty()) return;

        int[] last = moveHistory.pop(); // pop from stack -> O(1)
        board[last[0]][last[1]] = EMPTY;

        // In vs-AI mode, undo the human move that led to it too, so it's the
        // human's turn again (skip the AI's automatic reply).
        if (vsAi && last[2] == 1 && !moveHistory.isEmpty()) {
            int[] prev = moveHistory.pop();
            board[prev[0]][prev[1]] = EMPTY;
        }

        currentPlayer = HUMAN;
        gameOver = false;
        refreshBoard();
        updateStatus();
    }

    private void resetGame() {
        for (char[] row : board) Arrays.fill(row, EMPTY);
        moveHistory.clear();
        currentPlayer = HUMAN;
        gameOver = false;
        refreshBoard();
        updateStatus();
    }

    // ---------- Rendering ----------
    private void refreshBoard() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                char v = board[r][c];
                cells[r][c].setText(v == EMPTY ? "" : String.valueOf(v));
                cells[r][c].setForeground(v == HUMAN ? new Color(0x2563EB) : new Color(0xDC2626));
            }
        }
    }

    private void updateStatus() {
        if (gameOver) return;
        statusLabel.setText(currentPlayer == HUMAN ? "Player X's turn" : (vsAi ? "AI is thinking..." : "Player O's turn"));
    }

    // ---------- Win / draw checks using the pattern table ----------
    private char[] flat() {
        char[] f = new char[9];
        int k = 0;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                f[k++] = board[r][c];
        return f;
    }

    private char checkWinner() {
        char[] f = flat();
        for (int[] p : WIN_PATTERNS) {
            char a = f[p[0]], b = f[p[1]], c = f[p[2]];
            if (a != EMPTY && a == b && b == c) return a;
        }
        return EMPTY;
    }

    private List<Integer> winningLine() {
        char[] f = flat();
        for (int[] p : WIN_PATTERNS) {
            char a = f[p[0]], b = f[p[1]], c = f[p[2]];
            if (a != EMPTY && a == b && b == c) return List.of(p[0], p[1], p[2]);
        }
        return List.of();
    }

    private boolean isFull() {
        for (char[] row : board)
            for (char v : row)
                if (v == EMPTY) return false;
        return true;
    }

    private boolean checkGameEnd() {
        char winner = checkWinner();
        if (winner != EMPTY) {
            gameOver = true;
            highlightWin();
            String who = winner == HUMAN ? "Player X" : (vsAi ? "AI" : "Player O");
            statusLabel.setText(who + " wins!");
            JOptionPane.showMessageDialog(this, who + " wins!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        if (isFull()) {
            gameOver = true;
            statusLabel.setText("It's a draw!");
            JOptionPane.showMessageDialog(this, "It's a draw!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private void highlightWin() {
        for (int idx : winningLine()) {
            cells[idx / 3][idx % 3].setBackground(new Color(0xBBF7D0));
            cells[idx / 3][idx % 3].setOpaque(true);
        }
    }

    private int minimax(int depth, boolean isMaximizing, int alpha, int beta) {
        char winner = checkWinner();
        if (winner == AI) return 10 - depth;
        if (winner == HUMAN) return depth - 10;
        if (isFull()) return 0;

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (board[r][c] != EMPTY) continue;
                    board[r][c] = AI;
                    best = Math.max(best, minimax(depth + 1, false, alpha, beta));
                    board[r][c] = EMPTY;
                    alpha = Math.max(alpha, best);
                    if (beta <= alpha) return best;
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (board[r][c] != EMPTY) continue;
                    board[r][c] = HUMAN;
                    best = Math.min(best, minimax(depth + 1, true, alpha, beta));
                    board[r][c] = EMPTY;
                    beta = Math.min(beta, best);
                    if (beta <= alpha) return best;
                }
            }
            return best;
        }
    }

    private int[] bestAiMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] move = {-1, -1};
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] != EMPTY) continue;
                board[r][c] = AI;
                int score = minimax(0, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                board[r][c] = EMPTY;
                if (score > bestScore) {
                    bestScore = score;
                    move = new int[]{r, c};
                }
            }
        }
        return move;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicTacToe().setVisible(true));
    }
}