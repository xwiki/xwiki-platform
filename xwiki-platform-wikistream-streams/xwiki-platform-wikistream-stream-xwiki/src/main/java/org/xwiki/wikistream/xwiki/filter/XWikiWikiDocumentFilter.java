package org.xwiki.wikistream.xwiki.filter;

import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.filter.WikiDocumentFilter;

@Unstable
public interface XWikiWikiDocumentFilter extends WikiDocumentFilter
{
    /**
     * @type String
     */
    String PARAMETER_JRCSREVISIONS = "xwiki_document_jrcsrevisions";
}
