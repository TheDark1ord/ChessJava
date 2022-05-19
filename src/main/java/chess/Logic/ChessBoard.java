package chess.Logic;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessBoard.KingStatus.CheckState;
import chess.Logic.ChessPiece.Color;
import chess.Logic.ChessPiece.Name;
import chess.Moves.Move;
import chess.Moves.Castling.Side;

// This class is used to keep track of the state of the board,
// determining legal moves and
// Importing and exporting FEN strings
public class ChessBoard implements Iterable<ChessPiece> {

    public enum GameResult {
        NONE, WHITE_WON, BLACK_WON, DRAW
    };

    public GameResult gameResult = GameResult.NONE;

    public ChessBoard() {
        // All the variables are set up in the setPosition() function
    }

    public ChessBoard(String FEN) {
        setPosition(FEN);
    }

    @Override
    public Iterator<ChessPiece> iterator() {
        return chessBoardList.iterator();
    }

    public Move peekLastMove() {
        if (prevMoves.empty()) {
            return null;
        }
        return prevMoves.peek();
    }

    public Move popLastMove() {
        if (prevMoves.empty()) {
            return null;
        }

        return prevMoves.pop();
    }

    public boolean isInCheck() {
        return getStatus(whiteToMove).checkState != CheckState.NONE;
    }

    public ChessPiece getPiece(Vector pos) {
        return chessBoard[pos.y][pos.x];
    }

    public ChessPiece.Color getCurrentColor() {
        return whiteToMove ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
    }

    public KingStatus getStatus(ChessPiece.Color color) {
        return color == Color.WHITE ? WKingSt : BKingSt;
    }

    public KingStatus getStatus(boolean whiteToMove) {
        return whiteToMove ? WKingSt : BKingSt;
    }

    boolean isUnderAttack(Vector pos, ChessPiece.Color color) {
        if (color == ChessPiece.Color.WHITE) {
            return WKingSt.attackedSquares.get(pos.y * 8 + pos.x);
        } else {
            return BKingSt.attackedSquares.get(pos.y * 8 + pos.x);
        }
    }

    // Used to track how many times a certain position occured during that game
    private HashMap<PositionHash, Integer> posHashes;

    void checkRepetition() {
        PositionHash currentHash = new PositionHash(chessBoardList);
        // If a key does not exist, put one, else increment by one
        posHashes.merge(currentHash, 1, Integer::sum);
        if (posHashes.get(currentHash) >= 3) {
            gameResult = GameResult.DRAW;
        }
    }

    /// These variables describe board state, everything, that can be deduced
    /// from
    /// reading FEN string

    // Two different representations of a board
    // Array is used to acces pieces by position,
    // List is used to iterate through all of the pieces
    public ChessPiece[][] chessBoard;
    public List<ChessPiece> chessBoardList;

    private Stack<Move> prevMoves;
    Stack<BoardState> prevStates;
    // It is an array of 4 elements, it stores data on which side can castle,
    // Data is stored in this order: WShort, WLong, BShort, BLong
    // Does not account for temporary castling restrinctions, i.e. checks or blocks
    private boolean[] castlingRights;
    private boolean whiteToMove;
    // how many moves both players have made since the last pawn advance or piece
    // capture
    // Used to inforce 50-move draw rule (no capture has been made and no pawn has
    // been moved in the last fifty moves)
    int halfMoveClock;
    // Increments after the black move
    private int fullMoveClock;
    // Keeps track of double pawn moves (position behind the pawn)
    // If a pawn can attack the square, stored in this variable, then it is able
    // to make the french move
    public Vector enPassant;

    // Variables, that cannot be deduced when undoing the move
    class BoardState {
        public BoardState(boolean[] castlingRights, int halfMoveClock, Vector enPassant) {
            this.castlingRights = castlingRights.clone();
            this.halfMoveClock = halfMoveClock;
            this.enPassant = enPassant;
        }

        public boolean[] castlingRights;
        public int halfMoveClock;
        public Vector enPassant;
    }

    void addBoardState() {
        prevStates.add(new BoardState(castlingRights, halfMoveClock, enPassant));
    }

    // Load prev board state
    void loadBoardState() {
        BoardState prevState = prevStates.pop();
        
        castlingRights = prevState.castlingRights;
        halfMoveClock = prevState.halfMoveClock;
        enPassant = prevState.enPassant;
        gameResult = GameResult.NONE;
    }

    // Add a move to prev moves
    void trackMove(Move move) {
        prevMoves.add(move);
    }

    void resetHalfmoveClock() {
        halfMoveClock = 0;
    }

    void incrementClocks() {
        if (!whiteToMove)
            fullMoveClock++;
        halfMoveClock++;
        whiteToMove ^= true;
    }

    void decrementClocks() {
        if (whiteToMove) {
            fullMoveClock--;
        }
        whiteToMove ^= true;
    }

    void decrementPosHash() {
        PositionHash currentHash = new PositionHash(chessBoardList);
        if (!posHashes.containsKey(currentHash)) {
            return;
        }

        if (posHashes.get(currentHash) == 1) {
            posHashes.remove(currentHash);
        } else {
            posHashes.merge(currentHash, -1, Integer::sum);
        }
    }

    void updateCastlingRights(ChessPiece.Color color, Side side) {
        int index = color == Color.WHITE ? 0 : 2;
        index += side == Side.SHORT ? 0 : 1;
        castlingRights[index] = false;
    }

    void movePiece(Move move) {
        move.piece.pos = move.to;
        chessBoard[move.to.y][move.to.x] = move.piece;
        chessBoard[move.from.y][move.from.x] = null;
    }

    boolean[] castling() {
        return castlingRights;
    }

    boolean whiteToMove() {
        return whiteToMove;
    }

    int fullMoveClock() {
        return fullMoveClock;
    }

    Vector enPassant() {
        return enPassant;
    }

    // Update toBLockSq and Pinned pieces
    void updateKingStatus(KingStatus kStatus) {
        ChessPiece king = getPiece(kStatus.kingPos);

        if (king == null) {
            System.out.println("");
        }

        // Candidate varibles
        ChessPiece pinned = null;
        BitSet toBlock = new BitSet(64);

        for (int directionIndex = 0; directionIndex < 8; directionIndex++) {
            Vector pos = new Vector(king.pos);
            pinned = null;
            toBlock.clear();

            for (int i = 0; i < ChessPiece.iterationsToEdge(king.pos, directionIndex); i++) {
                pos.add(ChessPiece.directionOffsets[directionIndex]);

                ChessPiece piece = getPiece(pos);
                if (piece == null) {
                    if (pinned != null && !isUnderAttack(pos, king.color)) {
                        break;
                    }
                    toBlock.set(pos.y * 8 + pos.x);
                    continue;
                }
                if (piece.color == king.color) {
                    if (!isUnderAttack(pos, king.color)) {
                        break;
                    }
                    if (pinned != null) {
                        break;
                    } else {
                        pinned = piece;
                        continue;
                    }
                }

                if (piece.getName() == Name.QUEEN ||
                        (piece.getName() == Name.BISHOP && directionIndex >= 4) ||
                        (piece.getName() == Name.ROOK && directionIndex < 4)) {
                    if (pinned != null) {
                        pinned.pinnedDirection.add(ChessPiece.directionOffsets[directionIndex]);
                        kStatus.pinnedPieces.set(pinned.pos.y * 8 + pinned.pos.x);
                    } else {
                        toBlock.set(pos.y * 8 + pos.x);
                        kStatus.addAttacker();
                        kStatus.toBlockSq.or(toBlock);
                    }
                }
                break;
            }
        }

        king.generatePossibleMoves();
    }

    // It is all about checks and pins
    public static class KingStatus {
        // If a king is under a check, an unpinned piece can block this check
        // if a king is under a double check, no other piece can move
        public static enum CheckState {
            NONE, SINGLE, DOUBLE
        }

        KingStatus(Vector kingPos) {
            this.kingPos = kingPos;
            checkState = CheckState.NONE;
            pinnedPieces = new BitSet(64);
            toBlockSq = new BitSet(64);
            attackedSquares = new BitSet(64);
        }

        // Reset all variables except KingPos
        public void resetStatus() {
            checkState = CheckState.NONE;
            pinnedPieces = new BitSet(64);
            toBlockSq = new BitSet(64);
            attackedSquares = new BitSet(64);
        }

        public void addAttacker() {
            checkState = checkState == CheckState.NONE ? CheckState.SINGLE : CheckState.DOUBLE;
        }

        public Vector kingPos;
        // Pieces, that cannot move, because it will result
        // in opening the king to the check
        public BitSet pinnedPieces;
        // Keeps track of squres, that can be occupied to block the check
        // Not used in the case of double check
        public BitSet toBlockSq;
        public CheckState checkState;
        // Keep track of all the squares, attacked by enemy pieces
        public BitSet attackedSquares;
    }

    public KingStatus WKingSt;
    public KingStatus BKingSt;

    /// FEN
    // Load the position from the FEN string
    public void setPosition(String FEN) {
        chessBoard = new ChessPiece[8][8];
        chessBoardList = new LinkedList<>();
        posHashes = new HashMap<>();
        castlingRights = new boolean[4];

        posHashes = new HashMap<>();
        prevMoves = new Stack<>();
        prevStates = new Stack<>();

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
            for (int i = 0; i < line.length(); i++) {
                if (x > 7 || y < 0) {
                    throw new IllegalArgumentException("Incorrect FEN!");
                }

                char ch = line.charAt(i);
                if (Character.isDigit(ch)) {
                    x += ch - '0';
                    continue;
                }

                ChessPiece.Color color = Character.isUpperCase(ch) ? ChessPiece.Color.WHITE
                        : ChessPiece.Color.BLACK;
                ch = Character.toLowerCase(ch);

                ChessPiece newPiece;
                switch (ch) {
                    case 'k':
                        newPiece = new King(new Vector(x, y), color, this);
                        if (color == ChessPiece.Color.WHITE) {
                            WKingSt = new KingStatus(newPiece.pos);
                        } else {
                            BKingSt = new KingStatus(newPiece.pos);
                        }
                        break;
                    case 'q':
                        newPiece = new Queen(new Vector(x, y), color, this);
                        break;
                    case 'r':
                        newPiece = new Rook(new Vector(x, y), color, this);
                        break;
                    case 'b':
                        newPiece = new Bishop(new Vector(x, y), color, this);
                        break;
                    case 'n':
                        newPiece = new Knight(new Vector(x, y), color, this);
                        break;
                    case 'p':
                        newPiece = new Pawn(new Vector(x, y), color, this);
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
        if (data[1].equals("w")) {
            whiteToMove = true;
        } else if (data[1].equals("b")) {
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
                    castlingRights[0] = true;
                    break;
                case 'Q':
                    castlingRights[1] = true;
                    break;
                case 'k':
                    castlingRights[2] = true;
                    break;
                case 'q':
                    castlingRights[3] = true;
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect FEN!");
            }
        }
        /// Castling rights

        /// Other
        // Transform the position notation (a3, e4, etc.) into pos
        if (data[3].equals("-")) {
            enPassant = null;
        } else {
            enPassant = new Vector((int) (data[3].charAt(0) - 'a'), (int) (data[3].charAt(1) - '0'));
            if (enPassant.x > 7 || enPassant.x < 0 || enPassant.y > 7 || enPassant.y < 0) {
                throw new IllegalArgumentException("Incorrect FEN!");
            }
        }

        try {
            halfMoveClock = Integer.parseInt(data[4]);
            fullMoveClock = Integer.parseInt(data[5]);
        } catch (NumberFormatException nfEx) {
            throw new IllegalArgumentException("Incorrect FEN!");
        }
        /// Other

        WKingSt.resetStatus();
        BKingSt.resetStatus();

        for (ChessPiece piece : chessBoardList) {
            piece.generatePossibleMoves();
            getStatus(ChessPiece.invert(piece.color)).attackedSquares.or(piece.attackedSquares);
        }

        updateKingStatus(WKingSt);
        updateKingStatus(BKingSt);
    }

    private static final HashMap<ChessPiece.Name, Character> nameToChar = new HashMap<>() {
        {
            put(Name.KING, 'k');
            put(Name.QUEEN, 'q');
            put(Name.ROOK, 'r');
            put(Name.BISHOP, 'b');
            put(Name.KNIGHT, 'n');
            put(Name.PAWN, 'p');
        }
    };

    // Converts the position to FEN string
    public String toFEN() {
        StringBuilder retFen = new StringBuilder();

        /// Piece Placement
        int emptyConsec = 0;
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = chessBoard[y][x];

                if (piece == null) {
                    emptyConsec++;
                } else {
                    if (emptyConsec != 0) {
                        retFen.append(Integer.toString(emptyConsec));
                        emptyConsec = 0;
                    }

                    retFen.append(
                            piece.color == Color.BLACK ? nameToChar.get(piece.getName())
                                    : Character.toUpperCase(nameToChar.get(piece.getName())));
                }
            }
            if (emptyConsec != 0) {
                retFen.append(Integer.toString(emptyConsec));
                emptyConsec = 0;
            }
            retFen.append('/');
        }
        // Delete last / after the position
        retFen.deleteCharAt(retFen.length() - 1);
        retFen.append(' ');

        // Move
        retFen.append(whiteToMove ? 'w' : 'b');
        retFen.append(' ');

        // Castling
        if (castlingRights[0])
            retFen.append('K');
        if (castlingRights[1])
            retFen.append('Q');
        if (castlingRights[2])
            retFen.append('k');
        if (castlingRights[3])
            retFen.append('q');
        // If no castling is allowed
        if (retFen.charAt(retFen.length() - 1) == ' ')
            retFen.append('-');
        retFen.append(' ');

        // En passant
        if (enPassant == null) {
            retFen.append('-');
        } else {
            retFen.append(enPassant.toString());
        }
        retFen.append(' ');

        // Clocks
        retFen.append(String.valueOf(halfMoveClock) + " ");
        retFen.append(String.valueOf(fullMoveClock));

        return retFen.toString();
    }
}

// Used to check if the position has occured during the game
class PositionHash {
    PositionHash(List<ChessPiece> pieceList) {
        HashCodeBuilder builder = new HashCodeBuilder();
        for (ChessPiece piece : pieceList) {
            builder.append(piece.hashCode());
        }
        hashCode = builder.toHashCode();
    }

    private long hashCode;

    @Override
    public int hashCode() {
        return (int)hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof PositionHash)) {
            return false;
        }

        return this.hashCode == (((PositionHash) other).hashCode);
    }
}