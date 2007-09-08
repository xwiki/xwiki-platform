package com.xpn.xwiki.xmlrpc.client;

/**
 * This is the exception thrown by Swizzle to signal errors. Errors that occurred on the server are
 * indicated by throwing a {@link com.xpn.xwiki.xmlrpc.client.XWikiClientRemoteException}, which
 * is a subclass of SwizzleXWikiException.
 */
public class XWikiClientException extends Exception
{
    private static final long serialVersionUID = 4578210684020233937L;

    public XWikiClientException()
    {
        super();
    }

    public XWikiClientException(String message)
    {
        super(message);
    }

    public XWikiClientException(Throwable cause)
    {
        super(cause);
    }

    public XWikiClientException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
