package org.xwiki.url;

public class URLFactoryException extends RuntimeException
{
    public URLFactoryException(String message)
    {
        super(message);
    }

    public URLFactoryException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
