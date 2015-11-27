package net.brainas.android.app;

/**
 * Created by innok on 11/24/2015.
 */
public class BrainasAppException extends Exception{
    public BrainasAppException() { super(); }
    public BrainasAppException(String message) { super(message); }
    public BrainasAppException(String message, Throwable cause) { super(message, cause); }
    public BrainasAppException(Throwable cause) { super(cause); }
}
