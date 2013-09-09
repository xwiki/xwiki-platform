package org.xwiki.wikistream.instance.internal;

import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiDocumentProperties extends XWikiAttachmentProperties implements BaseObjectProperties
{
    private boolean withWikiDocumentRevisions = true;

    private boolean withWikiObjects = true;

    private boolean withWikiAttachments = true;

    private boolean withWikiDocumentContentHTML = false;

    public boolean isWithWikiDocumentRevisions()
    {
        return this.withWikiDocumentRevisions;
    }

    public void setWithWikiDocumentRevisions(boolean withWikiDocumentRevisions)
    {
        this.withWikiDocumentRevisions = withWikiDocumentRevisions;
    }

    public boolean isWithWikiAttachments()
    {
        return this.withWikiAttachments;
    }

    public void setWithWikiAttachments(boolean withWikiAttachments)
    {
        this.withWikiAttachments = withWikiAttachments;
    }

    public boolean isWithWikiObjects()
    {
        return this.withWikiObjects;
    }

    public void setWithWikiObjects(boolean withWikiObjects)
    {
        this.withWikiObjects = withWikiObjects;
    }

    public boolean isWithWikiDocumentContentHTML()
    {
        return this.withWikiDocumentContentHTML;
    }

    public void setWithWikiDocumentContentHTML(boolean withWikiDocumentContentHTML)
    {
        this.withWikiDocumentContentHTML = withWikiDocumentContentHTML;
    }
}
