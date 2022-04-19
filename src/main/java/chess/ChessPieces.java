package chess;

abstract class ChessPiece {
    public static enum Color {
        WHITE,
        BLACK
    };

    private final Color color;
    private Pos pos;
    private ChessBoard parentBoard;

    ChessPiece(Pos pos, Color color, ChessBoard parentBoard) {
        this.pos = pos;
        this.color = color;
        this.parentBoard = parentBoard;
    }

    public Color getColor() {
        return color;
    }

    public Pos getPos() {
        return pos;
    }

    // try to move a piece to a certain position
    // if a piece can move to that position then pos is changed to new_pos
    // and function returns -1 if it captured an enemy piece
    // else function return 1
    // if a piece cannot move to that position, this function returns 0
    public int tryToMove(Pos new_pos) {
        if (!isMoveValid(new_pos)) {
            return 0;
        }
        // If there is a piece on that square it has to be different color
        return parentBoard.getPiece(new_pos) == null ? 1 : -1;
    }

    // Get every move, that is avalible to that piece
    // moves are returned as a boolean matrix, where true means that that piece
    // can move to that particular square
    //
    // that mathod can be useful if you want, for example, if a king is under check,
    // if it is mate, or simply to determine whe the king can move
    abstract boolean[][] getMoveMap();

    public enum Name {
        W_KING, W_QUEEN,
        W_ROOK, W_KNIGHT,
        W_BISHOP, W_PAWN,

        B_KING, B_QUEEN,
        B_ROOK, B_KNIGHT,
        B_BISHOP, B_PAWN,
    };

    abstract public Name getName();

    // Checks if this piece can move to the specified square
    // This method does not change any of the properties
    abstract protected boolean isMoveValid(Pos new_pos);
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_KING : Name.B_KING;
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_QUEEN : Name.B_QUEEN;
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_ROOK : Name.B_ROOK;
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_BISHOP : Name.B_BISHOP;
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_KNIGHT : Name.B_KNIGHT;
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
    protected boolean isMoveValid(Pos new_pos) {
        return false;
    }

    @Override
    public Name getName() {
        return this.getColor() == Color.WHITE ? Name.W_PAWN : Name.B_PAWN;
    }
}