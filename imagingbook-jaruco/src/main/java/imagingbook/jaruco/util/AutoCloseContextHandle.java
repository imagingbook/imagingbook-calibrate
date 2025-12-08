package imagingbook.jaruco.util;

public class AutoCloseContextHandle<T> implements AutoCloseable {

    private T contextValue;

    public AutoCloseContextHandle<T> setValue(T value) {
        this.contextValue = value;
        return this;
    }

    public T getValue() {
        if (contextValue == null) {
            throw new IllegalStateException("context value unavailable");
        }
        return contextValue;
    }

    @Override
    public void close()  {
        contextValue = null;
        System.out.println("Closing context");
    }

}
