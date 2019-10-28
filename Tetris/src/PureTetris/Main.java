package PureTetris;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

enum Shapes {
    EMPTY_SHAPE(new int[][]{{0, 0}, {0, 0}, {0, 0}, {0, 0}}, Color.white),
    O_SHAPE(new int[][]{{0, 0}, {-1, 0}, {-1, 1}, {0, 1}}, Color.YELLOW),
    I_SHAPE(new int[][]{{-2, 0}, {-1, 0}, {0, 0}, {1, 0}}, Color.BLUE),
    S_SHAPE(new int[][]{{-1, 0}, {0, 0}, {0, 1}, {1, 1}}, Color.RED),
    Z_SHAPE(new int[][]{{-1, 1}, {0, 1}, {0, 0}, {1, 0}}, Color.GREEN),
    L_SHAPE(new int[][]{{-1, 1}, {-1, 0}, {0, 0}, {1, 0}}, Color.orange),
    J_SHAPE(new int[][]{{-1, 0}, {-1, 1}, {0, 1}, {1, 1}}, Color.pink),
    T_SHAPE(new int[][]{{-1, 1}, {0, 1}, {0, 0}, {1, 1}}, Color.MAGENTA);

    private int[][] shape;
    private Color color;

    Shapes(int[][] shape, Color color) {
        this.shape = shape;
        this.color = color;
    }

    public int[][] getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }
}

public class Main extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }

    public Main() {
        super("Tetris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(new GameMap());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class Tetromino {
    private Shapes shape;
    private int[][] coords;

    public Tetromino() {
        coords = new int[4][2];
        setShape(Shapes.EMPTY_SHAPE);
    }

    public void setShape(Shapes s) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                coords[i][j] = s.getShape()[i][j];
            }
        }
        shape = s;
    }

    public Shapes getShape() {
        return shape;
    }

    public void randShape() {
        int x = (int) (1 + Math.random() * 7);
        setShape(Shapes.values()[x]);
    }

    public void setX(int index, int x) {
        coords[index][0] = x;
    }

    public void setY(int index, int y) {
        coords[index][1] = y;
    }

    public int getX(int index) {
        return coords[index][0];
    }

    public int getY(int index) {
        return coords[index][1];
    }

    public int maxX() {
        int mx = coords[0][0];
        for (int[] coord : coords) {
            if (mx < coord[0]) mx = coord[0];
        }
        return mx;
    }

    public int maxY() {
        int my = coords[0][0];
        for (int[] coord : coords) {
            if (my < coord[1]) my = coord[1];
        }
        return my;
    }

    public Tetromino turnRight() {
        if (shape == Shapes.O_SHAPE) return this;
        Tetromino newTetromino = new Tetromino();
        newTetromino.shape = shape;
        for (int i = 0; i < 4; i++) {
            newTetromino.setX(i, getY(i));
            newTetromino.setY(i, -getX(i));
        }
        return newTetromino;
    }

    public Tetromino turnLeft() {
        if (shape == Shapes.O_SHAPE) return this;
        Tetromino newTetromino = new Tetromino();
        newTetromino.shape = shape;
        for (int i = 0; i < 4; i++) {
            newTetromino.setX(i, -getY(i));
            newTetromino.setY(i, getX(i));
        }
        return newTetromino;
    }

}

class GameMap extends JPanel implements ActionListener, KeyListener {

    private static final int HEIGHT = 22;
    public static final int WIDTH = 10;
    private final int block = 25;
    private Timer time;
    private Clip clip;
    private boolean fallingshapeDefined;
    private Tetromino currentShape;
    private int currentX = 0;
    private int currentY = 0;
    private int scores = 0;
    private boolean scoresAdded = false;
    private int shapesOnScreen = 0;
    private boolean gameIsOn;


    private Shapes[] gameMap;

    public GameMap() {
        gameIsOn = true;
        init();
    }

    private void music() {
        new Thread() {
            @Override
            public void run() {
                try {
                    InputStream in = new BufferedInputStream(new FileInputStream("src/resourses/MUSIC/tetris.wav"));
                    clip = AudioSystem.getClip();
                    clip.open(AudioSystem.getAudioInputStream(in));
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                } catch (Exception e) {
                    System.err.println(e);
                }

            }
        }.start();
    }

    public void init() {
        currentShape = new Tetromino();
        gameMap = new Shapes[WIDTH * HEIGHT];
        clearBoard();
        fallingshapeDefined = false;
        music();
        setFocusable(true);
        setPreferredSize(new Dimension(640, 560));
        time = new Timer(1000, this);
        addKeyListener(this);
        setFocusTraversalKeysEnabled(false);
        time.start();
        newPiece();
    }

    public void clearBoard() {
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            gameMap[i] = Shapes.EMPTY_SHAPE;
        }
    }

    public void newPiece() {
        currentShape.randShape();
        currentX = WIDTH / 2;
        currentY = currentShape.maxY();
        if (tryMoveDown(currentShape)) {
            shapesOnScreen++;
            fallingshapeDefined = true;
        } else {
            fallingshapeDefined = false;
            time.stop();
            gameIsOn = false;
            repaint();
        }
    }

    private void concat() {
        int x, y;
        if (!scoresAdded) {
            scores += 10;
            scoresAdded = true;
        }
        for (int i = 0; i < 4; i++) {
            x = currentX + currentShape.getX(i);
            y = currentY + currentShape.getY(i);
            gameMap[y * WIDTH + x] = currentShape.getShape();
        }

    }

    public boolean tryMoveDown(Tetromino shape) {
        int x;
        int y;
        for (int i = 0; i < 4; i++) {
            x = currentX + shape.getX(i);
            y = currentY + shape.getY(i);
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT - 1) {
                concat();
                fallingshapeDefined = false;
                return false;
            }
            if (shapeAt(x, y + 1) != Shapes.EMPTY_SHAPE) {
                concat();
                fallingshapeDefined = false;
                return false;
            }
        }

        currentY += 1;
        repaint();
        return true;
    }

    private void tryMoveRight(Tetromino shape) {
        int x;
        int y;
        for (int i = 0; i < 4; i++) {
            x = currentX + shape.getX(i);
            y = currentY + shape.getY(i);
            if (x >= WIDTH - 1) {
                return;
            }
            if (shapeAt(x + 1, y) != Shapes.EMPTY_SHAPE) {
                return;
            }
        }
        currentX += 1;
        repaint();
        return;
    }

    private void tryMoveLeft(Tetromino shape) {
        int x;
        int y;
        for (int i = 0; i < 4; i++) {
            x = currentX + shape.getX(i);
            y = currentY + shape.getY(i);
            if (x <= 0) {
                return;
            }
            if (shapeAt(x + 1, y) != Shapes.EMPTY_SHAPE) {
                return;
            }
        }
        currentX -= 1;
        repaint();
        return;
    }

    private void tryRotate(Tetromino shape, int side) {
        if (side == 1) {
            shape = currentShape.turnRight();
        } else if (side == -1) {
            shape = currentShape.turnLeft();
        }
        for (int i = 0; i < 4; i++) {
            if (currentX + shape.getX(i) < 0 || currentX + shape.getX(i) >= WIDTH || currentY + shape.getY(i) < 0 || currentY + shape.getY(i) >= HEIGHT - 1) {
                return;
            }
            if (shapeAt(currentX + shape.getX(i), currentY + shape.getY(i) + 1) != Shapes.EMPTY_SHAPE) {
                return;
            }
        }
        currentShape = shape;
        repaint();
    }

    private Shapes shapeAt(int x, int y) {
        return gameMap[y * WIDTH + x];
    }

    private void removeLines() {
        int countFullLines = 0;
        for (int i = HEIGHT - 1; i >= 0; i--) {
            boolean fulline = true;
            for (int j = 0; j < WIDTH; j++) {
                if (shapeAt(j, i) == Shapes.EMPTY_SHAPE) {
                    fulline = false;
                    break;
                }
            }
            if (fulline) {
                for (int k = i; k > 1; k--) {
                    for (int j = 0; j < WIDTH; j++) {
                        gameMap[k * WIDTH + j] = shapeAt(j, k - 1);
                    }
                }
                countFullLines++;
                sound();
                scores = scores + countFullLines * 100;
            }
        }

    }

    private void sound() {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream("src/resourses/MUSIC/Beep8.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(in));
            clip.start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(gameIsOn) {
            for (int i = 0; i < HEIGHT; i++) {
                for (int j = 0; j < WIDTH; j++) {
                    g.setColor(shapeAt(j, i).getColor());
                    g.fillRect(j * block, i * block, block, block);
                    if(i == 1){
                        g.setColor(Color.red);
                    } else{
                        g.setColor(shapeAt(j, i).getColor().darker());
                    }
                    g.drawRect(j * block + 1, i * block + 1, block - 1, block - 1);

                }
            }
            for (int i = 0; i < 4; i++) {
                g.setColor(currentShape.getShape().getColor());
                g.fillRect((currentX + currentShape.getX(i)) * block,
                        (currentY + currentShape.getY(i)) * block, block, block);
                g.setColor(currentShape.getShape().getColor().darker());
                g.drawRect((currentX + currentShape.getX(i)) * block + 1,
                        (currentY + currentShape.getY(i)) * block + 1, block - 1, block - 1);
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 25));
            g.drawString("scores: " + scores, block * 10 + 50, 50);
            g.drawString("shapes appeared: " + shapesOnScreen, block * 10 + 50, 100);
        } else{
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(Color.red);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 100));
            g.drawString("GAME OVER", getWidth()/15, getHeight()/3);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_D:
                System.out.println("right");
                tryMoveRight(currentShape);
                break;
            case KeyEvent.VK_A:
                System.out.println("left");
                tryMoveLeft(currentShape);
                break;
            case KeyEvent.VK_S:
                System.out.println("down");
                tryMoveDown(currentShape);
                break;
            case KeyEvent.VK_Q:
                System.out.println("turn left");
                tryRotate(currentShape, -1);
                break;
            case KeyEvent.VK_E:
                System.out.println("turn right");
                tryRotate(currentShape, 1);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (!fallingshapeDefined) {
            newPiece();
        }
        tryMoveDown(currentShape);
        removeLines();
        scoresAdded = false;
    }
}
