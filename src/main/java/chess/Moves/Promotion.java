package chess.Moves;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessPiece;

public class Promotion extends Move {
    // INPUT - still waiting for user input
    public enum PromoteTo {INPUT, QUEEN, ROOK, BISHOP, KNIGHT};
    public PromoteTo promoteTo;

    Promotion(ChessPiece piece, Vector from, Vector to) {
        super(piece, from, to);
        promoteTo = PromoteTo.INPUT;
    }

    Promotion(ChessPiece piece, Vector from, Vector to, PromoteTo promoteTo) {
        super(piece, from, to);
        this.promoteTo = promoteTo;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(super.hashCode())
                .append(promoteTo.hashCode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Promotion)) {
            return false;
        }

        Promotion o = (Promotion) other;
        return new EqualsBuilder()
                .append(this.from, o.from)
                .append(this.to, o.to)
                .append(this.promoteTo, o.promoteTo)
                .isEquals();
    }
}
