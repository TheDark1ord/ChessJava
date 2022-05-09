package chess;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.ChessBoard.KingStatus.CheckState;
import chess.ChessPiece.Name;

// TODO: add code to detect repetitions

// This class is used to keep track of the state of the board,
// determining legal moves and
// Importing and exporting FEN strings
public class ChessBoard implements Iterable<ChessPiece> {
    public enum GameResult {NONE, WHITE_WON, BLACK_WON, DRAW};
    public GameResult gameResult = GameResult.NONE;

    ChessBoard() {
        // All the variables are set up in the setPosition() function
    }

    ChessBoard(String FEN) {
        setPosition(FEN);
    }

    ChessBoard(ChessBoard other) {
        this.gameResult = other.gameResult;
        this.chessBoard = other.chessBoard;
        this.chessBoardList = other.chessBoardList;
        this.castling = other.castling;
        this.whiteToMove = other.whiteToMove;
        this.halfMoveClock = other.halfMoveClock;
        this.fullMoveClock = other.fullMoveClock;
        this.enPassant = other.enPassant;
        this.WKingSt = other.WKingSt;
        this.BKingSt = other.BKingSt;
    }

    // Checks if this move is possible, then updates some variables like
    // chess board, piece position, clocks, etc.
    public boolean makeAMove(Pos from, Pos to) {
        ChessPiece piece = getPiece(from);
        if (!getPieceMoves(piece).contains(to))
            return false;
        if (piece.getName() == Name.PAWN)
            halfMoveClock = -1;

        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;
        if (kSt.kingPos == from) {
            castling(kSt, to);

            // If a king moved castling is no longer possible
            kSt.kingPos = to;
            castling[whiteToMove ? 0 : 2] = false;
            castling[whiteToMove ? 1 : 3] = false;
        }

        capture(piece, from, to);
        doubleMove(piece, from, to);
        // If a rook moved, update castling rights
        if (piece.getName() == Name.ROOK) {
            if (from.x == 0) {
                // Long castle
                castling[whiteToMove ? 1 : 3] = false;
            } else if (from.x == 7) {
                // Short castle
                castling[whiteToMove ? 0 : 2] = false;
            }
        }

        if (!whiteToMove)
            fullMoveClock++;
        halfMoveClock++;
        whiteToMove ^= true;

        calculatePossition();
        endGame();
        System.out.println(gameResult);
        return true;
    }

    public List<Pos> getPieceMoves(ChessPiece piece) {
        if (gameResult != GameResult.NONE) {
            return new LinkedList<Pos>();
        }

        // Null and color
        ChessColor currentColor = whiteToMove ? ChessColor.WHITE : ChessColor.BLACK;
        if (piece == null || piece.color != currentColor)
            return new LinkedList<Pos>();

        // Check and pin
        KingStatus kSt = whiteToMove ? WKingSt : BKingSt;
        if (kSt.checkState == KingStatus.CheckState.DOUBLE ||
                kSt.pinnedPieces.contains(piece))
            return new LinkedList<Pos>();

        List<Pos> retArr;
        // If a king is under check, add all moves, that block that check
        if (piece.getName() == ChessPiece.Name.KING) {
            retArr = piece.possibleMoves.stream()
                    .filter((Pos pos) -> {
                        return !isUnderAttack(pos, piece.color);
                    })
                    .collect(Collectors.toList());
        } else if (kSt.checkState == KingStatus.CheckState.SINGLE) {
            retArr = piece.possibleMoves.stream()
                    .filter(kSt.toBlockSq::contains)
                    .collect(Collectors.toList());
        } else {
            retArr = piece.possibleMoves;
        }

        return retArr;
    }

    public Stream<Pos> getAllMoves() {
        Stream<Pos> retStream = Stream.empty();
        for (ChessPiece piece : chessBoardList) {
            retStream = Stream.concat(retStream, getPieceMoves(piece).stream());
        }
        return retStream;
    }

    // Check if the move is castling to move a rook
    private void castling(KingStatus kSt, Pos to) {
        if (to.equals(new Pos(kSt.kingPos.x - 2, kSt.kingPos.y))) {
            // Castle short
            Pos rookTo = new Pos(kSt.kingPos.x - 1, kSt.kingPos.y);
            chessBoard[kSt.kingPos.y][0].setPos(rookTo);
            chessBoard[rookTo.y][rookTo.x] = chessBoard[kSt.kingPos.y][0];
            chessBoard[kSt.kingPos.y][0] = null;
        } else if (to.equals(new Pos(kSt.kingPos.x + 2, kSt.kingPos.y))) {
            // Castle long
            Pos rookTo = new Pos(kSt.kingPos.x + 1, kSt.kingPos.y);
            chessBoard[kSt.kingPos.y][7].setPos(rookTo);
            chessBoard[rookTo.y][rookTo.x] = chessBoard[kSt.kingPos.y][7];
            chessBoard[kSt.kingPos.y][7] = null;
        }
    }

    // Check for capture
    private void capture(ChessPiece piece, Pos from, Pos to) {
        if (chessBoard[to.y][to.x] != null) {
            ChessPiece capturedPiece = chessBoard[to.y][to.x];

            // Update castling rights if a rook was captured
            if (capturedPiece.getName() == Name.ROOK) {
                if (capturedPiece.pos.x == 0) {
                    // Long castle
                    castling[whiteToMove ? 1 : 3] = false;
                } else if (capturedPiece.pos.x == 7) {
                    // Short castle
                    castling[whiteToMove ? 0 : 2] = false;
                }
            }
            chessBoardList.remove(capturedPiece);
            // Reset clock (it will be later incremented to 0)
            halfMoveClock = -1;

            // Check for enPassant capture
        } else if (enPassant != null && enPassant.equals(to)) {
            ChessPiece capturedPiece = chessBoard[piece.pos.y][to.x];

            chessBoardList.remove(capturedPiece);
            chessBoard[capturedPiece.pos.y][capturedPiece.pos.y] = null;
            // Reset clock (it will be later incremented to 0)
            halfMoveClock = -1;
        }
        piece.pos = to;
        chessBoard[to.y][to.x] = piece;
        chessBoard[from.y][from.x] = null;
    }

    // If a pawn made double move, update enPassant
    private void doubleMove(ChessPiece piece, Pos from, Pos to) {
        // If the pawn is white forward move will increase pos.y
        // If the pawn is black forward move will decrease pos.y
        int forMov = piece.color == ChessColor.WHITE ? 1 : -1;

        if (piece.getName() != Name.PAWN) {
            enPassant = null;
            return;
        }

        // Pawn made a double move
        if (new Pos(from.x, from.y + forMov * 2).equals(to)) {
            // Mark the square behind the pawn
            enPassant = new Pos(piece.pos.x, from.y + forMov);
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
        }

        // Fifty move rule
        if (halfMoveClock == 100) {
            gameResult = GameResult.DRAW;
        }

        /// Repetition
        PositionHash currentHash = new PositionHash(chessBoardList);
        // If a key does not exist, put one, else increment by one
        posHashes.merge(currentHash, 1, Integer::sum);
        if (posHashes.get(currentHash) >= 3) {
            gameResult = GameResult.DRAW;
        }
    }

    public ChessPiece getPiece(Pos pos) {
        return chessBoard[pos.y][pos.x];
    }

    boolean isUnderAttack(Pos pos, ChessColor color) {
        if (color == ChessColor.WHITE) {
            return WKingSt.attackedSquares.get(pos.y * 8 + pos.x);
        } else {
            return BKingSt.attackedSquares.get(pos.y * 8 + pos.x);
        }
    }

    /// ---These variables describe board state, everything, that can be deduced
    /// from
    /// ---reading FEN string
    // Two different representations of a board
    // Array is used to acces pieces by position,
    // List is used to iterate through all of the pieces
    private ChessPiece[][] chessBoard;
    private List<ChessPiece> chessBoardList;
    // Used to track how many times a certain position occured during that game
    private HashMap<PositionHash, Integer> posHashes;
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

    @Override
    public Iterator<ChessPiece> iterator() {
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

    // Calculate moves and update KingStatus
    private void calculatePossition() {
        WKingSt.resetStatus();
        BKingSt.resetStatus();

        for (ChessPiece piece : chessBoardList) {
            piece.generatePossibleMoves();
            (piece.color == ChessColor.WHITE ? BKingSt : WKingSt).attackedSquares.or(piece.getMoveMap());
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
        List<Pos> toBlock = new LinkedList<>();

        // Just to make a code smaller
        BiFunction<Pos, Boolean, Boolean> checkSquare = (Pos pos, Boolean diagonal) -> {
            ChessPiece piece = getPiece(pos);

            if (piece == null) {
                toBlock.add(pos);
                return false;
            }
            if (piece.color == king.color) {
                if (pinnedWrapper.pinned == null) {
                    pinnedWrapper.pinned = piece;
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
            if (checkSquare.apply(new Pos(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x - 1, y = kStatus.kingPos.y + 1; x > 0 && y < 8; x--, y++) {
            if (checkSquare.apply(new Pos(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x + 1, y = kStatus.kingPos.y - 1; x < 8 && y > 0; x++, y--) {
            if (checkSquare.apply(new Pos(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = kStatus.kingPos.x - 1, y = kStatus.kingPos.y - 1; x > 0 && y > 0; x--, y--) {
            if (checkSquare.apply(new Pos(x, y), true))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();

        // Check straigh lines
        for (int x = king.pos.x + 1; x < 8; x++) {
            if (checkSquare.apply(new Pos(x, king.pos.y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int x = king.pos.x - 1; x >= 0; x--) {
            if (checkSquare.apply(new Pos(x, king.pos.y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int y = king.pos.y + 1; y < 8; y++) {
            if (checkSquare.apply(new Pos(king.pos.x, y), false))
                break;
        }
        pinnedWrapper.pinned = null;
        toBlock.clear();
        for (int y = king.pos.y - 1; y >= 0; y--) {
            if (checkSquare.apply(new Pos(king.pos.x, y), false))
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

        KingStatus(Pos kingPos) {
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

        public Pos kingPos;
        // Pieces, that cannot move, because it will result
        // in opening the king to the check
        public Set<ChessPiece> pinnedPieces;
        // Keeps track of squres, that can be occupied to block the check
        // Not used in the case of double check
        public Set<Pos> toBlockSq;
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
        castling = new boolean[4];

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

                ChessColor color = Character.isUpperCase(ch) ? ChessColor.WHITE : ChessColor.BLACK;
                ch = Character.toLowerCase(ch);

                ChessPiece newPiece;
                switch (ch) {
                    case 'k':
                        newPiece = new King(new Pos(x, y), color, this);
                        if (color == ChessColor.WHITE) {
                            WKingSt = new KingStatus(newPiece.pos);
                        } else {
                            BKingSt = new KingStatus(newPiece.pos);
                        }
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
        if (data[3].equals("-")) {
            enPassant = null;
        } else {
            enPassant = new Pos((int) (data[3].charAt(0) - 'a'), (int) (data[3].charAt(1) - '0'));
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

        return this.hashCode == (((PositionHash)other).hashCode());
    }
}