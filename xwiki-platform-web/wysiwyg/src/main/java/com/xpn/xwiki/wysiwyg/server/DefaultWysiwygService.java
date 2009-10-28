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
package com.xpn.xwiki.wysiwyg.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.gwt.api.server.XWikiServiceImpl;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;

/**
 * The default implementation for {@link WysiwygService}.
 * 
 * @version $Id$
 */
public class DefaultWysiwygService extends XWikiServiceImpl implements WysiwygService
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = 7555724420345951844L;

    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(DefaultWysiwygService.class);

    /**
     * The name of the view action.
     */
    private static final String VIEW_ACTION = "view";

    /**
     * @return The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    private DocumentAccessBridge getDocumentAccessBridge()
    {
        return Utils.getComponent(DocumentAccessBridge.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#isMultiWiki()
     */
    public Boolean isMultiWiki()
    {
        return getXWikiContext().getWiki().isVirtualMode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getVirtualWikiNames()
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
     * @see WysiwygService#getSpaceNames(String)
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
            (ArrayList<String>) getThreadLocalRequest().getSession().getAttribute("blacklistedSpaces");
        // always return a list, even if blacklisted spaces variable wasn't set
        if (blacklistedSpaces == null) {
            blacklistedSpaces = Collections.emptyList();
        }
        return blacklistedSpaces;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getPageNames(String, String)
     */
    public List<String> getPageNames(String wikiName, String spaceName)
    {
        String database = getXWikiContext().getDatabase();
        List<String> pagesFullNameList = null;
        List<String> pagesNameList = new ArrayList<String>();
        List<String> params = new ArrayList<String>();
        params.add(spaceName);
        String query = "where doc.space = ? order by doc.fullName asc";
        try {
            if (wikiName != null) {
                getXWikiContext().setDatabase(wikiName);
            }
            pagesFullNameList =
                getXWikiContext().getWiki().getStore().searchDocumentsNames(query, params, getXWikiContext());
        } catch (XWikiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        } finally {
            if (wikiName != null) {
                getXWikiContext().setDatabase(database);
            }
        }
        if (pagesFullNameList != null) {
            for (String p : pagesFullNameList) {
                pagesNameList.add(p.substring(params.get(0).length() + 1));
            }
        }
        return pagesNameList;
    }

    /**
     * {@inheritDoc}
     */
    public List<com.xpn.xwiki.gwt.api.client.Document> getRecentlyModifiedPages(int start, int count)
        throws XWikiGWTException
    {
        try {
            List<XWikiDocument> docs =
                getXWikiContext().getWiki().search(
                    "select distinct doc from XWikiDocument doc where 1=1 and doc.author='"
                        + getXWikiContext().getUser() + "' order by doc.date desc", count, start, getXWikiContext());
            return prepareDocumentResultsList(docs);
        } catch (XWikiException e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<com.xpn.xwiki.gwt.api.client.Document> getMatchingPages(String keyword, int start, int count)
        throws XWikiGWTException
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
            List<XWikiDocument> docs =
                getXWikiContext().getWiki().search(
                    "select distinct doc from XWikiDocument as doc where " + noBlacklistedSpaces
                        + "(lower(doc.title) like '%" + escapedKeyword + "%' or lower(doc.fullName) like '%"
                        + escapedKeyword + "%')", count, start, getXWikiContext());
            return prepareDocumentResultsList(docs);
        } catch (XWikiException e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * Helper function to prepare a list of {@link com.xpn.xwiki.gwt.api.client.Document}s (with fullname, title, etc)
     * from a list of document names.
     * 
     * @param docs the list of the documents to include in the list
     * @return the list of {@link com.xpn.xwiki.gwt.api.client.Document}s corresponding to the passed names
     * @throws XWikiException if anything goes wrong retrieving the documents
     */
    private List<com.xpn.xwiki.gwt.api.client.Document> prepareDocumentResultsList(List<XWikiDocument> docs)
        throws XWikiException
    {
        List<com.xpn.xwiki.gwt.api.client.Document> results = new ArrayList<com.xpn.xwiki.gwt.api.client.Document>();
        for (XWikiDocument doc : docs) {
            com.xpn.xwiki.gwt.api.client.Document xwikiDoc = new com.xpn.xwiki.gwt.api.client.Document();
            xwikiDoc.setFullName(doc.getFullName());
            xwikiDoc.setTitle(doc.getRenderedTitle(Syntax.XHTML_1_0, getXWikiContext()));
            // FIXME: shouldn't use upload URL here, but since we don't want to add a new field...
            xwikiDoc.setUploadURL(doc.getURL(VIEW_ACTION, getXWikiContext()));
            results.add(xwikiDoc);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getPageLink(String, String, String, String, String)
     */
    public LinkConfig getPageLink(String wikiName, String spaceName, String pageName, String revision, String anchor)
    {
        String queryString = StringUtils.isEmpty(revision) ? null : "rev=" + revision;
        DocumentName docName = prepareDocumentName(wikiName, spaceName, pageName);
        // get the url to the targeted document from the bridge
        DocumentNameSerializer serializer = Utils.getComponent(DocumentNameSerializer.class);
        String pageReference = serializer.serialize(docName);
        String pageURL = getDocumentAccessBridge().getURL(pageReference, VIEW_ACTION, queryString, anchor);

        // get a document name serializer to return the page reference
        if (queryString != null) {
            pageReference += "?" + queryString;
        }
        if (!StringUtils.isEmpty(anchor)) {
            pageReference += "#" + anchor;
        }

        // create the link reference
        LinkConfig linkConfig = new LinkConfig();
        linkConfig.setUrl(pageURL);
        linkConfig.setReference(pageReference);

        return linkConfig;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getAttachment(String, String, String, String)
     */
    public Attachment getAttachment(String wikiName, String spaceName, String pageName, String attachmentName)
    {
        Attachment attach = new Attachment();

        XWikiContext context = getXWikiContext();
        // clean attachment filename to be synchronized with all attachment operations
        String cleanedFileName = context.getWiki().clearName(attachmentName, false, true, context);
        DocumentName docName = prepareDocumentName(wikiName, spaceName, pageName);
        DocumentNameSerializer serializer = Utils.getComponent(DocumentNameSerializer.class);
        String docReference = serializer.serialize(docName);
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(docReference, context);
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
        String attachmentReference = getAttachmentReference(docReference, cleanedFileName);
        attach.setReference(attachmentReference);
        attach.setDownloadUrl(doc.getAttachmentURL(cleanedFileName, context));

        return attach;
    }

    /**
     * Gets a document name from the passed parameters, handling the empty wiki, empty space or empty page name.
     * 
     * @param wiki the wiki of the document
     * @param space the space of the document
     * @param page the page name of the targeted document
     * @return the completed {@link DocumentName} corresponding to the passed parameters, with all the missing values
     *         completed with defaults
     */
    protected DocumentName prepareDocumentName(String wiki, String space, String page)
    {
        String newPageName = StringUtils.isEmpty(page) ? page : clearXWikiName(page);
        String newSpaceName = StringUtils.isEmpty(space) ? space : clearXWikiName(space);
        String newWikiName = StringUtils.isEmpty(wiki) ? wiki : clearXWikiName(wiki);
        if (StringUtils.isEmpty(wiki)) {
            newWikiName = getXWikiContext().getDoc().getWikiName();
        }
        if (StringUtils.isEmpty(page)) {
            newPageName = "WebHome";
        }
        // if we have no space, link to the current doc's space
        if (StringUtils.isEmpty(space)) {
            if ((StringUtils.isEmpty(page)) && !StringUtils.isEmpty(wiki)) {
                // if we have no space set and no page but we have a wiki, then create a link to the mainpage of the
                // wiki
                newSpaceName = "Main";
            } else if (StringUtils.isEmpty(wiki)) {
                // if all are empty, create a link to the current document
                newSpaceName = getXWikiContext().getDoc().getSpace();
                newPageName = getXWikiContext().getDoc().getPageName();
            }
        }
        return new DocumentName(newWikiName, newSpaceName, newPageName);
    }

    /**
     * Clears forbidden characters out of the passed name, in a way which is consistent with the algorithm used in the
     * create page panel. <br />
     * FIXME: this function needs to be deleted when there will be a function to do this operation in a consistent
     * manner across the whole xwiki, and all calls to this function should be replaced with calls to that function.
     * 
     * @param name the name to clear from forbidden characters and transform in a xwiki name.
     * @return the cleared up xwiki name, ready to be used as a page or space name.
     */
    private String clearXWikiName(String name)
    {
        // remove all . since they're used as separators for space and page
        return name.replaceAll("\\.", "");
    }

    /**
     * Helper method to get the reference to an attachment. <br />
     * FIXME: which should be removed when such a serializer will exist in the bridge.
     * 
     * @param docReference the reference of the document to which the file is attached
     * @param attachName the name of the attached file to get the reference for
     * @return the reference of a file attached to a document, in the form wiki:Space.Page@filename.ext
     */
    private String getAttachmentReference(String docReference, String attachName)
    {
        return docReference + "@" + attachName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getImageAttachments(String, String, String)
     */
    public List<Attachment> getImageAttachments(String wikiName, String spaceName, String pageName)
        throws XWikiGWTException
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        try {
            List<Attachment> allAttachments = getAttachments(wikiName, spaceName, pageName);
            for (Attachment attachment : allAttachments) {
                if (attachment.getMimeType().startsWith("image/")) {
                    imageAttachments.add(attachment);
                }
            }
        } catch (XWikiGWTException e) {
            throw getXWikiGWTException(e);
        }
        return imageAttachments;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#getAttachments(String, String, String)
     */
    public List<Attachment> getAttachments(String wikiName, String spaceName, String pageName) throws XWikiGWTException
    {
        XWikiContext context = getXWikiContext();
        List<Attachment> attachments = new ArrayList<Attachment>();
        DocumentName docName = prepareDocumentName(wikiName, spaceName, pageName);
        DocumentNameSerializer serializer = Utils.getComponent(DocumentNameSerializer.class);
        String docReference = serializer.serialize(docName);
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(docReference, context);
            for (XWikiAttachment attach : doc.getAttachmentList()) {
                Attachment currentAttach = new Attachment();
                currentAttach.setFilename(attach.getFilename());
                currentAttach.setDownloadUrl(doc.getAttachmentURL(attach.getFilename(), context));
                currentAttach.setReference(getAttachmentReference(docReference, attach.getFilename()));
                currentAttach.setMimeType(attach.getMimeType(context));
                attachments.add(currentAttach);
            }
        } catch (XWikiException e) {
            throw getXWikiGWTException(e);
        }
        return attachments;
    }
}
