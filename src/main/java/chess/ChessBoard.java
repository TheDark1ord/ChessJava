package chess;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import chess.ChessPiece.Name;

// TODO: add code to detect repetitions

// This class is used to keep track of the state of the board,
// determining legal moves and
// Importing and exporting FEN strings
public class ChessBoard {
    ChessBoard() {
        chessBoard = new ChessPiece[8][8];
        chessBoardList = new LinkedList<>();
    }

    // Load the position from the FEN string
    public void setPosition(String FEN) {
        // 0 - Piece Placement
        // 1 - Active color
        // 2 - Castling rights
        // 3 - En Passant targets
        // 4 - Halfmove clock
        // 5 - Fullmove clock
        String[] data = FEN.split(" ");
        if (data.length != 6) {
            throw new IllegalArgumentException("Incorrect FEN!");
        }

        /// Piece placement
        int x = 0, y = 7;
        for (String line : data[0].split("/")) {
            for (int i = 0; i < data[0].length(); i++) {
                if (x > 7 || y < 0) {
                    throw new IllegalArgumentException("Incorrect FEN!");
                }

                char ch = line.charAt(i);
                if (Character.isDigit(ch)) {
                    x += ch - '0';
                    continue;
                }

                Color color = Character.isUpperCase(ch) ? Color.WHITE : Color.BLACK;
                Character.toLowerCase(ch);

                ChessPiece newPiece;
                switch (ch) {
                    case 'k':
                        newPiece = new King(new Pos(x, y), color, this);
                        break;
                    case 'q':
                        newPiece = new Queen(new Pos(x, y), color, this);
                        break;
                    case 'r':
                        newPiece = new Rook(new Pos(x, y), color, this);
                        break;
                    case 'b':
                        newPiece = new Bishop(new Pos(x, y), color, this);
                        break;
                    case 'n':
                        newPiece = new Knight(new Pos(x, y), color, this);
                        break;
                    case 'p':
                        newPiece = new Pawn(new Pos(x, y), color, this);
                        break;
                    default:
                        throw new IllegalArgumentException("Incorrect FEN!");
                }
                chessBoard[y][x] = newPiece;
                chessBoardList.add(newPiece);

                x++;
            }
            y--;
            x = 0;
        }
        /// Piece placement

        /// Active color
        if (data[1] == "w") {
            whiteToMove = true;
        } else if (data[1] == "b") {
            whiteToMove = false;
        } else {
            throw new IllegalArgumentException("Incorrect FEN!");
        }
        /// Active color

        /// Castling rights
        if (data[2].length() > 4) {
            throw new IllegalArgumentException("Incorrect FEN!");
        }
        for (int i = 0; i < data[2].length(); i++) {
            switch (data[2].charAt(i)) {
                case '-':
                    break;
                case 'K':
                    castling[0] = true;
                    break;
                case 'Q':
                    castling[1] = true;
                    break;
                case 'k':
                    castling[2] = true;
                    break;
                case 'q':
                    castling[3] = true;
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect FEN!");
            }
        }
        /// Castling rights

        /// Other
        // Transform the position notation (a3, e4, etc.) into pos
        enPassant = new Pos((int)(data[3].charAt(0) - 'a'), (int)(data[43.charAt(1) - '0'));
        if (enPassant.x() > 7 || enPassant.x() < 0 || enPassant.y() > 7 || enPassant.y() < 0) {
            throw new IllegalArgumentException("Incorrect FEN!");
        }

        try {
            halfMoveClock = Integer.parseInt(data[4]);
            fullMoveClock = Integer.parseInt(data[5]);
        } catch(NumberFormatException nfEx) {
            throw new IllegalArgumentException("Incorrect FEN!");
        }
        /// Other

        generateKingStatus();
    }

    // Converts the position to FEN string
    public String toFEN() {
        return "";
    }

    // Checks if this move is possible, then updates some variables like
    // chess board, piece position, clocks, etc.
    public boolean makeAMove(Pos from, Pos to) {
        ChessPiece piece = getPiece(from);
        // King status of the current side
        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;
        Color currentColor = whiteToMove ? Color.WHITE : Color.BLACK;
        if (piece == null)
            return false;

        if (piece.color != currentColor) {
            return false;
        }

        if (kSt.kingPos == from) {
            piece.generatePossibleMoves();
            if (!piece.possibleMoves.contains(to)) {
                return false;
            }
            if (isUnderAttack(from, currentColor)) {
                return false;
            }

            // Check for castling
            // TODO

            // If a king moved castling is no longer possible
            kSt.kingPos = to;
            castling[whiteToMove ? 0 : 2] = false;
            castling[whiteToMove ? 1 : 3] = false;
        } else {
            // In case of the double check only a king can move
            if (kSt.checkState == KingStatus.CheckState.DOUBLE) {
                return false;
            }
            if (kSt.pinnedPieces.contains(piece)) {
                return false;
            }
            piece.generatePossibleMoves();
            if (!piece.possibleMoves.contains(to)) {
                return false;
            }
            // If a king is in check, you should move the king or block the check
            if (kSt.checkState == KingStatus.CheckState.SINGLE && !kSt.toBlockSq.contains(to)) {
                return false;
            }
        }

        // All checks out, make a move

        // Check for capture
        if (chessBoard[to.y()][to.x()] != null) {
            ChessPiece capturedPiece = chessBoard[to.y()][to.x()];

            // Update castling rights if a rook was captured
            if (capturedPiece.getName() == Name.W_ROOK || capturedPiece.getName() == Name.B_ROOK) {
                if (capturedPiece.pos.x() == 0) {
                    // Long castle
                    castling[whiteToMove ? 1 : 3] = false;
                } else if (capturedPiece.pos.x() == 7) {
                    // Short castle
                    castling[whiteToMove ? 0 : 2] = false;
                }
            }
            chessBoardList.remove(capturedPiece);
            // Reset clock (it will be later incremented to 0)
            halfMoveClock = -1;
        }
        piece.pos = to;
        chessBoard[to.y()][to.x()] = piece;
        chessBoard[from.y()][from.x()] = null;

        // If a rook moved, update castling rights
        if (piece.getName() == Name.W_ROOK || piece.getName() == Name.B_ROOK) {
            if (from.x() == 0) {
                // Long castle
                castling[whiteToMove ? 1 : 3] = false;
            } else if (from.x() == 7) {
                // Short castle
                castling[whiteToMove ? 0 : 2] = false;
            }
        }

        if (!whiteToMove)
            fullMoveClock++;
        halfMoveClock++;
        whiteToMove ^= true;

        generateKingStatus();

        return true;
    }

    // Generates KingStatus data for current side to move
    public void generateKingStatus() {
        // TODO
    }

    public ChessPiece getPiece(Pos pos) {
        return chessBoard[pos.y()][pos.x()];
    }

    public boolean isUnderAttack(Pos pos, Color color) {
        // TODO
        return true;
    }

    // This variables describe board state, everything, that can be deduced from
    // reading FEN string
    // Two different representations of a board
    // Array is used to acces pieces by position,
    // List is used to iterate through all of the pieces
    public ChessPiece[][] chessBoard;
    private List<ChessPiece> chessBoardList;
    // It is an array of 4 elements, it stores data on which side can castle,
    // Data is stored in this order: WShort, WLong, BShort, BLong
    // Does not account for temporary castling restrinctions, i.e. checks or blocks
    private boolean[] castling;
    private boolean whiteToMove;
    // how many moves both players have made since the last pawn advance or piece
    // capture
    // Used to inforce 50-move draw rule (no capture has been made and no pawn has
    // been moved in the last fifty moves)
    private int halfMoveClock;
    // Increments after the black move
    private int fullMoveClock;
    // Keeps track of double pawn moves (position behind the pawn)
    // If a pawn can attack the square, stored in this variable, then it is able
    // to make the french move
    private Pos enPassant;

    public Iterator<ChessPiece> getIterator() {
        return chessBoardList.iterator();
    }

    public boolean[] castling() {
        return castling;
    }

    public boolean whiteToMove() {
        return whiteToMove;
    }

    public int fullMoveClock() {
        return fullMoveClock;
    }

    public Pos enPassant() {
        return enPassant;
    }

    // It is all about checks and pins
    private static class KingStatus {
        public static enum CheckState {
            NONE, SINGLE, DOUBLE
        }

        KingStatus(Pos kingPos) {
            this.kingPos = kingPos;
            pinnedPieces = new HashSet<>();
            toBlockSq = new HashSet<>();
            checkState = CheckState.NONE;
        }

        public Pos kingPos;
        // Pieces, that cannot move, because it will result
        // in opening the king to the check
        public Set<ChessPiece> pinnedPieces;
        // Keeps track of squres, that can be occupied to block the check
        // Not used in the case of double check
        public Set<Pos> toBlockSq;
        // Keep track of all the squares, attacked by black pieces
        public CheckState checkState;
    }

    private KingStatus WKingSt;
    private KingStatus BKingSt;
}
