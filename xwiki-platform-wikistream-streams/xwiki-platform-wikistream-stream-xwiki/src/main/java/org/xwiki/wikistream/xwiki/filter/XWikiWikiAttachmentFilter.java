package org.xwiki.wikistream.xwiki.filter;

import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.filter.WikiAttachmentFilter;

@Unstable
public interface XWikiWikiAttachmentFilter extends WikiAttachmentFilter
{
    /**
     * @type String
     */
    String PARAMETER_JRCSREVISIONS = "xwiki_attachment_jrcsrevisions";
}
