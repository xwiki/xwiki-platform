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
package com.xpn.xwiki.wysiwyg.server.internal.wiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.wysiwyg.server.wiki.LinkService;

/**
 * The default implementation for {@link WikiService}.
 * 
 * @version $Id$
 */
public class DefaultWikiService implements WikiService
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(DefaultWikiService.class);

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /** Execution context handler, needed for accessing the XWikiContext. */
    @Requirement
    private Execution execution;

    /**
     * The service used to create links.
     */
    @Requirement
    private LinkService linkService;

    /**
     * The object used to convert between client-side entity references and server-side entity references.
     */
    private final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#isMultiWiki()
     */
    public Boolean isMultiWiki()
    {
        return getXWikiContext().getWiki().isVirtualMode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getVirtualWikiNames()
     */
    public List<String> getVirtualWikiNames()
    {
        List<String> virtualWikiNamesList = new ArrayList<String>();
        try {
            virtualWikiNamesList = getXWikiContext().getWiki().getVirtualWikisDatabaseNames(getXWikiContext());
            // put the current, default database if nothing is inside
            if (virtualWikiNamesList.size() == 0) {
                virtualWikiNamesList.add(getXWikiContext().getDatabase());
            }
            Collections.sort(virtualWikiNamesList);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        return virtualWikiNamesList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getSpaceNames(String)
     */
    public List<String> getSpaceNames(String wikiName)
    {
        List<String> spaceNamesList = new ArrayList<String>();
        String database = getXWikiContext().getDatabase();
        try {
            if (wikiName != null) {
                getXWikiContext().setDatabase(wikiName);
            }
            spaceNamesList = getXWikiContext().getWiki().getSpaces(getXWikiContext());
            // remove the blacklisted spaces from the all spaces list
            spaceNamesList.removeAll(getBlackListedSpaces());
            Collections.sort(spaceNamesList);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        } finally {
            if (wikiName != null) {
                getXWikiContext().setDatabase(database);
            }
        }
        return spaceNamesList;
    }

    /**
     * Helper function to retrieve the blacklisted spaces in this session, as they've been set in xwikivars.vm, when the
     * page edited with this wysiwyg was loaded. <br />
     * TODO: remove this when the public API will exclude them by default, or they'll be set in the config.
     * 
     * @return the list of blacklisted spaces from the session
     */
    @SuppressWarnings("unchecked")
    private List<String> getBlackListedSpaces()
    {
        // get the blacklisted spaces from the session
        List<String> blacklistedSpaces =
            (ArrayList<String>) getXWikiContext().getRequest().getSession().getAttribute("blacklistedSpaces");
        // always return a list, even if blacklisted spaces variable wasn't set
        if (blacklistedSpaces == null) {
            blacklistedSpaces = Collections.emptyList();
        }
        return blacklistedSpaces;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getPageNames(String, String)
     */
    public List<String> getPageNames(String wikiName, String spaceName)
    {
        XWikiContext xcontext = getXWikiContext();
        String database = xcontext.getDatabase();
        List<DocumentReference> documentReferences = null;
        String query = "where doc.space = ? order by doc.fullName asc";
        List<String> parameters = Arrays.asList(spaceName);
        try {
            if (wikiName != null) {
                xcontext.setDatabase(wikiName);
            }
            documentReferences = xcontext.getWiki().getStore().searchDocumentReferences(query, parameters, xcontext);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        } finally {
            if (wikiName != null) {
                xcontext.setDatabase(database);
            }
        }
        List<String> pagesNames = new ArrayList<String>();
        if (documentReferences != null) {
            for (DocumentReference documentReference : documentReferences) {
                pagesNames.add(documentReference.getName());
            }
        }
        return pagesNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getRecentlyModifiedPages(int, int)
     */
    public List<WikiPage> getRecentlyModifiedPages(int start, int count)
    {
        try {
            XWikiContext context = getXWikiContext();
            String query = "where doc.author = ? order by doc.date desc";
            List<String> parameters = Arrays.asList(context.getUser());
            List<DocumentReference> documentReferences =
                context.getWiki().getStore().searchDocumentReferences(query, count, start, parameters, context);
            return getWikiPages(documentReferences);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to retrieve the lists of recently modified pages.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getMatchingPages(String, int, int)
     */
    public List<WikiPage> getMatchingPages(String keyword, int start, int count)
    {
        try {
            List<String> blackListedSpaces = getBlackListedSpaces();
            String notInBlackListedSpaces = "";
            if (blackListedSpaces.size() > 0) {
                notInBlackListedSpaces =
                    "doc.web not in (?" + StringUtils.repeat(",?", blackListedSpaces.size() - 1) + ") and ";
            }
            String query =
                "where " + notInBlackListedSpaces + "(lower(doc.title) like '%'||?||'%' or"
                    + " lower(doc.fullName) like '%'||?||'%')" + " order by doc.fullName asc";
            List<String> parameters = new ArrayList<String>(blackListedSpaces);
            // Add twice the keyword, once for the document title and once for the document name.
            parameters.add(keyword.toLowerCase());
            parameters.add(keyword.toLowerCase());

            XWikiContext context = getXWikiContext();
            List<DocumentReference> documentReferences =
                context.getWiki().getStore().searchDocumentReferences(query, count, start, parameters, context);
            return getWikiPages(documentReferences);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to search XWiki pages.", e);
        }
    }

    /**
     * Helper function to create a list of {@link WikiPage}s from a list of document references.
     * 
     * @param documentReferences a list of document references
     * @return the list of {@link WikiPage}s corresponding to the given document references
     * @throws XWikiException if anything goes wrong while creating the list of {@link WikiPage}s
     */
    private List<WikiPage> getWikiPages(List<DocumentReference> documentReferences) throws XWikiException
    {
        List<WikiPage> wikiPages = new ArrayList<WikiPage>();
        for (DocumentReference documentReference : documentReferences) {
            WikiPage wikiPage = new WikiPage();
            XWikiContext context = getXWikiContext();
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            wikiPage.setReference(entityReferenceConverter.convert(documentReference));
            wikiPage.setTitle(document.getRenderedTitle(Syntax.XHTML_1_0, context));
            wikiPage.setUrl(document.getURL("view", context));
            wikiPages.add(wikiPage);
        }
        return wikiPages;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference,
     *      org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference destination)
    {
        return linkService.getEntityConfig(origin, destination);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getAttachment(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public Attachment getAttachment(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference attachmentReference)
    {
        XWikiContext context = getXWikiContext();
        // Clean attachment filename to be synchronized with all attachment operations.
        String cleanedFileName = context.getWiki().clearName(attachmentReference.getFileName(), false, true, context);
        DocumentReference documentReference =
            new DocumentReference(attachmentReference.getWikiName(), attachmentReference.getSpaceName(),
                attachmentReference.getPageName());
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            LOG.error("Failed to get attachment: there was a problem with getting the document on the server.", e);
            return null;
        }
        if (doc.isNew()) {
            LOG.warn(String.format("Failed to get attachment: %s document doesn't exist.", documentReference));
            return null;
        }
        if (doc.getAttachment(cleanedFileName) == null) {
            LOG.warn(String.format("Failed to get attachment: %s not found.", cleanedFileName));
            return null;
        }

        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference foundAttachmentReference =
            entityReferenceConverter.convert(documentReference);
        foundAttachmentReference.setType(attachmentReference.getType());
        foundAttachmentReference.setFileName(cleanedFileName);

        Attachment attach = new Attachment();
        attach.setReference(foundAttachmentReference);
        attach.setUrl(doc.getAttachmentURL(cleanedFileName, context));
        return attach;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getImageAttachments(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public List<Attachment> getImageAttachments(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference reference)
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        List<Attachment> allAttachments = getAttachments(reference);
        for (Attachment attachment : allAttachments) {
            if (attachment.getMimeType().startsWith("image/")) {
                attachment.getReference().setType(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.IMAGE);
                imageAttachments.add(attachment);
            }
        }
        return imageAttachments;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getAttachments(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public List<Attachment> getAttachments(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference reference)
    {
        try {
            XWikiContext context = getXWikiContext();
            List<Attachment> attachments = new ArrayList<Attachment>();
            DocumentReference documentReference =
                new DocumentReference(reference.getWikiName(), reference.getSpaceName(), reference.getPageName());
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            for (XWikiAttachment attach : doc.getAttachmentList()) {
                org.xwiki.gwt.wysiwyg.client.wiki.EntityReference attachmentReference =
                    entityReferenceConverter.convert(documentReference);
                attachmentReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT);
                attachmentReference.setFileName(attach.getFilename());

                Attachment currentAttach = new Attachment();
                currentAttach.setUrl(doc.getAttachmentURL(attach.getFilename(), context));
                currentAttach.setReference(attachmentReference);
                currentAttach.setMimeType(attach.getMimeType(context));
                attachments.add(currentAttach);
            }
            return attachments;
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to retrieve the list of attachments.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getUploadURL(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public String getUploadURL(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference documentReference)
    {
        return documentAccessBridge.getDocumentURL(new DocumentReference(documentReference.getWikiName(),
            documentReference.getSpaceName(), documentReference.getPageName()), "upload", null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#parseLinkReference(String, org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType,
     *      org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public org.xwiki.gwt.wysiwyg.client.wiki.EntityReference parseLinkReference(String linkReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType entityType,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        return linkService.parseLinkReference(linkReference, entityType, baseReference);
    }
}
