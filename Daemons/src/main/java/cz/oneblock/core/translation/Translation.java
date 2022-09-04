package cz.oneblock.core.translation;

import java.util.Objects;


public record Translation(String local, String english, String id) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Translation that = (Translation) o;
        return local.equals(that.local) && english.equals(that.english) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(local, english, id);
    }
}
