package chess;

import org.apache.commons.lang3.builder.*;

public class Pos {
    private final int x;
    private final int y;

    Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
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
