package com.xpn.xwiki.xmlrpc.client;

/**
 * This exception is thrown to signal an error on the server. Sometimes the original cause of the
 * error is also transmitted and can be obtained by calling the getCause method (otherwise getCause
 * will return null).
 */
public class XWikiClientRemoteException extends XWikiClientException
{
    private static final long serialVersionUID = 3943876772981124681L;

    public XWikiClientRemoteException()
    {
        super();
    }

    public XWikiClientRemoteException(String message)
    {
        super(message);
    }

    public XWikiClientRemoteException(Throwable cause)
    {
        super(cause);
    }

    public XWikiClientRemoteException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
