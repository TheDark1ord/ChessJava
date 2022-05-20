package chess.Moves;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessPiece;

public class Castling extends Move {
    public enum Side {SHORT, LONG}
    public Side side;

    // From and to represent king positions and not anything alse
    public Castling(ChessPiece piece, Vector from, Vector to, Side side) {
        super(piece, from, to);
        this.side = side;
    }

    @Override
    public String toString() {
        if (side == Side.LONG) {
            return "O-O-O";
        }
        return "O-O";
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(super.hashCode())
                .append(side.hashCode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Castling)) {
            return false;
        }

        Castling o = (Castling) other;
        return new EqualsBuilder()
                .append(this.from, o.from)
                .append(this.to, o.to)
                .isEquals();
    }
}
