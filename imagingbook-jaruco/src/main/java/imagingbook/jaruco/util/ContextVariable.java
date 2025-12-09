package imagingbook.jaruco.util;

public class ContextVariable<T> implements AutoCloseable {

    private T value;

    public ContextVariable<T> set(T value) {
        this.value = value;
        return this;
    }

    public T get() {
        if (value == null) {
            throw new IllegalStateException("context value unavailable");
        }
        return value;
    }

    @Override
    public void close()  {
        value = null;
    }

}
