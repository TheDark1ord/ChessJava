package chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import chess.Logic.ChessBoard;
import chess.Logic.ChessPiece;
import chess.Logic.ChessBoard.GameResult;
import chess.Logic.ChessPiece.ChessColor;
import chess.Moves.Castling;
import chess.Moves.Move;

public class App extends Application {
    // Visual settings
    private final int WIDTH = 720, HEIGHT = 720;
    // Black at the bottom if true
    private boolean flipTheBoard = false;
    // How much to shrink piece textures
    private final double squarePadding = 0.05;
    private final String title = "Chess";

    private final Color whiteColor = Color.web("0xf5e6bf");
    private final Color blackColor = Color.web("0x725046");
    private final Color selectedColor = Color.web("0xff4545", 0.5);
    private final Color prevMoveColor = Color.web("0xfeff93", 0.75);

    private final String startingPos = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        stage.setResizable(false);
        stage.setTitle(title);

        loadIcon(stage);
        loadPieceTextures();
        loadSounds();

        Group group = new Group(mainCanvas);
        Scene scene = new Scene(group, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.sizeToScene();

        // Set up the position
        board = new ChessBoard();
        // Load starting position
        board.setPosition(startingPos);

        // Events
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, mousePosHandler);
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, mousePosHandler);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        mainCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
        mainCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, mouseHandler);
        // Main loop
        new AnimationTimer() {
            public void handle(long currentTime) {
                mainLoop(currentTime);
            }
        }.start();

        stage.show();
    }

    private void mainLoop(long currentTime) {
        GraphicsContext context = mainCanvas.getGraphicsContext2D();
        context.clearRect(0, 0, WIDTH, HEIGHT);

        drawBackground(context);
        drawPieces(context);
        if (selectedPiece != null)
            drawPieceMoves(context, selectedPiece);
    }

    private EventHandler<MouseEvent> mouseHandler = new EventHandler<>() {
        @Override
        public void handle(MouseEvent ev) {
            // Coord of a click on a board
            Vector destCoord = screenToBoardCoord(mousePos);

            if (ev.getEventType().equals(MouseEvent.MOUSE_PRESSED) && ev.getButton().equals(MouseButton.PRIMARY)) {
                if (selectedPiece != null) {
                    // Click on the same piece twice
                    if (selectedPiece.pos().equals(destCoord)) {
                        selectedPiece = null;
                        return;
                    }
                    // If the move is invalid, continue
                    if (movePiece(destCoord))
                        return;
                }
                mouseHold = true;
                selectedPiece = board.getPiece(destCoord);
                if (selectedPiece != null) {
                    if (selectedPiece.color() != board.getCurrentColor()) {
                        selectedPiece = null;
                    }
                }

            } else if (ev.getEventType().equals(MouseEvent.MOUSE_RELEASED)
                    && ev.getButton().equals(MouseButton.PRIMARY)) {

                if (mouseHold && selectedPiece != null) {
                    if (!selectedPiece.pos().equals(destCoord)) {
                        if (!movePiece(destCoord)) {
                            selectedPiece = null;
                        }
                    }
                }
                mouseHold = false;
            }
        }
    };

    private EventHandler<MouseEvent> mousePosHandler = new EventHandler<>() {
        @Override
        public void handle(MouseEvent ev) {
            mousePos = new Vector((int) ev.getX(), (int) ev.getY());
        }
    };

    private EventHandler<KeyEvent> keyHandler = new EventHandler<>() {
        @Override
        public void handle(KeyEvent ev) {
            if (ev.getEventType().equals(KeyEvent.KEY_PRESSED)) {
                switch (ev.getCode()) {
                    case F:
                        flipTheBoard ^= true;
                        break;
                    case LEFT:
                        board.undoMove();
                    default:
                        break;
                }
            }
        }
    };

    private void drawBackground(GraphicsContext context) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                context.setFill((x + y) % 2 == 0 ? blackColor : whiteColor);
                context.fillRect(squareWidth * x, HEIGHT - squareHeight * (y + 1), squareWidth, squareHeight);
            }
        }

        if (selectedPiece != null) {
            context.setFill(selectedColor);

            if (flipTheBoard) {
                context.fillRect(squareWidth * selectedPiece.pos().x,
                        squareHeight * (selectedPiece.pos().y + 1), squareWidth, squareHeight);
            } else {
                context.fillRect(squareWidth * selectedPiece.pos().x,
                        HEIGHT - squareHeight * (selectedPiece.pos().y + 1), squareWidth, squareHeight);
            }
        }
        if (prevFrom != null && prevTo != null) {
            context.setFill(prevMoveColor);

            if (flipTheBoard) {
                context.fillRect(squareWidth * prevFrom.x,
                        squareHeight * prevFrom.y, squareWidth, squareHeight);
                context.fillRect(squareWidth * prevTo.x,
                        squareHeight * prevTo.y, squareWidth, squareHeight);
            } else {
                context.fillRect(squareWidth * prevFrom.x,
                        HEIGHT - squareHeight * (prevFrom.y + 1), squareWidth, squareHeight);
                context.fillRect(squareWidth * prevTo.x,
                        HEIGHT - squareHeight * (prevTo.y + 1), squareWidth, squareHeight);
            }
        }
    }

    private void drawPieces(GraphicsContext context) {
        for (ChessPiece piece : board) {
            Integer index = getPieceTextureIndex(piece.getName(), piece.color());
            Double paddingX = squareWidth * squarePadding;
            Double paddingY = squareHeight * squarePadding;
            // Compare by adress
            if (piece == selectedPiece) {
                // Draw dragged piece under the cursor
                if (mouseHold) {
                    context.drawImage(
                            piceTextures[index],
                            mousePos.x - squareWidth / 2, mousePos.y - squareHeight / 2,
                            squareWidth, squareHeight);

                    continue;
                }
            }
            if (flipTheBoard) {
                context.drawImage(
                        piceTextures[index],
                        piece.pos().x * squareWidth + paddingX, piece.pos().y * squareHeight + paddingY,
                        squareWidth - paddingX * 2, squareHeight - paddingY * 2);
            } else {
                context.drawImage(
                        piceTextures[index],
                        piece.pos().x * squareWidth + paddingX, HEIGHT - (piece.pos().y + 1) * squareHeight + paddingY,
                        squareWidth - paddingX * 2, squareHeight - paddingY * 2);
            }
        }
    }

    private void drawPieceMoves(GraphicsContext context, ChessPiece piece) {
        context.setStroke(Color.rgb(255, 255, 255, 0.8));
        final double radX = squareWidth / 3;
        final double radY = squareHeight / 3;

        for (Move move : board.getPieceMoves(piece)) {
            context.setLineWidth(5);

            if (flipTheBoard) {
                context.strokeOval((move.to.x + 0.5) * squareWidth - radX / 2,
                        (move.to.y + 0.5) * squareHeight - radY / 2, radX, radY);
            } else {
                context.strokeOval((move.to.x + 0.5) * squareWidth - radX / 2,
                        HEIGHT - (move.to.y + 0.5) * squareHeight - radY / 2, radX, radY);
            }
        }
    }

    // Move selected piece to the specified pos
    private boolean movePiece(Vector to) {
        prevFrom = selectedPiece.pos();
        prevTo = to;

        if (board.makeAMove(new Move(selectedPiece, prevFrom, to))) {
            playSound(whatToPlay(board.getLastMove()), board.getCurrentColor());

            selectedPiece = null;
            return true;
        } else {
            prevFrom = null;
            prevTo = null;
            return false;
        }
    }

    // Determines what sound to play after the last move
    private Sound whatToPlay(Move move) {
        if (board.gameResult != GameResult.NONE) {
            return Sound.GameOver;
        }
        if (board.isInCheck()) {
            return Sound.Check;
        }

        if (move instanceof Castling) {
            return Sound.Castles;
        }
        if (move.captured != null) {
            return Sound.Capture;
        }
        return Sound.Move;
    }

    private enum Sound {
        Move, Capture,
        Castles, Check,
        GameOver
    };

    private void playSound(Sound sound, ChessColor side) {
        int ordinal = sound.ordinal();
        int index;

        // The first 4 sounds are slightly different for black and white
        if (ordinal < 4) {
            index = ordinal * 2 + side.ordinal();
        } else {
            // The next are identical for both sides
            index = 9;
        }

        MediaPlayer player = new MediaPlayer(sounds[index]);
        player.play();
    }

    private void loadIcon(Stage stage) throws IOException {
        String iconPath = currentPath + resourcePath + "\\icon.png";
        if (!(new File(iconPath).exists()))
            return;

        Image icon = new Image(new File(iconPath).toURI().toString());
        stage.getIcons().add(icon);
    }

    private List<String> textureFilenames = Arrays.asList(
            "wK.svg", "wQ.svg", "wR.svg",
            "wN.svg", "wB.svg", "wP.svg",
            "bK.svg", "bQ.svg", "bR.svg",
            "bN.svg", "bB.svg", "bP.svg");

    private void loadPieceTextures() {
        piceTextures = new Image[12];

        String texturePath = currentPath + resourcePath + "\\Chess Pieces\\";

        for (int i = 0; i < textureFilenames.size(); i++) {
            if (!(new File(texturePath + textureFilenames.get(i))).exists()) {
                throw new RuntimeException("Texture file not found!");
            }

            BufferedImage bImg;
            try {
                bImg = ImageIO.read(new File(texturePath + textureFilenames.get(i)));
            } catch (IOException ioEx) {
                System.out.println(ioEx.getMessage());
                System.exit(1);

                // To make compiler happy)
                return;
            }
            piceTextures[i] = SwingFXUtils.toFXImage(bImg, null);
        }
    }

    private Media[] sounds;
    private List<String> soundFilenames = Arrays.asList(
            "Move-W.wav", "Move-B.wav",
            "Capture-W.wav", "Capture-B.wav",
            "Castles-W.wav", "Castles-B.wav",
            "Check-W.wav", "Check-B.wav",
            "EndGame.wav");

    private void loadSounds() {
        sounds = new Media[10];

        String soundPath = currentPath + resourcePath + "\\Sounds\\";

        for (int i = 0; i < soundFilenames.size(); i++) {
            if (!(new File(soundPath + soundFilenames.get(i))).exists()) {
                throw new RuntimeException("Sound file not found!");
            }

            sounds[i] = new Media(new File(soundPath + soundFilenames.get(i)).toURI().toString());
        }

    }

    private Vector screenToBoardCoord(Vector screenCoord) {
        if (flipTheBoard) {
            return new Vector((int) (screenCoord.x / squareWidth),
                    (int) (screenCoord.y / squareWidth));
        } else {
            return new Vector((int) (screenCoord.x / squareWidth),
                    (int) ((HEIGHT - screenCoord.y) / squareWidth));
        }
    }

    private Integer getPieceTextureIndex(ChessPiece.Name name, ChessPiece.ChessColor color) {
        return name.ordinal() + (color == ChessPiece.ChessColor.BLACK ? (piceTextures.length / 2) : 0);
    }

    private Image[] piceTextures;

    private ChessBoard board;
    private ChessPiece selectedPiece;
    // Previous move
    private Vector prevFrom, prevTo;

    private boolean mouseHold;
    private Vector mousePos;

    private final Canvas mainCanvas = new Canvas(WIDTH, HEIGHT);
    private final int squareWidth = WIDTH / 8;
    private final int squareHeight = HEIGHT / 8;

    private final String currentPath = System.getProperty("user.dir");
    private final String resourcePath = "\\Resources";
}