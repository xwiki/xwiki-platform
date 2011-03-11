package org.xwiki.extension.xar.internal.handler.packager;

public class PackagerException extends Exception
{
    public PackagerException(String message)
    {
        super(message);
    }

    public PackagerException(String message, Exception e)
    {
        super(message, e);
    }
}
