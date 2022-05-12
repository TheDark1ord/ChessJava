package chess.Logic;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Moves.Move;
import javafx.scene.paint.Color;

public abstract class ChessPiece {
    public enum ChessColor {
        WHITE,
        BLACK
    }

    protected final ChessColor color;
    protected Vector pos;
    protected ChessBoard parentBoard;
    // Squares with friendly pieces to which this piece cannot move
    // but prevents the enemy king from capturing
    public List<Vector> protectedSquares;
    public List<Move> possibleMoves;

    ChessPiece(Vector pos, ChessColor color, ChessBoard parentBoard) {
        this.pos = pos;
        this.color = color;
        this.parentBoard = parentBoard;
    }

    public ChessColor color() {
        return color;
    }

    public Vector pos() {
        return pos;
    }

    public void setPos(Vector newPos) {
        pos = newPos;
    }

    // Generates all the possible moves the piece can make with current
    // board position and lists them in the possibleMoves list
    // also lists protected pieces
    // does not account for the pinned pieces and checks
    abstract public void generatePossibleMoves();

    // Get the map of all possible moves,
    // Squares, where the piece can move will be marked as 1
    // Expects the generatePossibleMoves() to be called beforehand
    public BitSet getMoveMap() {
        BitSet retSet = new BitSet(64);

        for (Move move : possibleMoves) {
            retSet.set(move.to.y * 8 + move.to.x);
        }
        for (Vector move : protectedSquares) {
            retSet.set(move.y * 8 + move.x);
        }

        return retSet;
    }

    public enum Name {
        KING, QUEEN,
        ROOK, KNIGHT,
        BISHOP, PAWN,
    };

    abstract public Name getName();

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
    King(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    private void checkSquare(int x, int y) {
        Vector to_check = new Vector(x, y);
        switch (isOccupied(to_check)) {
            case (1):
                protectedSquares.add(to_check);
                break;
            case (0):
                possibleMoves.add(new Move(this, this.pos, to_check));
                break;
            case (-1):
                possibleMoves.add(new Move(this, this.pos, to_check, parentBoard.getPiece(to_check)));
                break;
        }
    }

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
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
        if (!parentBoard.isUnderAttack(pos, color)) {
            int startingIndex = this.color == ChessColor.WHITE ? 0 : 2;

            if (parentBoard.castling()[startingIndex]) {
                Vector pos1 = new Vector(pos.x - 1, pos.y);
                Vector pos2 = new Vector(pos.x - 2, pos.y);
                if ((!parentBoard.isUnderAttack(pos1, color) && isOccupied(pos1) == 0) &&
                        (!parentBoard.isUnderAttack(pos2, color) && isOccupied(pos2) == 0)) {
                    possibleMoves.add(new Move(this, this.pos, pos2));
                }
            }
            if (parentBoard.castling()[startingIndex + 1]) {
                Vector pos1 = new Vector(pos.x + 1, pos.y);
                Vector pos2 = new Vector(pos.x + 2, pos.y);
                if ((!parentBoard.isUnderAttack(pos1, color) && isOccupied(pos1) == 0) &&
                        (!parentBoard.isUnderAttack(pos2, color) && isOccupied(pos2) == 0)) {
                    possibleMoves.add(new Move(this, this.pos, pos2));
                }
            }
        }
    }

    @Override
    public Name getName() {
        return Name.KING;
    }
}

class Queen extends ChessPiece {
    Queen(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Returns true if the square if occupied
    private boolean checkSquare(int p1, int p2) {
        Vector to_check = new Vector(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(new Move(this, this.pos, to_check));
                break;
            case (-1):
                possibleMoves.add(new Move(this, this.pos, to_check));
                // To prevent the king from walking along the check
                if (parentBoard.getPiece(to_check).getName() != Name.KING)
                    return true;
                return false;
            case (1):
                protectedSquares.add(to_check);
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
        possibleMoves = new LinkedList<>();

        // Check straight lines
        for (int x = pos.x + 1; x < 8; x++) {
            if (checkSquare(x, pos.y)) {
                break;
            }
        }
        for (int x = pos.x - 1; x >= 0; x--) {
            if (checkSquare(x, pos.y)) {
                break;
            }
        }
        for (int y = pos.y + 1; y < 8; y++) {
            if (checkSquare(pos.x, y)) {
                break;
            }
        }
        for (int i = pos.y - 1; i >= 0; i--) {
            if (checkSquare(pos.x, i)) {
                break;
            }
        }

        // Check diagonals
        for (int i = pos.y + 1, j = pos.x + 1; i < 8 && j < 8; i++, j++) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y - 1, j = pos.x + 1; i >= 0 && j < 8; i--, j++) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y + 1, j = pos.x - 1; i < 8 && j >= 0; i++, j--) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y - 1, j = pos.x - 1; i >= 0 && j >= 0; i--, j--) {
            if (checkSquare(j, i)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return Name.QUEEN;
    }
}

class Rook extends ChessPiece {
    Rook(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Returns true if the square if occupied
    private boolean checkSquare(int p1, int p2) {
        Vector to_check = new Vector(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(new Move(this, this.pos, to_check));
                break;
            case (-1):
                possibleMoves.add(new Move(this, this.pos, to_check, parentBoard.getPiece(to_check)));
                // To prevent the king from walking along the check
                if (parentBoard.getPiece(to_check).getName() != Name.KING)
                    return true;
                return false;
            case (1):
                protectedSquares.add(to_check);
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
        possibleMoves = new LinkedList<>();

        for (int i = pos.x + 1; i < 8; i++) {
            if (checkSquare(i, pos.y)) {
                break;
            }
        }
        for (int i = pos.x - 1; i >= 0; i--) {
            if (checkSquare(i, pos.y)) {
                break;
            }
        }
        for (int i = pos.y + 1; i < 8; i++) {
            if (checkSquare(pos.x, i)) {
                break;
            }
        }
        for (int i = pos.y - 1; i >= 0; i--) {
            if (checkSquare(pos.x, i)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return Name.ROOK;
    }
}

class Bishop extends ChessPiece {
    Bishop(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Returns true if the square if occupied
    private boolean checkSquare(int p1, int p2) {
        Vector to_check = new Vector(p1, p2);
        int ocup = isOccupied(to_check);
        switch (ocup) {
            case (0):
                possibleMoves.add(new Move(this, this.pos, to_check));
                break;
            case (-1):
                possibleMoves.add(new Move(this, this.pos, to_check));
                // To prevent the king from walking along the check
                if (parentBoard.getPiece(to_check).getName() != Name.KING)
                    return true;
                return false;
            case (1):
                protectedSquares.add(to_check);
                return true;
        }
        return false;
    }

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
        possibleMoves = new LinkedList<>();

        for (int i = pos.y + 1, j = pos.x + 1; i < 8 && j < 8; i++, j++) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y - 1, j = pos.x + 1; i >= 0 && j < 8; i--, j++) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y + 1, j = pos.x - 1; i < 8 && j >= 0; i++, j--) {
            if (checkSquare(j, i)) {
                break;
            }
        }
        for (int i = pos.y - 1, j = pos.x - 1; i >= 0 && j >= 0; i--, j--) {
            if (checkSquare(j, i)) {
                break;
            }
        }
    }

    @Override
    public Name getName() {
        return Name.BISHOP;
    }
}

class Knight extends ChessPiece {
    Knight(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    // Possible moves for a knight
    private int X[] = { 2, 1, -1, -2, -2, -1, 1, 2 };
    private int Y[] = { 1, 2, 2, 1, -1, -2, -2, -1 };

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
        possibleMoves = new LinkedList<>();

        for (int i = 0; i < 8; i++) {
            Vector to_check = new Vector(pos.x + X[i], pos.y + Y[i]);
            if (to_check.x >= 0 && to_check.x < 8 && to_check.y >= 0 && to_check.y < 8) {
                if (isOccupied(to_check) != 1) {
                    possibleMoves.add(new Move(this, this.pos, to_check));
                } else {
                    protectedSquares.add(to_check);
                }
            }
        }
    }

    @Override
    public Name getName() {
        return Name.KNIGHT;
    }
}

class Pawn extends ChessPiece {
    Pawn(Vector pos, ChessColor color, ChessBoard parentBoard) {
        super(pos, color, parentBoard);
    }

    @Override
    public void generatePossibleMoves() {
        protectedSquares = new LinkedList<>();
        possibleMoves = new LinkedList<>();

        int startingRow = color == ChessColor.WHITE ? 1 : 6;
        // If the pawn is white forward move will increase pos.y
        // If the pawn is black forward move will decrease pos.y
        int forMov = color == ChessColor.WHITE ? 1 : -1;

        // Forward move
        if (isOccupied(new Vector(pos.x, pos.y + forMov)) == 0) {
            possibleMoves.add(new Move(this, this.pos, new Vector(pos.x, pos.y + forMov)));

            // Pawn can make a double move
            if (pos.y == startingRow && isOccupied(new Vector(pos.x, pos.y + forMov * 2)) == 0) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x, pos.y + forMov * 2)));
            }
        }

        // Capture
        if (pos.x > 0) {
            Vector to_check = new Vector(pos.x - 1, pos.y + forMov);
            if (isOccupied(to_check) == -1) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x - 1, pos.y + forMov)));
            } else if (isOccupied(to_check) == 1) {
                protectedSquares.add(new Vector(pos.x - 1, pos.y + forMov));
            }
        }
        if (pos.x < 7) {
            Vector to_check = new Vector(pos.x + 1, pos.y + forMov);
            if (isOccupied(to_check) == -1) {
                possibleMoves.add(new Move(this, this.pos, new Vector(pos.x + 1, pos.y + forMov)));
            } else if (isOccupied(to_check) == 1) {
                protectedSquares.add(new Vector(pos.x - 1, pos.y + forMov));
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

    @Override
    public BitSet getMoveMap() {
        BitSet retSet = new BitSet(64);
        int forMov = color == ChessColor.WHITE ? 1 : -1;

        retSet.set((pos.y + forMov) * 8 + pos.x - 1);
        retSet.set((pos.y + forMov) * 8 + pos.x + 1);
        return retSet;
    }

    @Override
    public Name getName() {
        return Name.PAWN;
    }
}