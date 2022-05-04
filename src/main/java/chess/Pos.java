package chess;

import org.apache.commons.lang3.builder.*;

public class Pos {
    public final int x;
    public final int y;

    Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Pos(Pos pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(x)
                .append(y)
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pos)) {
            return false;
        }

        Pos o = (Pos) other;
        return new EqualsBuilder()
                .append(this.x, o.x)
                .append(this.y, o.y)
                .isEquals();
    }
}
