package org.xwiki.wikistream.databaseold.internal;

public class XWikiAttachmentProperties
{
    private boolean withWikiAttachmentContent = true;

    private boolean withWikiAttachmentRevisions = true;

    public boolean isWithWikiAttachmentContent()
    {
        return this.withWikiAttachmentContent;
    }

    public void setWithWikiAttachmentContent(boolean withWikiAttachmentContent)
    {
        this.withWikiAttachmentContent = withWikiAttachmentContent;
    }

    public boolean isWithWikiAttachmentRevisions()
    {
        return this.withWikiAttachmentRevisions;
    }

    public void setWithWikiAttachmentRevisions(boolean withWikiAttachmentsRevisions)
    {
        this.withWikiAttachmentRevisions = withWikiAttachmentsRevisions;
    }
}
