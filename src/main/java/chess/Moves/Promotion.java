package chess.Moves;

import java.util.HashMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import chess.Vector;
import chess.Logic.ChessPiece;

public class Promotion extends Move {
    // INPUT - still waiting for user input
    public enum PromoteTo {
        INPUT, QUEEN, ROOK, BISHOP, KNIGHT
    };

    public PromoteTo promoteTo;

    public Promotion(ChessPiece piece, Vector from, Vector to) {
        super(piece, from, to);
        promoteTo = PromoteTo.INPUT;
    }

    public Promotion(ChessPiece piece, Vector from, Vector to, ChessPiece captured) {
        super(piece, from, to, captured);
        promoteTo = PromoteTo.INPUT;
    }

    public Promotion(ChessPiece piece, Vector from, Vector to, PromoteTo promoteTo) {
        super(piece, from, to);
        this.promoteTo = promoteTo;
    }

    public Promotion(ChessPiece piece, Vector from, Vector to, ChessPiece captured, PromoteTo promoteTo) {
        super(piece, from, to, captured);
        this.promoteTo = promoteTo;
    }

    private static HashMap<PromoteTo, Character> promToChar = new HashMap<>() {{
        put(PromoteTo.INPUT, 'q'); put(PromoteTo.QUEEN, 'q');
        put(PromoteTo.ROOK, 'r'); put(PromoteTo.BISHOP, 'b');
        put(PromoteTo.KNIGHT, 'n');  
    }};

    @Override
    public String toString() {
        return super.toString() + promToChar.get(promoteTo);
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
