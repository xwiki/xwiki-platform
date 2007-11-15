package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWikiException;

/**
 * Exception when try get {@link SuperDocument} that does not exist.
 * 
 * @version $Id: $
 * @future XA2 : rename to tDocumentObjectDoesNotExistException.
 */
public class SuperDocumentDoesNotExistException extends XWikiException
{
    /**
     * Create new instance of {@link SuperDocumentDoesNotExistException}.
     * 
     * @param message the error message.
     */
    public SuperDocumentDoesNotExistException(String message)
    {
        super(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, message);
    }
}
