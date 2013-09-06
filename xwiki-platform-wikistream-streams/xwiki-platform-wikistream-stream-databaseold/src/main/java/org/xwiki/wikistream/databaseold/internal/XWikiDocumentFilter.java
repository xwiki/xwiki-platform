package org.xwiki.wikistream.databaseold.internal;

import org.xwiki.wikistream.filter.WikiClassFilter;
import org.xwiki.wikistream.filter.WikiClassPropertyFilter;
import org.xwiki.wikistream.filter.WikiObjectFilter;
import org.xwiki.wikistream.filter.WikiObjectPropertyFilter;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiDocumentFilter;

public interface XWikiDocumentFilter extends XWikiWikiDocumentFilter, XWikiAttachmentFilter, WikiClassFilter,
    WikiClassPropertyFilter, WikiObjectFilter, WikiObjectPropertyFilter
{
}
