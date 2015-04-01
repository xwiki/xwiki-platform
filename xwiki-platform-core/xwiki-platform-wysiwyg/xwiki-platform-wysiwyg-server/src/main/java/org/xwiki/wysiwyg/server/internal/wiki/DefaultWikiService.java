/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wysiwyg.server.internal.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The default implementation for {@link org.xwiki.gwt.wysiwyg.client.wiki.WikiService}.
 * <p>
 * NOTE: Keep here only the methods that are implemented using the old XWiki core.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWikiService extends AbstractWikiService
{
    /** Execution context handler, needed for accessing the XWikiContext. */
    @Inject
    private Execution execution;

    /**
     * The component used to serialize an entity reference relative to another entity reference.
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    @Deprecated
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public List<String> getVirtualWikiNames()
    {
        List<String> virtualWikiNamesList = new ArrayList<String>();
        try {
            virtualWikiNamesList = getXWikiContext().getWiki().getVirtualWikisDatabaseNames(getXWikiContext());
            // put the current, default database if nothing is inside
            if (virtualWikiNamesList.size() == 0) {
                virtualWikiNamesList.add(getXWikiContext().getWikiId());
            }
            Collections.sort(virtualWikiNamesList);
        } catch (Exception e) {
            this.logger.error(e.getLocalizedMessage(), e);
        }
        return virtualWikiNamesList;
    }

    @Override
    protected String getCurrentUserRelativeTo(String wikiName)
    {
        return compactEntityReferenceSerializer.serialize(getXWikiContext().getUserReference(), new WikiReference(
            wikiName));
    }

    @Override
    protected List<WikiPage> getWikiPages(List<DocumentReference> documentReferences)
    {
        XWikiContext context = getXWikiContext();
        List<WikiPage> wikiPages = new ArrayList<WikiPage>();
        for (DocumentReference documentReference : documentReferences) {
            try {
                WikiPage wikiPage = new WikiPage();
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                wikiPage.setReference(entityReferenceConverter.convert(documentReference).getEntityReference());
                wikiPage.setTitle(document.getRenderedTitle(context));
                wikiPage.setUrl(document.getURL("view", context));
                wikiPages.add(wikiPage);
            } catch (Exception e) {
                this.logger.warn("Failed to load document [{}]", documentReference, e);
            }
        }
        return wikiPages;
    }

    @Override
    public List<Attachment> getAttachments(WikiPageReference reference)
    {
        try {
            XWikiContext context = getXWikiContext();
            List<Attachment> attachments = new ArrayList<Attachment>();
            DocumentReference documentReference = entityReferenceConverter.convert(reference);
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            for (XWikiAttachment attach : doc.getAttachmentList()) {
                org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference attachmentReference =
                    new org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference(attach.getFilename(), reference);
                Attachment currentAttach = new Attachment();
                currentAttach.setUrl(doc.getAttachmentURL(attach.getFilename(), context));
                currentAttach.setReference(attachmentReference.getEntityReference());
                currentAttach.setMimeType(attach.getMimeType(context));
                attachments.add(currentAttach);
            }
            return attachments;
        } catch (Exception e) {
            this.logger.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to retrieve the list of attachments.", e);
        }
    }
}
