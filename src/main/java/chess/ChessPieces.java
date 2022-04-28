package chess;

import java.util.List;

import javax.swing.colorchooser.ColorSelectionModel;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

abstract class ChessPiece {
    public static enum Color {
        WHITE,
        BLACK
    };

    protected final Color color;
    protected Pos pos;
    protected ChessBoard parentBoard;
    public List<Pos> possibleMoves;

    ChessPiece(Pos pos, Color color, ChessBoard parentBoard) {
        this.pos = pos;
        this.color = color;
        this.parentBoard = parentBoard;
    }

    public Color color() {
        return color;
    }

    public Pos pos() {
        return pos;
    }

    // try to move a piece to a certain position
    // if a piece can move to that position then pos is changed to new_pos
    // and function returns -1 if it captured an enemy piece
    // else function returns 1
    // if a piece cannot move to that position, this function returns 0\
    //
    // possibleMoves.contains() performance is not of a highest priority,
    // as this method is entended to be used only when user is trying to move a
    // piece
    //
    // Requires to generatePossibleMoves to have been called
    public int tryToMove(Pos new_pos) {
        if (!possibleMoves.contains(new_pos)) {
            return 0;
        }
        pos = new_pos;
        // If there is a piece on that square it has to be different color
        return parentBoard.getPiece(new_pos) == null ? 1 : -1;
    }

    // Get every move, that is avalible to that piece
    // does not account for pinned pieces
    // moves are returned as a boolean matrix, where true means that that piece
    // can move to that particular square
    //
    // that mathod can be useful if you want, for example, if a king is under check,
    // if it is mate, or simply to determine whe the king can move
    //
    // Requires to generatePossibleMoves to have been called
    abstract boolean[][] getMoveMap();

    // Generates all the possible moves the piece can make with current
    // board position and lists them in the possibleMoves list
    // does not account for the pinned pieces and checks
    abstract void generatePossibleMoves();

    public enum Name {
        W_KING, W_QUEEN,
        W_ROOK, W_KNIGHT,
        W_BISHOP, W_PAWN,

        B_KING, B_QUEEN,
        B_ROOK, B_KNIGHT,
        B_BISHOP, B_PAWN,
    };

    abstract public Name getName();

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
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

    // Checks parent board and
    // Returns 1 if square is occupied by a friendly piece
    // Returns 0 if square is not occupied
    // Returns -1 if is occupied by an enemy piece
    private int isOccupied(Pos squarePos) {
        ChessPiece piece = parentBoard.getPiece(squarePos);
        if (piece == null)
            return 0;
        return piece.color.equals(this.color) ? 1 : -1;
    }
}

class King extends ChessPiece {
    King(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {

    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_KING : Name.B_KING;
    }
}

class Queen extends ChessPiece {
    Queen(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {
        
    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_QUEEN : Name.B_QUEEN;
    }
}

class Rook extends ChessPiece {
    Rook(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {

    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_ROOK : Name.B_ROOK;
    }
}

class Bishop extends ChessPiece {
    Bishop(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {

    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_BISHOP : Name.B_BISHOP;
    }
}

class Knight extends ChessPiece {
    Knight(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {

    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_KNIGHT : Name.B_KNIGHT;
    }
}

class Pawn extends ChessPiece {
    Pawn(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    boolean[][] getMoveMap() {
        return new boolean[8][8];
    }

    @Override
    void generatePossibleMoves() {

    }

    @Override
    public Name getName() {
        return color.equals(Color.WHITE) ? Name.W_PAWN : Name.B_PAWN;
    }
}