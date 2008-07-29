package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

/**
 * Exception when try get {@link SuperDocument} that does not exist.
 * 
 * @version $Id: $
 * @deprecated Use {@link XObjectDocumentDoesNotExistException} since 1.0 RC1
 */
@Deprecated
public class SuperDocumentDoesNotExistException extends XObjectDocumentDoesNotExistException
{
    /**
     * Create new instance of {@link SuperDocumentDoesNotExistException}.
     * 
     * @param message the error message.
     */
    public SuperDocumentDoesNotExistException(String message)
    {
        super(message);
    }
}
