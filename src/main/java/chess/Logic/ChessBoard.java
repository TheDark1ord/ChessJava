package chess.Logic;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessBoard.KingStatus.CheckState;
import chess.Logic.ChessPiece.Name;
import chess.Moves.Castling;
import chess.Moves.Move;
import chess.Moves.Promotion;
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

    // Checks if this move is possible, then updates some variables like
    // chess board, piece position, clocks, etc.
    public boolean makeAMove(Move move) {
        if (!getPieceMoves(move.piece).contains(move))
            return false;
        prevStates.add(new BoardState(castlingRights, halfMoveClock, enPassant));

        if (move.piece.getName() == Name.PAWN)
            halfMoveClock = -1;

        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;
        Castling castling = null;
        if (move.piece.getName() == Name.KING) {
            // If a king moved castling is no longer possible
            castling = castle(move);

            kSt.kingPos = move.to;
            castlingRights[whiteToMove ? 0 : 2] = false;
            castlingRights[whiteToMove ? 1 : 3] = false;
        } else if (move.piece.getName() == Name.ROOK) {
            // If a rook moved, update castling rights
            if (move.from.x == 0) {
                // Long castle
                castlingRights[whiteToMove ? 1 : 3] = false;
            } else if (move.from.x == 7) {
                // Short castle
                castlingRights[whiteToMove ? 0 : 2] = false;
            }
        }

        if (castling != null) {
            prevMoves.add(castling);
        } else {
            doubleMove(move);
            // If a capture was made, then the move was already added
            if (!capture(move)) {
                prevMoves.add(move);
            }
        }

        move.piece.pos = move.to;
        chessBoard[move.to.y][move.to.x] = move.piece;
        chessBoard[move.from.y][move.from.x] = null;

        if (!whiteToMove)
            fullMoveClock++;
        halfMoveClock++;
        whiteToMove ^= true;

        calculatePossition();
        endGame();
        return true;
    }

    public void undoMove() {
        if (prevMoves.isEmpty()) {
            return;
        }

        Move toUndo = prevMoves.pop();
        BoardState prevState = prevStates.pop();

        posHashes.remove(new PositionHash(chessBoardList));

        castlingRights = prevState.castlingRights;
        halfMoveClock = prevState.halfMoveClock;
        enPassant = prevState.enPassant;
        gameResult = GameResult.NONE;

        if (whiteToMove) {
            fullMoveClock--;
        }
        whiteToMove ^= true;

        if (toUndo instanceof Promotion) {
            Promotion prom = (Promotion) toUndo;
            // TODO

            calculatePossition();
            return;
        } else if (toUndo instanceof Castling) {
            Castling cstl = (Castling) toUndo;

            if (cstl.side == Side.SHORT) {
                chessBoard[toUndo.from.y][7] = chessBoard[toUndo.from.y][toUndo.from.x + 1];
                chessBoard[toUndo.from.y][toUndo.from.x + 1] = null;
                chessBoard[toUndo.from.y][7].pos = new Vector(7, toUndo.from.y);
            } else {
                chessBoard[toUndo.from.y][0] = chessBoard[toUndo.from.y][toUndo.from.x - 1];
                chessBoard[toUndo.from.y][toUndo.from.x - 1] = null;
                chessBoard[toUndo.from.y][0].pos = new Vector(0, toUndo.from.y);
            }
            // Essentially a fallthrough
        }
        if (toUndo.piece.getName() == Name.KING) {
            (whiteToMove ? WKingSt : BKingSt).kingPos = new Vector(toUndo.from.x, toUndo.from.y);
        }
        chessBoard[toUndo.to.y][toUndo.to.x] = toUndo.captured;
        chessBoard[toUndo.from.y][toUndo.from.x] = toUndo.piece;
        toUndo.piece.pos = toUndo.from;

        if (toUndo.captured != null) {
            chessBoardList.add(toUndo.captured);
        }

        calculatePossition();
    }

    public Move getLastMove() {
        return prevMoves.peek();
    }

    public boolean isInCheck() {
        return (whiteToMove ? WKingSt : BKingSt).checkState != CheckState.NONE;
    }

    public List<Move> getPieceMoves(ChessPiece piece) {
        if (gameResult != GameResult.NONE) {
            return new LinkedList<Move>();
        }

        // Null and color
        ChessPiece.ChessColor currentColor = whiteToMove ? ChessPiece.ChessColor.WHITE : ChessPiece.ChessColor.BLACK;
        if (piece == null || piece.color != currentColor)
            return new LinkedList<Move>();

        // Check and pin
        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;
        if (kSt.checkState == KingStatus.CheckState.DOUBLE ||
                kSt.pinnedPieces.contains(piece))
            return new LinkedList<Move>();

        List<Move> retArr;
        // If a king is under check, add all moves, that block that check
        if (piece.getName() == ChessPiece.Name.KING) {
            retArr = piece.possibleMoves.stream()
                    .filter((Move move) -> {
                        return !isUnderAttack(move.to, piece.color);
                    })
                    .collect(Collectors.toList());
        } else if (kSt.checkState == KingStatus.CheckState.SINGLE) {
            retArr = piece.possibleMoves.stream()
                    .filter((Move move) -> {
                        return kSt.toBlockSq.contains(move.to);
                    })
                    .collect(Collectors.toList());
        } else {
            retArr = piece.possibleMoves;
        }

        return retArr;
    }

    public Stream<Move> getAllMoves() {
        Stream<Move> retStream = Stream.empty();
        for (ChessPiece piece : chessBoardList) {
            retStream = Stream.concat(retStream, getPieceMoves(piece).stream());
        }
        return retStream;
    }

    // Check if the move is castling to move a rook
    private Castling castle(Move move) {
        if (move.to.equals(new Vector(move.from.x - 2, move.from.y))) {
            // Castle long
            Vector rookTo = new Vector(move.from.x - 1, move.from.y);
            chessBoard[move.from.y][0].setPos(rookTo);
            chessBoard[rookTo.y][rookTo.x] = chessBoard[move.from.y][0];
            chessBoard[move.from.y][0] = null;

            return new Castling(move.piece, move.from, move.to, Castling.Side.LONG);
        } else if (move.to.equals(new Vector(move.from.x + 2, move.from.y))) {
            // Castle short

            Vector rookTo = new Vector(move.from.x + 1, move.from.y);
            chessBoard[move.from.y][7].setPos(rookTo);
            chessBoard[rookTo.y][rookTo.x] = chessBoard[move.from.y][7];
            chessBoard[move.from.y][7] = null;

            return new Castling(move.piece, move.from, move.to, Castling.Side.SHORT);
        }

        return null;
    }

    // Check for capture
    private boolean capture(Move move) {
        if (chessBoard[move.to.y][move.to.x] != null) {
            ChessPiece capturedPiece = chessBoard[move.to.y][move.to.x];

            prevMoves.add(new Move(move.piece, move.from, move.to, capturedPiece));

            // Update castling rights if a rook was captured
            if (capturedPiece.getName() == Name.ROOK) {
                if (capturedPiece.pos.x == 0) {
                    // Long castle
                    castlingRights[whiteToMove ? 1 : 3] = false;
                } else if (capturedPiece.pos.x == 7) {
                    // Short castle
                    castlingRights[whiteToMove ? 0 : 2] = false;
                }
            }
            chessBoardList.remove(capturedPiece);
            // Reset clock (it will be later incremented to 0)
            halfMoveClock = -1;
            return true;

            // Check for enPassant capture
        } else if (enPassant != null && enPassant.equals(move.to)) {
            ChessPiece capturedPiece = chessBoard[move.piece.pos.y][move.to.x];

            prevMoves.add(new Move(move.piece, move.from, move.to, capturedPiece));

            chessBoardList.remove(capturedPiece);
            chessBoard[capturedPiece.pos.y][capturedPiece.pos.y] = null;
            // Reset clock (it will be later incremented to 0)
            halfMoveClock = -1;

            return true;
        }
        return false;
    }

    // If a pawn made double move, update enPassant
    private void doubleMove(Move move) {
        // If the pawn is white forward move will increase pos.y
        // If the pawn is black forward move will decrease pos.y
        int forMov = move.piece.color == ChessPiece.ChessColor.WHITE ? 1 : -1;

        if (move.piece.getName() != Name.PAWN) {
            enPassant = null;
            return;
        }

        // Pawn made a double move
        if (new Vector(move.from.x, move.from.y + forMov * 2).equals(move.to)) {
            // Mark the square behind the pawn
            enPassant = new Vector(move.piece.pos.x, move.from.y + forMov);
        } else {
            enPassant = null;
        }
    }

    // Chech if the game should conclude
    private void endGame() {
        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;

        // If no moves are possible
        if (getAllMoves().count() == 0) {
            if (kSt.checkState != CheckState.NONE) {
                gameResult = whiteToMove ? GameResult.BLACK_WON : GameResult.WHITE_WON;
            } else {
                gameResult = GameResult.DRAW;
            }
            return;
        }

        // Fifty move rule
        if (halfMoveClock == 100) {
            gameResult = GameResult.DRAW;
            return;
        }

        /// Repetition
        PositionHash currentHash = new PositionHash(chessBoardList);
        // If a key does not exist, put one, else increment by one
        posHashes.merge(currentHash, 1, Integer::sum);
        if (posHashes.get(currentHash) >= 3) {
            gameResult = GameResult.DRAW;
        }
    }

    public ChessPiece getPiece(Vector pos) {
        return chessBoard[pos.y][pos.x];
    }

    public ChessPiece.ChessColor getCurrentColor() {
        return whiteToMove ? ChessPiece.ChessColor.WHITE : ChessPiece.ChessColor.BLACK;
    }

    boolean isUnderAttack(Vector pos, ChessPiece.ChessColor color) {
        if (color == ChessPiece.ChessColor.WHITE) {
            return WKingSt.attackedSquares.get(pos.y * 8 + pos.x);
        } else {
            return BKingSt.attackedSquares.get(pos.y * 8 + pos.x);
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
    // Used to track how many times a certain position occured during that game
    private HashMap<PositionHash, Integer> posHashes;

    private Stack<Move> prevMoves;
    private Stack<BoardState> prevStates;
    // It is an array of 4 elements, it stores data on which side can castle,
    // Data is stored in this order: WShort, WLong, BShort, BLong
    // Does not account for temporary castling restrinctions, i.e. checks or blocks
    private boolean[] castlingRights;
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
    public Vector enPassant;

    // Variables, that cannot be deduced when undoing the move
    private class BoardState {
        public BoardState(boolean[] castlingRights, int halfMoveClock, Vector enPassant) {
            this.castlingRights = castlingRights.clone();
            this.halfMoveClock = halfMoveClock;
            this.enPassant = enPassant;
        }

        public boolean[] castlingRights;
        public int halfMoveClock;
        public Vector enPassant;
    }

    @Override
    public Iterator<ChessPiece> iterator() {
        return chessBoardList.iterator();
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

    // Calculate moves and update KingStatus
    private void calculatePossition() {
        WKingSt.resetStatus();
        BKingSt.resetStatus();

        for (ChessPiece piece : chessBoardList) {
            piece.generatePossibleMoves();

            // Add squares, that is attacked by that square to list of all pieces
            (piece.color == ChessPiece.ChessColor.WHITE ? BKingSt : WKingSt).attackedSquares.or(piece.getMoveMap());
        }
        updateKingStatus(WKingSt);
        updateKingStatus(BKingSt);
    }

    // Update toBLockSq and Pinned pieces
    private void updateKingStatus(KingStatus kStatus) {
        ChessPiece king = getPiece(kStatus.kingPos);

        // Candidate varibles
        // To use in lambdas
        var pinnedWrapper = new Object() {
            ChessPiece pinned = null;
        };
        List<Vector> toBlock = new LinkedList<>();

        // Just to make a code smaller
        BiFunction<Vector, Boolean, Boolean> checkSquare = (Vector pos, Boolean diagonal) -> {
            ChessPiece piece = getPiece(pos);

            if (piece == null) {
                toBlock.add(pos);
                return false;
            }
            if (piece.color == king.color) {
                if (pinnedWrapper.pinned == null) {
                    pinnedWrapper.pinned = piece;
                } else {
                    return true;
                }
                return false;
            }

            if (piece.getName() == Name.QUEEN ||
                    (piece.getName() == Name.BISHOP && diagonal) ||
                    (piece.getName() == Name.ROOK && diagonal)) {
                if (pinnedWrapper.pinned != null) {
                    kStatus.pinnedPieces.add(pinnedWrapper.pinned);
                } else {
                    toBlock.add(pos);
                    kStatus.addAttacker();
                    kStatus.toBlockSq.addAll(toBlock);
                }
            }
            return true;
        };

        // Check diagonals
        for (int x = kStatus.kingPos.x + 1, y = kStatus.kingPos.y + 1; x < 8 && y < 8; x++, y++) {
            if (checkSquare.apply(new Vector(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x - 1, y = kStatus.kingPos.y + 1; x >= 0 && y < 8; x--, y++) {
            if (checkSquare.apply(new Vector(x, y), true))
                break;
        }

        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x + 1, y = kStatus.kingPos.y - 1; x < 8 && y >= 0; x++, y--) {
            if (checkSquare.apply(new Vector(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x - 1, y = kStatus.kingPos.y - 1; x >= 0 && y >= 0; x--, y--) {
            if (checkSquare.apply(new Vector(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();

        // Check straigh lines
        for (int x = king.pos.x + 1; x < 8; x++) {
            if (checkSquare.apply(new Vector(x, king.pos.y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = king.pos.x - 1; x >= 0; x--) {
            if (checkSquare.apply(new Vector(x, king.pos.y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int y = king.pos.y + 1; y < 8; y++) {
            if (checkSquare.apply(new Vector(king.pos.x, y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int y = king.pos.y - 1; y >= 0; y--) {
            if (checkSquare.apply(new Vector(king.pos.x, y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
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
            pinnedPieces = new HashSet<>();
            toBlockSq = new HashSet<>();
            checkState = CheckState.NONE;
            attackedSquares = new BitSet(64);
        }

        // Reset all variables except KingPos
        public void resetStatus() {
            pinnedPieces = new HashSet<>();
            toBlockSq = new HashSet<>();
            checkState = CheckState.NONE;
            attackedSquares = new BitSet(64);
        }

        public void addAttacker() {
            checkState = checkState == CheckState.NONE ? CheckState.SINGLE : CheckState.DOUBLE;
        }

        public Vector kingPos;
        // Pieces, that cannot move, because it will result
        // in opening the king to the check
        public Set<ChessPiece> pinnedPieces;
        // Keeps track of squres, that can be occupied to block the check
        // Not used in the case of double check
        public Set<Vector> toBlockSq;
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

                ChessPiece.ChessColor color = Character.isUpperCase(ch) ? ChessPiece.ChessColor.WHITE
                        : ChessPiece.ChessColor.BLACK;
                ch = Character.toLowerCase(ch);

                ChessPiece newPiece;
                switch (ch) {
                    case 'k':
                        newPiece = new King(new Vector(x, y), color, this);
                        if (color == ChessPiece.ChessColor.WHITE) {
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

        calculatePossition();
    }

    // Converts the position to FEN string
    public String toFEN() {
        return "";
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

    private int hashCode;

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof PositionHash)) {
            return false;
        }

        return this.hashCode == (((PositionHash) other).hashCode());
    }
}