package chess.Logic;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Moves.Move;
import chess.Moves.Promotion;
import chess.Moves.Promotion.PromoteTo;

public abstract class ChessPiece {
    public enum Color {
        WHITE,
        BLACK
    }

    static public Color invert(Color color) {
        return color == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    protected final Color color;
    protected Vector pos;

    protected ChessBoard parentBoard;

    public List<Move> possibleMoves;
    public BitSet attackedSquares;

    // If a piece is relativly pinned(it can move along the direction of a pin)
    // It is a list it case of a double pin
    public List<Vector> pinnedDirection;

    ChessPiece(Vector pos, Color color, ChessBoard parentBoard) {
        this.pos = pos;
        this.color = color;
        this.parentBoard = parentBoard;

        pinnedDirection = new LinkedList<>();
    }

    public Color color() {
        return color;
    }

    public Vector pos() {
        return pos;
    }

    public void setPos(Vector newPos) {
        pos = newPos;
    }

    // Generates all the possible pseudo-legal moves the piece can make with current
    // board position and lists them in the possibleMoves list
    // also lists protected pieces
    abstract public void generatePossibleMoves();

    public enum Name {
        KING, QUEEN,
        ROOK, KNIGHT,
        BISHOP, PAWN,
    };

    abstract public Name getName();

    abstract public boolean isSlidingPiece();

    // Checks parent board and
    // Returns 1 if square is occupied by a friendly piece
    // Returns 0 if square is not occupied
    // Returns -1 if is occupied by an enemy piece
    protected int isOccupied(Vector squarePos) {
        ChessPiece piece = parentBoard.getPiece(squarePos);
        if (piece == null)
            return 0;
        return piece.color == this.color ? 1 : -1;
    }

    protected void setAttacked(Vector movePos) {
        attackedSquares.set(movePos.y * 8 + movePos.x);
    }

    protected void setAttacked(int x, int y) {
        attackedSquares.set(y * 8 + x);
    }

    static Vector[] directionOffsets = new Vector[] {
            // Straing paths
            new Vector(1, 0), new Vector(-1, 0),
            new Vector(0, 1), new Vector(0, -1),
            // Diagonal paths
            new Vector(1, 1), new Vector(1, -1),
            new Vector(-1, 1), new Vector(-1, -1),
    };

    // Generate moves for sliding pieces(queen, rook and bishop)
    protected List<Move> generateSlidingMoves() {
        List<Move> possibleMoves = new LinkedList<>();

        // Only diagonal moves
        int startDirIndex = (this.getName() == Name.BISHOP) ? 4 : 0;
        // Only straight moves
        int endDirIndex = (this.getName() == Name.ROOK) ? 4 : 8;

        for (int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            Vector destSquare = new Vector(this.pos);

            int iterationNum = iterationsToEdge(this.pos, directionIndex);
            for (int i = 0; i < iterationNum; i++) {
                destSquare.add(directionOffsets[directionIndex]);
                ChessPiece otherPiece = parentBoard.getPiece(destSquare);

                setAttacked(destSquare);

                if (otherPiece != null) {
                    if (otherPiece.color == this.color) {
                        break;
                    }
                    // Capture enemy piece
                    possibleMoves.add(new Move(this, this.pos, destSquare, otherPiece));
                    if (otherPiece.getName() == Name.KING) {
                        markAttackedDirection(directionIndex, destSquare, iterationsToEdge(destSquare, directionIndex));
                    }
                    break;
                }
                possibleMoves.add(new Move(this, this.pos, new Vector(destSquare)));
            }
        }

        return possibleMoves;
    }

    static int iterationsToEdge(Vector pos, int directionIndex) {
        Vector off = directionOffsets[directionIndex];

        if (off.x == 0) {
            return Math.max((7 - pos.y) * off.y, pos.y * (-off.y));
        }
        if (off.y == 0) {
            return Math.max((7 - pos.x) * off.x, pos.x * (-off.x));
        }

        return Math.min(
                // One of the operands will be negative
                Math.max((7 - pos.x) * off.x, pos.x * (-off.x)),
                Math.max((7 - pos.y) * off.y, pos.y * (-off.y)));
    }

    // If a sliding piece checks the king directly, then mark all the squares in
    // this direction as attacked so the king cant move along the check
    private void markAttackedDirection(int directionIndex, Vector startingPos, int iterationsToEdge) {
        Vector currentPos = new Vector(startingPos);
        for (int i = 0; i < iterationsToEdge; i++) {
            currentPos.add(directionOffsets[directionIndex]);
            setAttacked(currentPos);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getName().hashCode())
                .append(pos.hashCode())
                .append(color.hashCode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChessPiece)) {
            return false;
        }

        ChessPiece o = (ChessPiece) other;
        return new EqualsBuilder()
                .append(this.pos, o.pos)
                // Color data is already baked into name
                .append(this.getName(), o.getName())
                .isEquals();
    }
}

class King extends ChessPiece {
    King(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    private void checkSquare(int x, int y) {
        Vector toCheck = new Vector(x, y);
        if (isOccupied(toCheck) != 1) {
            possibleMoves.add(new Move(this, this.pos, toCheck, parentBoard.getPiece(toCheck)));
        }
        setAttacked(toCheck);
    }

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        // Account for a king standing in a leftmost ar a rightmost position
        int leftBound = Math.max(0, pos.x - 1);
        int rightBound = Math.min(pos.x + 1, 7);

        // Check upper row
        if (pos.y != 7) {
            for (int i = leftBound; i <= rightBound; i++) {
                checkSquare(i, pos.y + 1);
            }
        }

        // Check middle row
        if (leftBound != pos.x) {
            checkSquare(pos.x - 1, pos.y);
        }
        if (rightBound != pos.x) {
            checkSquare(pos.x + 1, pos.y);
        }

        // Check bottom row
        if (pos.y != 0) {
            for (int i = leftBound; i <= rightBound; i++) {
                checkSquare(i, pos.y - 1);
            }
        }

        // Castling
        if (parentBoard.getStatus(this.color).checkState == ChessBoard.KingStatus.CheckState.NONE) {
            int colorIndex = this.color == Color.WHITE ? 0 : 2;

            // Check short castling
            if (parentBoard.castling()[colorIndex]) {
                Vector pos1 = new Vector(pos.x + 1, pos.y);
                Vector pos2 = new Vector(pos.x + 2, pos.y);
                if ((!parentBoard.isUnderAttack(pos1, color) && isOccupied(pos1) == 0) &&
                        (!parentBoard.isUnderAttack(pos2, color) && isOccupied(pos2) == 0)) {
                    possibleMoves.add(new Move(this, this.pos, pos2));
                }
            }
            // Check long castling
            if (parentBoard.castling()[colorIndex + 1]) {
                Vector pos1 = new Vector(pos.x - 1, pos.y);
                Vector pos2 = new Vector(pos.x - 2, pos.y);
                if ((!parentBoard.isUnderAttack(pos1, color) && isOccupied(pos1) == 0) &&
                        (!parentBoard.isUnderAttack(pos2, color) && isOccupied(pos2) == 0)) {
                    possibleMoves.add(new Move(this, this.pos, pos2));
                }
            }
        }
    }

    @Override
    public boolean isSlidingPiece() {
        return false;
    }

    @Override
    public Name getName() {
        return Name.KING;
    }
}

class Queen extends ChessPiece {
    Queen(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        possibleMoves = generateSlidingMoves();
    }

    @Override
    public boolean isSlidingPiece() {
        return true;
    }

    @Override
    public Name getName() {
        return Name.QUEEN;
    }
}

class Rook extends ChessPiece {
    Rook(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        possibleMoves = generateSlidingMoves();
    }

    @Override
    public boolean isSlidingPiece() {
        return true;
    }

    @Override
    public Name getName() {
        return Name.ROOK;
    }
}

class Bishop extends ChessPiece {
    Bishop(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        possibleMoves = generateSlidingMoves();
    }

    @Override
    public boolean isSlidingPiece() {
        return true;
    }

    @Override
    public Name getName() {
        return Name.BISHOP;
    }
}

class Knight extends ChessPiece {
    Knight(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Possible moves for a knight
    static public Vector[] knightPosOffsets = new Vector[] {
            new Vector(2, 1), new Vector(-2, -1),
            new Vector(1, 2), new Vector(-1, -2),
            new Vector(-1, 2), new Vector(1, -2),
            new Vector(-2, 1), new Vector(2, -1),
    };

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        ChessBoard.KingStatus enemyKing = parentBoard.getStatus(invert(this.color));

        for (int i = 0; i < 8; i++) {
            Vector toCheck = Vector.add(pos, knightPosOffsets[i]);
            if (toCheck.x >= 0 && toCheck.x < 8 && toCheck.y >= 0 && toCheck.y < 8) {
                setAttacked(toCheck);
                if (enemyKing.kingPos.equals(toCheck)) {
                    enemyKing.addAttacker();
                    enemyKing.toBlockSq.set(this.pos.y * 8 + this.pos.x);
                }
                if (isOccupied(toCheck) != 1) {
                    possibleMoves.add(new Move(this, this.pos, toCheck));
                }
            }
        }
    }

    @Override
    public boolean isSlidingPiece() {
        return false;
    }

    @Override
    public Name getName() {
        return Name.KNIGHT;
    }
}

class Pawn extends ChessPiece {
    Pawn(Vector pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        attackedSquares = new BitSet(64);
        possibleMoves = new LinkedList<>();

        int startingRow = color == Color.WHITE ? 1 : 6;
        // If the pawn is white forward move will increase pos.y
        // If the pawn is black forward move will decrease pos.y
        int forMov = color == Color.WHITE ? 1 : -1;

        if (pos.y + forMov == 0 || pos.y + forMov == 7) {
            generatePromotions(forMov);
            return;
        }

        // Forward move
        if (isOccupied(new Vector(pos.x, pos.y + forMov)) == 0) {
            possibleMoves.add(new Move(this, this.pos, new Vector(pos.x, pos.y + forMov)));

            // Pawn can make a double move
            if (pos.y == startingRow && isOccupied(new Vector(pos.x, pos.y + forMov * 2)) == 0) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x, pos.y + forMov * 2)));
            }
        }

        // Capture
        ChessBoard.KingStatus enemyKing = parentBoard.getStatus(invert(this.color));
        if (pos.x > 0) {
            Vector toCheck = new Vector(pos.x - 1, pos.y + forMov);
            setAttacked(toCheck);
            if (enemyKing.kingPos.equals(toCheck)) {
                enemyKing.addAttacker();
                enemyKing.toBlockSq.set(this.pos.y * 8 + this.pos.x);
            }
            if (isOccupied(toCheck) == -1) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x - 1, pos.y + forMov)));
            }
        }
        if (pos.x < 7) {
            Vector toCheck = new Vector(pos.x + 1, pos.y + forMov);
            setAttacked(toCheck);
            if (enemyKing.kingPos.equals(toCheck)) {
                enemyKing.addAttacker();
                enemyKing.toBlockSq.set(this.pos.y * 8 + this.pos.x);
            }
            if (isOccupied(toCheck) == -1) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x + 1, pos.y + forMov)));
            }
        }

        // French move
        if (parentBoard.enPassant() != null) {
            if (pos.x > 0) {
                if (parentBoard.enPassant().equals(new Vector(pos.x - 1, pos.y + forMov))) {
                    possibleMoves.add(new Move(this, this.pos, new Vector(pos.x - 1, pos.y + forMov)));
                }
            }
            if (pos.x < 7) {
                if (parentBoard.enPassant().equals(new Vector(pos.x + 1, pos.y + forMov))) {
                    possibleMoves.add(new Move(this, this.pos, new Vector(pos.x + 1, pos.y + forMov)));
                }
            }
        }
    }

    private void generatePromotions(int forMov) {
        // Forward move
        if (isOccupied(new Vector(pos.x, pos.y + forMov)) == 0) {
            for (var i : Promotion.PromoteTo.values()) {
                if (i == PromoteTo.INPUT)
                    continue;
                possibleMoves.add(new Promotion(this, this.pos, new Vector(pos.x, pos.y + forMov), i));
            }
        }

        ChessBoard.KingStatus enemyKing = parentBoard.getStatus(invert(this.color));

        // Capture
        if (pos.x > 0) {
            Vector toCheck = new Vector(pos.x - 1, pos.y + forMov);
            setAttacked(toCheck);
            if (enemyKing.kingPos.equals(toCheck)) {
                enemyKing.addAttacker();
                enemyKing.toBlockSq.set(this.pos.y * 8 + this.pos.x);
            }
            if (isOccupied(toCheck) == -1) {
                for (var i : Promotion.PromoteTo.values()) {
                    if (i == PromoteTo.INPUT)
                        continue;
                    possibleMoves.add(new Promotion(this, this.pos, new Vector(pos.x - 1, pos.y + forMov), i));
                }
            }
        }
        if (pos.x < 7) {
            Vector toCheck = new Vector(pos.x + 1, pos.y + forMov);
            setAttacked(toCheck);
            if (enemyKing.kingPos.equals(toCheck)) {
                enemyKing.addAttacker();
                enemyKing.toBlockSq.set(this.pos.y * 8 + this.pos.x);
            }
            if (isOccupied(toCheck) == -1) {
                for (var i : Promotion.PromoteTo.values()) {
                    if (i == PromoteTo.INPUT)
                        continue;
                    possibleMoves.add(new Promotion(this, this.pos, new Vector(pos.x + 1, pos.y + forMov), i));
                }
            }
        }
    }

    @Override
    public boolean isSlidingPiece() {
        return false;
    }

    @Override
    public Name getName() {
        return Name.PAWN;
    }
}