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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import org.xwiki.wysiwyg.server.wiki.LinkService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The default implementation for {@link WikiService}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWikiService implements WikiService
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(DefaultWikiService.class);

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Execution context handler, needed for accessing the XWikiContext. */
    @Inject
    private Execution execution;

    /**
     * The service used to create links.
     */
    @Inject
    private LinkService linkService;

    /**
     * The component that protects us against cross site request forgery by using a secret token validation mechanism.
     * The secret token is added to the query string of the upload URL and then checked in the upload action when files
     * are uploaded.
     */
    @Inject
    private CSRFToken csrf;

    /**
     * The component used to resolve a string document reference.
     */
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * The component used to serialize an entity reference.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The object used to convert between client and server entity reference.
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

    @Override
    public Boolean isMultiWiki()
    {
        return getXWikiContext().getWiki().isVirtualMode();
    }

    @Override
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

    @Override
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

    @Override
    public List<String> getPageNames(String wikiName, String spaceName)
    {
        String query = "where doc.space = ? order by doc.fullName asc";
        List<String> parameters = Arrays.asList(spaceName);
        List<String> pagesNames = new ArrayList<String>();
        for (DocumentReference documentReference : searchDocumentReferences(wikiName, query, parameters, 0, 0)) {
            pagesNames.add(documentReference.getName());
        }
        return pagesNames;
    }

    @Override
    public List<WikiPage> getRecentlyModifiedPages(String wikiName, int start, int count)
    {
        String query = "where doc.author = ? order by doc.date desc";
        List<String> parameters = Arrays.asList(getCurrentUserRelativeTo(wikiName));
        return getWikiPages(searchDocumentReferences(wikiName, query, parameters, start, count));
    }

    @Override
    public List<WikiPage> getMatchingPages(String wikiName, String keyword, int start, int count)
    {
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

        return getWikiPages(searchDocumentReferences(wikiName, query, parameters, start, count));
    }

    /**
     * Searches documents in the specified wiki.
     * 
     * @param wikiName the wiki where to search for documents
     * @param query the query to execute
     * @param parameters the query parameters
     * @param start the position in the result set where to start
     * @param count the maximum number of documents to retrieve
     * @return the documents from the specified wiki that match the given query
     */
    private List<DocumentReference> searchDocumentReferences(String wikiName, String query, List<String> parameters,
        int start, int count)
    {
        XWikiContext context = getXWikiContext();
        String database = context.getDatabase();

        try {
            if (wikiName != null) {
                context.setDatabase(wikiName);
            }
            return context.getWiki().getStore().searchDocumentReferences(query, count, start, parameters, context);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to search XWiki pages.", e);
        } finally {
            if (wikiName != null) {
                context.setDatabase(database);
            }
        }
    }

    /**
     * @param wikiName the name of a wiki
     * @return the name of the current user, relative to the specified wiki
     */
    private String getCurrentUserRelativeTo(String wikiName)
    {
        XWikiContext context = getXWikiContext();
        String currentUser = context.getUser();
        String currentWiki = context.getDatabase();
        if (!currentWiki.equals(wikiName)) {
            WikiReference currentWikiRef = new WikiReference(currentWiki);
            DocumentReference currentUserRef = documentReferenceResolver.resolve(currentUser, currentWikiRef);
            currentUser = entityReferenceSerializer.serialize(currentUserRef, new WikiReference(wikiName));
        }
        return currentUser;
    }

    /**
     * Helper function to create a list of {@link WikiPage}s from a list of document references.
     * 
     * @param documentReferences a list of document references
     * @return the list of {@link WikiPage}s corresponding to the given document references
     */
    private List<WikiPage> getWikiPages(List<DocumentReference> documentReferences)
    {
        XWikiContext context = getXWikiContext();
        List<WikiPage> wikiPages = new ArrayList<WikiPage>();
        for (DocumentReference documentReference : documentReferences) {
            try {
                WikiPage wikiPage = new WikiPage();
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                wikiPage.setReference(entityReferenceConverter.convert(documentReference).getEntityReference());
                wikiPage.setTitle(document.getRenderedTitle(Syntax.XHTML_1_0, context));
                wikiPage.setUrl(document.getURL("view", context));
                wikiPages.add(wikiPage);
            } catch (XWikiException e) {
                LOG.warn("Failed to load document " + documentReference, e);
            }
        }
        return wikiPages;
    }

    @Override
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        ResourceReference destination)
    {
        return linkService.getEntityConfig(origin, destination);
    }

    @Override
    public Attachment getAttachment(org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference attachmentReference)
    {
        XWikiContext context = getXWikiContext();
        // Clean attachment filename to be synchronized with all attachment operations.
        String cleanedFileName = context.getWiki().clearName(attachmentReference.getFileName(), false, true, context);
        DocumentReference documentReference =
            entityReferenceConverter.convert(attachmentReference.getWikiPageReference());
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

        org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference foundAttachmentReference = attachmentReference.clone();
        foundAttachmentReference.setFileName(cleanedFileName);

        Attachment attach = new Attachment();
        attach.setReference(foundAttachmentReference.getEntityReference());
        attach.setUrl(doc.getAttachmentURL(cleanedFileName, context));
        return attach;
    }

    @Override
    public List<Attachment> getImageAttachments(WikiPageReference reference)
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        List<Attachment> allAttachments = getAttachments(reference);
        for (Attachment attachment : allAttachments) {
            if (attachment.getMimeType().startsWith("image/")) {
                imageAttachments.add(attachment);
            }
        }
        return imageAttachments;
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
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to retrieve the list of attachments.", e);
        }
    }

    @Override
    public String getUploadURL(WikiPageReference reference)
    {
        String queryString = "form_token=" + csrf.getToken();
        return documentAccessBridge.getDocumentURL(entityReferenceConverter.convert(reference), "upload", queryString,
            null);
    }

    @Override
    public ResourceReference parseLinkReference(String linkReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        return linkService.parseLinkReference(linkReference, baseReference);
    }
}
