package org.xwiki.platform.comment;

public class CommentException extends Exception
{
    public CommentException(String message)
    {
        super(message);
    }
    public CommentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
