package chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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

public class App extends Application {
    // Visual settings
    private final int WIDTH = 720, HEIGHT = 720;
    // How much to shrink piece textures
    private final double squarePadding = 0.05;
    private final String title = "Chess";

    private final Color whiteColor = Color.web("0xf5e6bf");
    private final Color blackColor = Color.web("0x725046");
    private final Color selectedColor = Color.web("0xff4545", 0.5);
    private final Color prevMoveColor = Color.web("0xa2d8d5", 0.0);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        stage.setResizable(false);
        stage.setTitle(title);

        loadIcon(stage);
        loadPieceTextures();

        Group group = new Group(mainCanvas);
        Scene scene = new Scene(group, WIDTH, HEIGHT);
        stage.setScene(scene);
        stage.sizeToScene();

        // Set up the position
        board = new ChessBoard();
        // Load starting position
        board.setPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Events
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, mousePosHandler);
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, mousePosHandler);
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
            Pos destCoord = screenToBoardCoord(mousePos);

            if (ev.getEventType().equals(MouseEvent.MOUSE_PRESSED) && ev.getButton().equals(MouseButton.PRIMARY)) {
                if (selectedPiece != null) {
                    // Click on the same piece twice
                    if (selectedPiece.pos.equals(destCoord)) {
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
                    if (selectedPiece.color != (board.whiteToMove() ? ChessColor.WHITE : ChessColor.BLACK)) {
                        selectedPiece = null;
                    }
                }

            } else if (ev.getEventType().equals(MouseEvent.MOUSE_RELEASED)
                    && ev.getButton().equals(MouseButton.PRIMARY)) {

                if (mouseHold && selectedPiece != null) {
                    if (!selectedPiece.pos.equals(destCoord)) {
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
            mousePos = new Pos((int) ev.getX(), (int) ev.getY());
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
            context.fillRect(squareWidth * selectedPiece.pos.x,
                    HEIGHT - squareHeight * (selectedPiece.pos.y + 1), squareWidth, squareHeight);
        }
        if (prevFrom != null && prevTo != null) {
            context.setFill(prevMoveColor);
            context.fillRect(squareWidth * prevFrom.x,
                    HEIGHT - squareHeight * (prevFrom.y + 1), squareWidth, squareHeight);
            context.fillRect(squareWidth * prevTo.x,
                    HEIGHT - squareHeight * (prevTo.y + 1), squareWidth, squareHeight);
        }
    }

    private void drawPieces(GraphicsContext context) {
        for (ChessPiece piece : board) {
            Integer index = getPieceTextureIndex(piece.getName(), piece.color);
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
            context.drawImage(
                    piceTextures[index],
                    piece.pos.x * squareWidth + paddingX, HEIGHT - (piece.pos.y + 1) * squareHeight + paddingY,
                    squareWidth - paddingX * 2, squareHeight - paddingY * 2);
        }
    }

    private void drawPieceMoves(GraphicsContext context, ChessPiece piece) {
        context.setStroke(Color.rgb(255, 255, 255, 0.8));
        final double radX = squareWidth / 3;
        final double radY = squareHeight / 3;

        for (Pos pos : board.getPieceMoves(piece)) {
            context.setLineWidth(5);
            context.strokeOval((pos.x + 0.5) * squareWidth - radX / 2,
                    HEIGHT - (pos.y + 0.5) * squareHeight - radY / 2, radX, radY);
        }
    }

    // Move selected piece to the specified pos
    private boolean movePiece(Pos to) {
        prevFrom = selectedPiece.pos;
        prevTo = to;

        if (board.makeAMove(selectedPiece.pos, to)) {
            selectedPiece = null;
            return true;
        } else {
            prevFrom = null;
            prevTo = null;
            return false;
        }
    }

    private void loadIcon(Stage stage) throws IOException {
        String iconPath = currentPath + resourcePath + "\\icon.png";
        if (!(new File(iconPath).exists()))
            return;

        Image icon = new Image(new File(iconPath).toURI().toString());
        stage.getIcons().add(icon);
    }

    private void loadPieceTextures() {
        piceTextures = new Image[12];

        String texturePath = currentPath + resourcePath + "\\Chess Pieces\\";
        if (!(new File(texturePath)).exists())
            throw new RuntimeException("Texture file not found!");

        List<String> filenames = Arrays.asList(
                "wK.svg", "wQ.svg", "wR.svg",
                "wN.svg", "wB.svg", "wP.svg",
                "bK.svg", "bQ.svg", "bR.svg",
                "bN.svg", "bB.svg", "bP.svg");

        for (int i = 0; i < filenames.size(); i++) {
            if (!(new File(texturePath + filenames.get(i))).exists()) {
                throw new RuntimeException("Texture file not found!");
            }

            BufferedImage bImg;
            try {
                bImg = ImageIO.read(new File(texturePath + filenames.get(i)));
            } catch (IOException ioEx) {
                System.out.println(ioEx.getMessage());
                System.exit(1);

                // To make compiler happy)
                return;
            }
            piceTextures[i] = SwingFXUtils.toFXImage(bImg, null);
        }
    }

    private Pos screenToBoardCoord(Pos screenCoord) {
        return new Pos((int) (screenCoord.x / squareWidth),
                (int) ((HEIGHT - screenCoord.y) / squareWidth));
    }

    private Integer getPieceTextureIndex(ChessPiece.Name name, ChessColor color) {
        return name.ordinal() + (color == ChessColor.BLACK ? (piceTextures.length / 2) : 0);
    }

    private Image[] piceTextures;

    private ChessBoard board;
    private ChessPiece selectedPiece;
    // Previous move
    private Pos prevFrom, prevTo;

    private boolean mouseHold;
    private Pos mousePos;

    private final Canvas mainCanvas = new Canvas(WIDTH, HEIGHT);
    private final int squareWidth = WIDTH / 8;
    private final int squareHeight = HEIGHT / 8;

    private final String currentPath = System.getProperty("user.dir");
    private final String resourcePath = "\\Resources";
}