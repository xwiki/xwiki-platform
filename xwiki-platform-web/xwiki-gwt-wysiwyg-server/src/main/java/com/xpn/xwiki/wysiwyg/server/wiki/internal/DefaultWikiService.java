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
package com.xpn.xwiki.wysiwyg.server.wiki.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

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
     * The name of the view action.
     */
    private static final String VIEW_ACTION = "view";

    /**
     * The component used to serialize XWiki document references.
     */
    @Requirement
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to construct a valid document reference.
     */
    @Requirement("default/reference")
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /** Execution context handler, needed for accessing the XWikiContext. */
    @Requirement
    private Execution execution;

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
        List<String> pagesNames = new ArrayList<String>();
        List<String> params = new ArrayList<String>();
        params.add(spaceName);
        String query = "where doc.space = ? order by doc.fullName asc";
        try {
            if (wikiName != null) {
                xcontext.setDatabase(wikiName);
            }
            documentReferences = xcontext.getWiki().getStore().searchDocumentReferences(query, params, xcontext);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        } finally {
            if (wikiName != null) {
                xcontext.setDatabase(database);
            }
        }
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
            // NOTE: Currently Oracle doesn't support distinct when a row is BLOB or CLOB. As a consequence we cannot
            // query distinct documents. This is not a problem though as long as we query only the XWikiDocument table,
            // which shouldn't contain duplicates.
            List<XWikiDocument> docs =
                context.getWiki().search(
                    "select doc from XWikiDocument doc where doc.author='" + context.getUser()
                        + "' order by doc.date desc", count, start, context);
            return prepareDocumentResultsList(docs);
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
            String quote = "'";
            String doubleQuote = "''";
            // FIXME: this fullname comparison with the keyword does not contain the wiki name
            String escapedKeyword = keyword.replaceAll(quote, doubleQuote).toLowerCase();
            // add condition for the doc to not be in the list of blacklisted spaces.
            // TODO: might be a pb with scalability of this
            String noBlacklistedSpaces = "";
            List<String> blackListedSpaces = getBlackListedSpaces();
            if (!blackListedSpaces.isEmpty()) {
                StringBuffer spacesList = new StringBuffer();
                for (String bSpace : blackListedSpaces) {
                    if (spacesList.length() > 0) {
                        spacesList.append(", ");
                    }
                    spacesList.append(quote);
                    spacesList.append(bSpace.replaceAll(quote, doubleQuote));
                    spacesList.append(quote);
                }
                noBlacklistedSpaces = "doc.web not in (" + spacesList.toString() + ") and ";
            }
            // NOTE: Currently Oracle doesn't support distinct when a row is BLOB or CLOB. As a consequence we cannot
            // query distinct documents. This is not a problem though as long as we query only the XWikiDocument table,
            // which shouldn't contain duplicates.
            List<XWikiDocument> docs =
                getXWikiContext().getWiki().search(
                    "select doc from XWikiDocument as doc where " + noBlacklistedSpaces + "(lower(doc.title) like '%"
                        + escapedKeyword + "%' or lower(doc.fullName) like '%" + escapedKeyword + "%')", count, start,
                    getXWikiContext());
            return prepareDocumentResultsList(docs);
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to search XWiki pages.", e);
        }
    }

    /**
     * Helper function to prepare a list of {@link WikiPage}s (with full name, title, etc) from a list of document
     * names.
     * 
     * @param docs the list of the documents to include in the list
     * @return the list of {@link WikiPage}s corresponding to the passed names
     * @throws XWikiException if anything goes wrong retrieving the documents
     */
    private List<WikiPage> prepareDocumentResultsList(List<XWikiDocument> docs) throws XWikiException
    {
        List<WikiPage> results = new ArrayList<WikiPage>();
        for (XWikiDocument doc : docs) {
            WikiPage page = new WikiPage();
            page.setName(entityReferenceSerializer.serialize(doc.getDocumentReference()));
            page.setTitle(doc.getRenderedTitle(Syntax.XHTML_1_0, getXWikiContext()));
            page.setURL(doc.getURL(VIEW_ACTION, getXWikiContext()));
            results.add(page);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getPageLink(String, String, String, String, String)
     */
    public LinkConfig getPageLink(String wikiName, String spaceName, String pageName, String revision, String anchor)
    {
        String queryString = StringUtils.isEmpty(revision) ? null : "rev=" + revision;
        DocumentReference documentReference = prepareDocumentReference(wikiName, spaceName, pageName);
        // get the url to the targeted document from the bridge
        String documentReferenceAsString = this.entityReferenceSerializer.serialize(documentReference);
        String pageURL = documentAccessBridge.getURL(documentReferenceAsString, VIEW_ACTION, queryString, anchor);

        // get a document name serializer to return the page reference
        if (queryString != null) {
            documentReferenceAsString += "?" + queryString;
        }
        if (!StringUtils.isEmpty(anchor)) {
            documentReferenceAsString += "#" + anchor;
        }

        // create the link reference
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.setUrl(pageURL);
        linkConfig.setReference(documentReferenceAsString);

        return linkConfig;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getAttachment(String, String, String, String)
     */
    public Attachment getAttachment(String wikiName, String spaceName, String pageName, String attachmentName)
    {
        Attachment attach = new Attachment();

        XWikiContext context = getXWikiContext();
        // clean attachment filename to be synchronized with all attachment operations
        String cleanedFileName = context.getWiki().clearName(attachmentName, false, true, context);
        DocumentReference documentReference = prepareDocumentReference(wikiName, spaceName, pageName);
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            // there was a problem with getting the document on the server
            return null;
        }
        if (doc.isNew()) {
            // the document does not exist, therefore nor does the attachment. Return null
            return null;
        }
        // check for the existence of the attachment
        if (doc.getAttachment(cleanedFileName) == null) {
            // attachment is not there, something bad must have happened
            return null;
        }
        // all right, now set the reference and url and return
        String attachmentReferenceAsString =
            this.entityReferenceSerializer.serialize(new AttachmentReference(cleanedFileName, documentReference));
        attach.setReference(attachmentReferenceAsString);
        attach.setURL(doc.getAttachmentURL(cleanedFileName, context));

        return attach;
    }

    /**
     * Gets a document reference from the passed parameters, handling the empty wiki, empty space or empty page name.
     * 
     * @param wiki the wiki of the document
     * @param space the space of the document
     * @param page the page name of the targeted document
     * @return the completed {@link DocumentReference} corresponding to the passed parameters, with all the missing
     *         values completed with defaults
     * @since 2.2M1
     */
    protected DocumentReference prepareDocumentReference(String wiki, String space, String page)
    {
        // Note: we use the default normalizer instead of the current normalizer because the execution context in which
        // this component is used doesn't have a current document. This component is used to respond to GWT-RPC requests
        // which don't have the information required to detect the current document (e.g. these informations can't be
        // extracted from the request URL).

        EntityReference reference = null;
        if (!StringUtils.isEmpty(wiki)) {
            reference = new EntityReference(wiki, EntityType.WIKI);
        }
        if (!StringUtils.isEmpty(space)) {
            reference = new EntityReference(space, EntityType.SPACE, reference);
        }
        if (!StringUtils.isEmpty(page)) {
            reference = new EntityReference(page, EntityType.DOCUMENT, reference);
        }

        DocumentReference resolvedReference = this.defaultReferenceDocumentReferenceResolver.resolve(reference);
        if (StringUtils.isEmpty(wiki)) {
            resolvedReference.setWikiReference(new WikiReference(getXWikiContext().getDatabase()));
        }
        return resolvedReference;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getImageAttachments(String, String, String)
     */
    public List<Attachment> getImageAttachments(String wikiName, String spaceName, String pageName)
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        List<Attachment> allAttachments = getAttachments(wikiName, spaceName, pageName);
        for (Attachment attachment : allAttachments) {
            if (attachment.getMimeType().startsWith("image/")) {
                imageAttachments.add(attachment);
            }
        }
        return imageAttachments;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiService#getAttachments(String, String, String)
     */
    public List<Attachment> getAttachments(String wikiName, String spaceName, String pageName)
    {
        try {
            XWikiContext context = getXWikiContext();
            List<Attachment> attachments = new ArrayList<Attachment>();
            DocumentReference documentReference = prepareDocumentReference(wikiName, spaceName, pageName);
            XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
            for (XWikiAttachment attach : doc.getAttachmentList()) {
                Attachment currentAttach = new Attachment();
                currentAttach.setFileName(attach.getFilename());
                currentAttach.setURL(doc.getAttachmentURL(attach.getFilename(), context));
                currentAttach.setReference(this.entityReferenceSerializer.serialize(new AttachmentReference(attach
                    .getFilename(), documentReference)));
                currentAttach.setMimeType(attach.getMimeType(context));
                attachments.add(currentAttach);
            }
            return attachments;
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to retrieve the list of attachments.", e);
        }
    }
}
