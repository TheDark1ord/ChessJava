package chess;

import java.util.HashMap;
import java.util.List;

// This class is used to keep track of the state of the board,
// determining legal moves and
// Importing and exporting FEN strings
public class ChessBoard {
    ChessBoard() {
        chessBoard = new HashMap<>();
    }

    // Load the position from the FEN string
    public void setPosition(String FEN) {

    }

    // Converts the position to FEN string
    @Override
    public String toString() {
        return "";
    }

    public ChessPiece getPiece(Pos pos) {
        return chessBoard.getOrDefault(pos, null);
    }

    // This variables describe board state, everything, that can be deduced from reading FEN string
    private HashMap<Pos, ChessPiece> chessBoard;
    // It is an array of 4 elements, it stores data on which side can castle,
    // Data is stored in this order: WShort, WLong, BShort, BLong
    // Does not account for temporary castling restrinctions, i.e. checks or blocks
    private boolean[] castling;
    private boolean whiteToMove;
    // how many moves both players have made since the last pawn advance or piece capture
    // Used to inforce 50-move draw rule (no capture has been made and no pawn has been moved in the last fifty moves)
    private int halfMoveClock;
    // Increments after the black move
    private int fullMoveClock;
    // Keeps track of double pawn moves (position behind the pawn)
    // If a pawn can attack the square, stored in this variable, then it is able
    // to make the french move
    private Pos enPassant;

    // It is all about checks and pins
    private static class KingStatus {
        public static enum CheckState {
            NONE, SINGLE, DOUBLE
        }

        public Pos kingPos;
        // Pieces, that cannot move, because it will result
        // in opening the king to the check
        public List<ChessPiece> pinnedPieces;
        // Keeps track of squres, that can be occupied to block the check
        // Not used in the case of double check
        public List<Pos> toBlockSq;
        // Keep track of all the squares, attacked by black pieces
        public boolean[][] attackedSquares;
        public CheckState checkState;
    }

    private KingStatus WKingSt;
    private KingStatus BKingSt;
}
