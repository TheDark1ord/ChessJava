package chess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import chess.Logic.ChessBoard;
import chess.Moves.Move;

class Stockfish {
    private final String EnginePath = "\\Resources\\stockfish.exe";
    private final String currentPath = System.getProperty("user.dir");

    private final Process stockfishProcess;
    private final ChessBoard board;

    private final Object ioMutex = new Object();
    private final Writer processInput;
    private final BufferedReader processOutput;

    private boolean isThinking = false;
    private Move bestMove = null;

    Stockfish(ChessBoard board) throws IOException {
        stockfishProcess = new ProcessBuilder(currentPath + EnginePath).start();
        this.board = board;

        processInput = new OutputStreamWriter(stockfishProcess.getOutputStream(), StandardCharsets.UTF_8);
        processOutput = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));

        writeCommand("isready");
    }

    public void close() throws IOException {
        processInput.close();
        processOutput.close();
        stockfishProcess.destroy();
    }

    public void feedPosition() throws IOException {
        if (!isReady() || isThinking()) {
            return;
        }

        writeCommand(String.format("position fen %s", board.toFEN()));
        writeCommand("go movetime 1000");

        Thread waitForMove = new Thread() {
            public void run() {
                while (true) {
                    try {
                        waitForMove();
                        break;
                    } catch (IOException e) {}
                }

                synchronized(this) {
                    isThinking = false;
                }
            }
        };
        waitForMove.start();

        synchronized (this) {
            isThinking = true;
        }
    }

    public Move getBestMove() throws IOException {
        if (!isReady() || isThinking()) {
            return null;
        }

        Move toReturn;
        synchronized (this) {
            isThinking = false;
            toReturn =  bestMove;
            bestMove = null;
        }
        return toReturn;
    }

    private void writeCommand(String command) throws IOException {
        synchronized (ioMutex) {
            processInput.write(command + "\n");
            processInput.flush();
        }
    }

    private String readLine() throws IOException {
        synchronized (ioMutex) {
            return processOutput.readLine();
        }
    }

    private boolean isReady = false;

    public boolean isReady() throws IOException {
        if (isReady) {
            return true;
        }
        while (true) {
            String line = readLine();

            if (line.equals("")) {
                break;
            }
            if (line.equalsIgnoreCase("readyok")) {
                isReady = true;
                return true;
            }
        }

        return false;
    }

    private boolean isThinking() {
        synchronized (this) {
            return isThinking;
        }
    }

    private static final String squareNotation = "abcdefgh";

    private void waitForMove() throws IOException {
        String moveLine;

        // B for best move
        while ((moveLine = getLastLine()).charAt(0) != 'b') {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Move bMove = parseMove(moveLine);
        synchronized (this) {
            bestMove = bMove;
        }
    }

    private String getLastLine() throws IOException {
        String line;
        // B for best move
        while ((line = readLine()).charAt(0) != 'b' ) {}

        return line;
    }

    private Move parseMove(String engineLine) {
        String[] splitted = engineLine.split(" ");

        if (splitted.length < 2) {
            return null;
        }

        Vector from = new Vector(squareNotation.indexOf(splitted[1].charAt(0)), splitted[1].charAt(1) - '0' - 1);
        Vector to = new Vector(squareNotation.indexOf(splitted[1].charAt(2)), splitted[1].charAt(3) - '0' - 1);

        return new Move(board.getPiece(from), from, to);
    }
}