/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.xmlrpc;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Page;
import org.codehaus.swizzle.confluence.PageHistorySummary;
import org.codehaus.swizzle.confluence.PageSummary;
import org.codehaus.swizzle.confluence.SearchResult;
import org.codehaus.swizzle.confluence.Space;
import org.codehaus.swizzle.confluence.SpaceSummary;
import org.codehaus.swizzle.confluence.User;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiEngineContext;

import org.codehaus.swizzle.confluence.Comment;
import org.suigeneris.jrcs.rcs.Version;

/**
 * @version $Id: $
 */
public class DomainObjectFactory
{
    
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    // TODO ":" is also used in XWiki for selecting a particular database. Change ?
    private static final String PAGE_VERSION_SEPARATOR = ":";

    private static final String OBJNO_SEPARATOR = ";";
    
    private static final Log log = LogFactory.getLog(DomainObjectFactory.class);

    // TODO our ids are unique still they are sensitive to renaming
    // Q: is this a problem ? If so we really need the numeric ids (globally unique)
    public XWikiDocument getDocFromPageId(String pageId, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        if (!pageId.contains(PAGE_VERSION_SEPARATOR)) {
            // Current version of document
            if (xwiki.exists(pageId, context)) {
                return xwiki.getDocument(pageId, context);
            } else {
                throw exception("The page '" + pageId + "' does not exist.");
            }
        } else {
            int i = pageId.indexOf(PAGE_VERSION_SEPARATOR);
            String fullName = pageId.substring(0, i);
            String version = pageId.substring(i + 1);
            if (xwiki.exists(fullName, context)) {
                XWikiDocument currentDoc = xwiki.getDocument(fullName, context);
                return xwiki.getDocument(currentDoc, version, context);
            } else {
                throw exception("The page '" + fullName + "' does not exist.");
            }
        }
    }

    /**
     * 
     * @param commentId
     * @param context
     * @return
     * @throws XWikiException
     */
    public Object[] getDocObjectPair(String commentId, XWikiContext context)
        throws XWikiException
    {
        int i = commentId.indexOf(OBJNO_SEPARATOR);
        if (i < 0) {
            throw exception("Invalid comment ID.");
        }
        String pageId = commentId.substring(0, i);
        int nb = 0;
        try {
            nb = (new Integer(commentId.substring(i + 1))).intValue();
        } catch (NumberFormatException nfe) {
            throw exception("Invalid comment ID.");
        }

        XWikiDocument doc = getDocFromPageId(pageId, context);

        Vector comments = doc.getComments();
        if (nb >= comments.size()) {
            throw exception("Invalid comment ID.");
        }
        BaseObject obj = (BaseObject) comments.get(nb);

        // TODO this is more general, is it also more efficient?
        // XWiki xwiki = context.getWiki();
        // BaseObject obj = doc.getObject(xwiki.getCommentsClass(context).getName(), nb);

        if (obj == null) {
            throw exception("This comment was already removed.");
        }

        return new Object[] {doc, obj};
    }

    /**
     * Notes:
     * <ul>
     * <li>XWiki ignores the content type field set by the user and uses the file extension instead
     * to determine it (Confluence requires this field to be set by the user).</li>
     * <li>XWiki always sets the id of the attachments to the empty string since this field is
     * totally useless.</li>
     * <li>XWiki always sets the title of the attachment to its file name.</li>
     * </ui>
     * 
     * @param doc the (@link com.xpn.xwiki.XWikiDocument), used to create the Attachment. The reason
     *            we need its that some information for creating the Attachment is available only
     *            from the XWikiDocument object and not in the passed XWikiAttachment.
     * @param attachment the (@link com.xpn.xwiki.XWikiAttachment), used to create the Attachment
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki
     *            primitives for loading documents
     */
    public Attachment createAttachment(XWikiDocument doc, XWikiAttachment attachment,
        XWikiContext context)
    {
        Attachment result = new Attachment();

        // Ids for attachments are useless so we don't set them (Confluence does)
        result.setId("");
        result.setPageId(doc.getFullName());
        // We use the filename as the document title (Confluence does the same)
        result.setTitle(attachment.getFilename());
        result.setFileName(attachment.getFilename());
        result.setFileSize(attachment.getFilesize());
        XWiki xwiki = context.getWiki();
        XWikiEngineContext engineContext = xwiki.getEngineContext();
        String mimeType = engineContext.getMimeType(attachment.getFilename());
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        result.setContentType(mimeType);
        result.setCreator(attachment.getAuthor());
        result.setCreated(attachment.getDate());
        result.setUrl(doc.getAttachmentURL(attachment.getFilename(), "download", context));
        result.setComment(attachment.getComment());

        return result;
    }

    public Comment createComment(XWikiDocument doc, BaseObject obj, XWikiContext context)
    {
        Comment result = new Comment();

        if (doc.isMostRecent()) {
            result.setId(doc.getFullName() + ";" + obj.getNumber());
            result.setPageId(doc.getFullName());
            result.setUrl(doc.getURL("view", context));
        } else {
            result.setId(doc.getFullName() + ":" + doc.getVersion() + ";" + obj.getNumber());
            result.setPageId(doc.getFullName() + ":" + doc.getVersion());
            result.setUrl(doc.getURL("view", "rev=" + doc.getVersion(), context));
        }
        result.setTitle(doc.getName());
        result.setContent(obj.getStringValue("comment"));
        result.setCreated(obj.getDateValue("date"));
        result.setCreator(obj.getStringValue("author"));

        return result;
    }

    public Page createPage(XWikiDocument doc, XWikiContext context)
    {
        Page result = new Page();

        // since we don't have multiple inheritance
        // we had to copy paste this initial part from PageSummary
        if (doc.isMostRecent()) {
            // Current version of document
            result.setId(doc.getFullName());
            result.setUrl(doc.getURL("view", context));
        } else {
            // Old version of document
            result.setId(doc.getFullName() + ":" + doc.getVersion());
            result.setUrl(doc.getURL("view", "rev=" + doc.getVersion(), context));
        }
        result.setSpace(doc.getSpace());
        result.setParentId(doc.getParent());
        result.setTitle(doc.getName());
        result.setLocks(0);

        result.setVersion(constructVersion(doc.getRCSVersion()));
        result.setContent(doc.getContent());
        result.setCreated(doc.getCreationDate());
        result.setCreator(doc.getAuthor());
        result.setModified(doc.getDate());
        result.setModifier(doc.getAuthor());
        result.setHomePage((doc.getName().equals("WebHome")));

        return result;
    }

    /**
     * Notes:
     * <ul>
     * <li>XWiki does not have mutex locks to getLocks always returns 0.</li>
     * </ul>
     */
    public PageSummary createPageSummary(XWikiDocument doc, XWikiContext context)
    {
        PageSummary result = new PageSummary();

        if (doc.isMostRecent()) {
            // Current version of document
            result.setId(doc.getFullName());
            result.setUrl(doc.getURL("view", context));
        } else {
            // Old version of document
            result.setId(doc.getFullName() + ":" + doc.getVersion());
            result.setUrl(doc.getURL("view", "rev=" + doc.getVersion(), context));
        }

        result.setSpace(doc.getSpace());
        result.setParentId(doc.getParent());
        result.setTitle(doc.getName());
        result.setLocks(0);

        return result;
    }

    public PageHistorySummary createPageHistorySummary(XWikiDocument document)
    {
        PageHistorySummary result = new PageHistorySummary();

        result.setId(document.getFullName() + ":" + document.getVersion());
        result.setVersion(constructVersion(document.getRCSVersion()));
        result.setModified(document.getDate());
        result.setModifier(document.getAuthor());

        return result;
    }

    public SearchResult createSearchResult(XWikiDocument document, XWikiContext context)
    {
        SearchResult result = new SearchResult();

        result.setTitle(document.getName());
        result.setId(document.getFullName());
        result.setUrl(document.getURL("view", context));
        result.setType("page");
        String content = document.getContent();
        // TODO is this a good way to generate excerpts?
        if (content.length() <= 256) {
            result.setExcerpt(content);
        } else {
            result.setExcerpt(content.substring(0, 256));
        }

        return result;
    }

    public Space createSpace(String key, String name, String url, String description,
        String homepage)
    {
        Space result = new Space();

        result.setKey(key);
        result.setName(name);
        result.setUrl(url);

        result.setDescription(description);
        result.setHomepage(homepage);

        return result;
    }

    /**
     * @param key of the space (usually the Space's name as it's unique)
     * @param name of the space
     * @param url to view the space online. Example: "http://server/xwiki/bin/view/Space/WebHome"
     */
    public SpaceSummary createSpaceSummary(String key, String name, String url)
    {
        SpaceSummary result = new SpaceSummary();

        result.setKey(key);
        result.setName(name);
        result.setUrl(url);
        // TODO we do not set the type ... document at least

        return result;
    }

    public User createUser(XWikiDocument userdoc, XWikiContext context)
    {
        User result = new User();

        result.setName(userdoc.getName());
        result.setFullname(userdoc.getStringValue("XWiki.XWikiUsers", "fullName"));
        result.setEmail(userdoc.getStringValue("XWiki.XWikiUsers", "email"));
        result.setUrl(userdoc.getURL("view", context));

        return result;
    }

    private static int constructVersion(Version ver)
    {
        return ((ver.at(0) - 1) << 16) + ver.at(1);
    }
    
    private XWikiException exception(String message)
    {
        log.info("Exception thrown to XML-RPC client: " + message);
        XWikiException ex = new XWikiException();
        ex.setModule(XWikiException.MODULE_XWIKI_XMLRPC);
        ex.setMessage(message);
        return ex;
    }
}
