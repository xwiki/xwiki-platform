package org.xwiki.extension.xar.internal.handler.packager;

public class NotADocumentException extends PackagerException
{
    public NotADocumentException(String message)
    {
        super(message);
    }

    public NotADocumentException(String message, Exception e)
    {
        super(message, e);
    }
}
