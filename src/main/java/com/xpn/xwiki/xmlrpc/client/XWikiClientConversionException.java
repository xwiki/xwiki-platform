package com.xpn.xwiki.xmlrpc.client;

public class XWikiClientConversionException extends XWikiClientException
{
    private static final long serialVersionUID = 5971517238393022569L;

    public XWikiClientConversionException()
    {
        super();
    }

    public XWikiClientConversionException(String message)
    {
        super(message);
    }

    public XWikiClientConversionException(Throwable cause)
    {
        super(cause);
    }

    public XWikiClientConversionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
