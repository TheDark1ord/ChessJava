package chess;

import org.apache.commons.lang3.builder.*;

public class Vector {
    public int x;
    public int y;

    public Vector(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector(Vector pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    // Some vector math
    static public Vector add(Vector lhs, Vector rhs) {
        return new Vector(lhs.x + rhs.x, lhs.y + rhs.y);
    }

    static public Vector mul(Vector lhs, int rhs) {
        return new Vector(lhs.x * rhs, lhs.y * rhs);
    }

    public Vector add(Vector other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector mul(int other) {
        this.x *= other;
        this.y *= other;
        return this;
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
        if (!(other instanceof Vector)) {
            return false;
        }

        Vector o = (Vector) other;
        return new EqualsBuilder()
                .append(this.x, o.x)
                .append(this.y, o.y)
                .isEquals();
    }
}
