package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWikiException;

/**
 * Exception when try get {@link SuperDocument} that does not exist.
 * 
 * @version $Id$
 * @since Application Manager 1.0RC1
 */
public class XObjectDocumentDoesNotExistException extends XWikiException
{
    /**
     * Create new instance of {@link XObjectDocumentDoesNotExistException}.
     * 
     * @param message the error message.
     */
    public XObjectDocumentDoesNotExistException(String message)
    {
        super(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, message);
    }
}
