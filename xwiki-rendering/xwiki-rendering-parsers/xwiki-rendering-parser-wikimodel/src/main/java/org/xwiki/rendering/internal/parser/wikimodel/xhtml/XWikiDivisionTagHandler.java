package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.wikimodel.wem.xhtml.handler.DivisionTagHandler;

/**
 * Change the class value indicating that the division is an embedded document. We do this in order to be independent 
 * of WikiModel in what we expose to the outside world. Thus if one day we need to change to another implementation
 * we won't be tied to WikiModel.
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
