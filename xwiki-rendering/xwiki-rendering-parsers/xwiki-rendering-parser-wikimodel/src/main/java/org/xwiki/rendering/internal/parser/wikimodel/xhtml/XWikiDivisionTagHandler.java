package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.wikimodel.wem.xhtml.handler.DivisionTagHandler;

/**
 * Change the class value indicating that the division is an embedded document.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class XWikiDivisionTagHandler extends DivisionTagHandler
{
    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.xhtml.handler.DivisionTagHandler#getDocumentClass()
     */
    @Override
    protected String getDocumentClass()
    {
        return "xwiki-document";
    }
}
