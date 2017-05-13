package exceptions;

public interface ErrorHandler {
    public void errorOccurred(String whatIWasTryingToDo, Exception failure);
}
