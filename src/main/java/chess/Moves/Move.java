package chess.Moves;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessPiece;

public class Move {
    // Piece that moves
    public final ChessPiece piece;
    public final ChessPiece captured;
    public final Vector from;
    public final Vector to;

    public Move(ChessPiece piece, Vector from, Vector to) {
        this.piece = piece;
        this.from = from;
        this.to = to;

        this.captured = null;
    }

    public Move(ChessPiece piece, Vector from, Vector to, ChessPiece captured) {
        this.piece = piece;
        this.from = from;
        this.to = to;

        this.captured = captured;
    }

    // Corresponding letter for a coordinate
    private String posNot = "abcdefgh";
    @Override
    public String toString() {
        StringBuilder retString = new StringBuilder();
        retString.append(posNot.charAt(from.x));
        retString.append(String.valueOf(from.y + 1));

        if (captured != null) {
            retString.append("x");
        }

        retString.append(posNot.charAt(to.x));
        retString.append(String.valueOf(to.y + 1));

        return retString.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(from.hashCode())
                .append(to.hashCode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move)) {
            return false;
        }

        Move o = (Move) other;
        return new EqualsBuilder()
                .append(this.from, o.from)
                .append(this.to, o.to)
                .isEquals();
    }
}
