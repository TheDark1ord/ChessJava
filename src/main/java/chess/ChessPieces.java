package chess;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

enum Color {
    WHITE,
    BLACK
}

abstract class ChessPiece {
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

    // Generates all the possible moves the piece can make with current
    // board position and lists them in the possibleMoves list
    // also lists protected pieces
    // does not account for the pinned pieces and checks
    abstract public void generatePossibleMoves();

    public enum Name {
        W_KING, W_QUEEN,
        W_ROOK, W_KNIGHT,
        W_BISHOP, W_PAWN,

        B_KING, B_QUEEN,
        B_ROOK, B_KNIGHT,
        B_BISHOP, B_PAWN,
    };

    abstract public Name getName();

    // Checks parent board and
    // Returns 1 if square is occupied by a friendly piece
    // Returns 0 if square is not occupied
    // Returns -1 if is occupied by an enemy piece
    protected int isOccupied(Pos squarePos) {
        ChessPiece piece = parentBoard.getPiece(squarePos);
        if (piece == null)
            return 0;
        return piece.color == this.color ? 1 : -1;
    }

    // TODO: add check diagonal and check line methods

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
}

class King extends ChessPiece {
    King(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    // Does not account for squares under attack
    public void generatePossibleMoves() {
        possibleMoves = new LinkedList<>();

        // Account for a king standing in a leftmost ar a rightmost position
        int leftBound = Math.max(0, pos.x() - 1);
        int rightBound = Math.min(pos.x() + 1, 7);

        // Check upper row
        if (pos.y() != 7) {
            for (int i = leftBound; i <= rightBound; i++) {
                Pos to_check = new Pos(i, pos.y());
                if (isOccupied(to_check) != 1) {
                    possibleMoves.add(to_check);
                }
            }
        }

        // Check middle row
        if (leftBound != pos.x()) {
            Pos to_check = new Pos(pos.x() - 1, pos.y());
            if (isOccupied(to_check) != 1) {
                possibleMoves.add(to_check);
            }
        }
        if (rightBound != pos.x()) {
            Pos to_check = new Pos(pos.x() + 1, pos.y());
            if (isOccupied(to_check) != 1) {
                possibleMoves.add(to_check);
            }
        }

        // Check bottom row
        if (pos.y() != 0) {
            for (int i = leftBound; i <= rightBound; i++) {
                Pos to_check = new Pos(i, pos.y());
                if (isOccupied(to_check) != 1) {
                    possibleMoves.add(to_check);
                }
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_KING : Name.B_KING;
    }
}

class Queen extends ChessPiece {
    Queen(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Returns true if the square if occupied
    private boolean checkSquare(int p1, int p2) {
        Pos to_check = new Pos(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(to_check);
                break;
            // Fallthrough
            case (-1):
                possibleMoves.add(to_check);
            case (1):
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        // Check straight lines
        for (int i = pos.x() + 1; i < 8; i++) {
            if (checkSquare(i, pos.y())) {
                break;
            }
        }
        for (int i = pos.x() - 1; i >= 0; i--) {
            if (checkSquare(i, pos.y())) {
                break;
            }
        }
        for (int i = pos.y() + 1; i < 8; i++) {
            if (checkSquare(pos.x(), i)) {
                break;
            }
        }
        for (int i = pos.y() - 1; i >= 0; i--) {
            if (checkSquare(pos.x(), i)) {
                break;
            }
        }

        // Check diagonals
        for (int i = pos.y() + 1, j = pos.x() + 1; i < 8 && j < 8; i++, j++) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() - 1, j = pos.x() + 1; i >= 0 && j < 8; i--, j++) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() + 1, j = pos.x() - 1; i < 8 && j >= 0; i++, j--) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() - 1, j = pos.x() - 1; i >= 0 && j >= 0; i--, j--) {
            if (checkSquare(i, j)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_QUEEN : Name.B_QUEEN;
    }
}

class Rook extends ChessPiece {
    Rook(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Returns true if the square is occupied
    private boolean checkSquare(int p1, int p2) {
        Pos to_check = new Pos(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(to_check);
                break;
            // Fallthrough
            case (-1):
                possibleMoves.add(to_check);
                return true;
            case (1):
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        for (int i = pos.x() + 1; i < 8; i++) {
            if (checkSquare(i, pos.y())) {
                break;
            }
        }
        for (int i = pos.x() - 1; i >= 0; i--) {
            if (checkSquare(i, pos.y())) {
                break;
            }
        }
        for (int i = pos.y() + 1; i < 8; i++) {
            if (checkSquare(pos.x(), i)) {
                break;
            }
        }
        for (int i = pos.y() - 1; i >= 0; i--) {
            if (checkSquare(pos.x(), i)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_ROOK : Name.B_ROOK;
    }
}

class Bishop extends ChessPiece {
    Bishop(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    private boolean checkSquare(int p1, int p2) {
        Pos to_check = new Pos(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(to_check);
                break;
            // Fallthrough
            case (-1):
                possibleMoves.add(to_check);
                return true;
            case (1):
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        for (int i = pos.y() + 1, j = pos.x() + 1; i < 8 && j < 8; i++, j++) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() - 1, j = pos.x() + 1; i >= 0 && j < 8; i--, j++) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() + 1, j = pos.x() - 1; i < 8 && j >= 0; i++, j--) {
            if (checkSquare(i, j)) {
                break;
            }
        }
        for (int i = pos.y() - 1, j = pos.x() - 1; i >= 0 && j >= 0; i--, j--) {
            if (checkSquare(i, j)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_BISHOP : Name.B_BISHOP;
    }
}

class Knight extends ChessPiece {
    Knight(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Possible moves for a knight
    private int X[] = { 2, 1, -1, -2, -2, -1, 1, 2 };
    private int Y[] = { 1, 2, 2, 1, -1, -2, -2, -1 };

    @Override
    public void generatePossibleMoves() {
        for (int i = 0; i < 8; i++) {
            Pos to_check = new Pos(pos.x() + X[i], pos.y() + Y[i]);
            if (to_check.x() > 0 && to_check.x() < 8 && to_check.y() > 0 && to_check.y() < 8) {
                if (isOccupied(pos) != 1) {
                    possibleMoves.add(to_check);
                } 
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_KNIGHT : Name.B_KNIGHT;
    }
}

class Pawn extends ChessPiece {
    Pawn(Pos pos, Color color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        int startingRow = color == Color.WHITE ? 2 : 6;
        // If the pawn is white forward move will increase pos.y()
        // If the pawn is black forward move will decrease pos.y()
        int forMov = color == Color.WHITE ? 1 : -1;

        // Forward move
        if (isOccupied(new Pos(pos.x(), pos.y() + forMov)) == 0) {
            possibleMoves.add(new Pos(pos.x(), pos.y() + forMov));

            // Pawn can make a double move
            if (pos.y() == startingRow && isOccupied(new Pos(pos.x(), pos.y() + forMov * 2)) == 0) {
                possibleMoves.add(new Pos(pos.x(), pos.y() + forMov * 2));
            }
        }

        // Capture
        if (pos.x() > 0) {
            if (isOccupied((new Pos(pos.x() - 1, pos.y() + forMov))) == -1) {
                possibleMoves.add(new Pos(pos.x() - 1, pos.y() + forMov));
            }
        }
        if (pos.x() < 7) {
            if (isOccupied((new Pos(pos.x() + 1, pos.y() + forMov))) == -1) {
                possibleMoves.add(new Pos(pos.x() + 1, pos.y() + forMov));
            }
        }

        // French move
        if (parentBoard.enPassant() != null) {
            if (pos.x() > 0) {
                if (parentBoard.enPassant().equals(new Pos(pos.x() - 1, pos.y() + forMov))) {
                    possibleMoves.add(new Pos(pos.x() - 1, pos.y() + forMov));
                }
            }
            if (pos.x() < 7) {
                if (parentBoard.enPassant().equals(new Pos(pos.x() + 1, pos.y() + forMov))) {
                    possibleMoves.add(new Pos(pos.x() + 1, pos.y() + forMov));
                }
            }
        }
    }

    @Override
    public Name getName() {
        return color == Color.WHITE ? Name.W_PAWN : Name.B_PAWN;
    }
}