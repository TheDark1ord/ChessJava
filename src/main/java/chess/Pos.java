package chess;

import org.apache.commons.lang3.builder.*;

public class Pos {
    private final int x;
    private final int y;

    // Expects an unsigned integer
    Pos(int x, int y) {
        if (x < 0 || y < 0)
            throw new IllegalArgumentException();

        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
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
