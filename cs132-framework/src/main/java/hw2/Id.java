package hw2;

import java.util.Objects;

public class Id {
    String type;
    String name;

    public Id(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Id))
            return false;
        Id other = (Id) o;
        return other.name.equals(this.name) && other.type.equals(this.type);

    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

}
