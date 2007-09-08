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
 */
package com.xpn.xwiki.xmlrpc.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.swizzle.confluence.Confluence;
import org.codehaus.swizzle.confluence.ConfluenceException;
import org.codehaus.swizzle.confluence.IdentityObjectConvertor;
import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConfluenceException;

import com.xpn.xwiki.xmlrpc.model.Attachment;
import com.xpn.xwiki.xmlrpc.model.BlogEntry;
import com.xpn.xwiki.xmlrpc.model.Comment;
import com.xpn.xwiki.xmlrpc.model.Label;
import com.xpn.xwiki.xmlrpc.model.Page;
import com.xpn.xwiki.xmlrpc.model.PageSummary;
import com.xpn.xwiki.xmlrpc.model.ServerInfo;
import com.xpn.xwiki.xmlrpc.model.Space;
import com.xpn.xwiki.xmlrpc.model.User;
import com.xpn.xwiki.xmlrpc.model.UserInformation;
import com.xpn.xwiki.xmlrpc.model.swizzle.AttachmentImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.BlogEntryImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.BlogEntrySummaryImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.CommentImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.LabelImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.PageHistorySummaryImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.PageImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.PageSummaryImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.PermissionImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.SearchResultImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.ServerInfoImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.SpaceImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.SpaceSummaryImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.UserImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.UserInformationImpl;

public class SwizzleXWikiClient implements XWikiClient
{
    Confluence swizzle;

    public SwizzleXWikiClient(String endpoint) throws XWikiClientException
    {
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }

        if (!endpoint.endsWith("xmlrpc")) {
            endpoint += "/xwiki/xmlrpc";
        }

        try {
            swizzle = new Confluence(endpoint);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#login(java.lang.String, java.lang.String)
     */
    public void login(String username, String password) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            swizzle.login(username, password);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#logout()
     */
    public boolean logout() throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.logout();
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#exportSite(boolean)
     */
    public String exportSite(boolean exportAttachments) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.exportSite(exportAttachments);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getServerInfo()
     */
    public ServerInfo getServerInfo() throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new ServerInfoImpl(swizzle.getServerInfo());
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getSpaces()
     */
    public List getSpaces() throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getSpaces(), SpaceSummaryImpl.class,
                org.codehaus.swizzle.confluence.SpaceSummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getSpace(java.lang.String)
     */
    public Space getSpace(String spaceKey) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new SpaceImpl(swizzle.getSpace(spaceKey));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#exportSpace(java.lang.String, java.lang.String)
     */
    public String exportSpace(String spaceKey, String exportType) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.exportSpace(spaceKey, exportType);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addSpace(org.codehaus.swizzle.confluence.Space)
     */
    public Space addSpace(Space space) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new SpaceImpl(swizzle.addSpace(((SpaceImpl) space).getTarget()));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeSpace(java.lang.String)
     */
    public boolean removeSpace(String spaceKey) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeSpace(spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPages(java.lang.String)
     */
    public List getPages(String spaceKey) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getPages(spaceKey), PageSummaryImpl.class,
                org.codehaus.swizzle.confluence.PageSummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPage(java.lang.String)
     */
    public Page getPage(String pageId) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new PageImpl(swizzle.getPage(pageId));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPage(java.lang.String, java.lang.String)
     */
    public Page getPage(String spaceKey, String pageTitle) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new PageImpl(swizzle.getPage(spaceKey, pageTitle));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPageHistory(java.lang.String)
     */
    public List getPageHistory(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getPageHistory(pageId), PageHistorySummaryImpl.class,
                org.codehaus.swizzle.confluence.PageHistorySummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getAttachments(java.lang.String)
     */
    public List getAttachments(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getAttachments(pageId), AttachmentImpl.class,
                org.codehaus.swizzle.confluence.Attachment.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getAncestors(java.lang.String)
     */
    public List getAncestors(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getAncestors(pageId), PageSummaryImpl.class,
                org.codehaus.swizzle.confluence.PageSummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getChildren(java.lang.String)
     */
    public List getChildren(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getChildren(pageId), PageSummaryImpl.class,
                org.codehaus.swizzle.confluence.PageSummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getDescendents(java.lang.String)
     */
    public List getDescendents(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getDescendents(pageId), PageSummaryImpl.class,
                org.codehaus.swizzle.confluence.PageSummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getComments(java.lang.String)
     */
    public List getComments(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getComments(pageId), CommentImpl.class,
                org.codehaus.swizzle.confluence.Comment.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getComment(java.lang.String)
     */
    public Comment getComment(String commentId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new CommentImpl(swizzle.getComment(commentId));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addComment(org.codehaus.swizzle.confluence.Comment)
     */
    public Comment addComment(Comment comment) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new CommentImpl(swizzle.addComment(((CommentImpl) comment).getTarget()));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeComment(java.lang.String)
     */
    public boolean removeComment(String commentId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeComment(commentId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#storePage(org.codehaus.swizzle.confluence.Page)
     */
    public Page storePage(Page page) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new PageImpl(swizzle.storePage(((PageImpl) page).getTarget()));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#renderContent(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public String renderContent(String spaceKey, String pageId, String content)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.renderContent(spaceKey, pageId, content);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#renderContent(java.lang.String,
     *      java.lang.String)
     */
    public String renderContent(String spaceKey, String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.renderContent(spaceKey, pageId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#renderContent(org.codehaus.swizzle.confluence.PageSummary)
     */
    public String renderContent(PageSummary page) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.renderContent(((PageSummaryImpl) page).getTarget());
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#renderContent(java.lang.String,
     *      java.lang.String, java.lang.String, java.util.Map)
     */
    public String renderContent(String spaceKey, String pageId, String content, Map parameters)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.renderContent(spaceKey, pageId, content, parameters);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removePage(java.lang.String)
     */
    public void removePage(String pageId) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            swizzle.removePage(pageId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getAttachment(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public Attachment getAttachment(String pageId, String fileName, String versionNumber)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new AttachmentImpl(swizzle.getAttachment(pageId, fileName, versionNumber));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getAttachmentData(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public byte[] getAttachmentData(String pageId, String fileName, String versionNumber)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.getAttachmentData(pageId, fileName, versionNumber);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addAttachment(java.lang.String,
     *      org.codehaus.swizzle.confluence.Attachment, byte[])
     */
    public Attachment addAttachment(String pageId, Attachment attachment, byte[] attachmentData)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new AttachmentImpl(swizzle.addAttachment(pageId, ((AttachmentImpl) attachment)
                .getTarget(), attachmentData));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeAttachment(java.lang.String,
     *      java.lang.String)
     */
    public boolean removeAttachment(String pageId, String fileName) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeAttachment(pageId, fileName);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#moveAttachment(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean moveAttachment(String originalPageId, String originalName, String newPageId,
        String newName) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.moveAttachment(originalPageId, originalName, newPageId, newName);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getBlogEntries(java.lang.String)
     */
    public List getBlogEntries(String spaceKey) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getBlogEntries(spaceKey), BlogEntrySummaryImpl.class,
                org.codehaus.swizzle.confluence.BlogEntrySummary.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getBlogEntry(java.lang.String)
     */
    public BlogEntry getBlogEntry(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new BlogEntryImpl(swizzle.getBlogEntry(pageId));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#storeBlogEntry(org.codehaus.swizzle.confluence.BlogEntry)
     */
    public BlogEntry storeBlogEntry(BlogEntry entry) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new BlogEntryImpl(swizzle.storeBlogEntry(((BlogEntryImpl) entry).getTarget()));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getBlogEntryByDayAndTitle(java.lang.String, int,
     *      java.lang.String)
     */
    public BlogEntry getBlogEntryByDayAndTitle(String spaceKey, int dayOfMonth, String postTitle)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new BlogEntryImpl(swizzle.getBlogEntryByDayAndTitle(spaceKey, dayOfMonth,
                postTitle));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#search(java.lang.String, int)
     */
    public List search(String query, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.search(query, maxResults), SearchResultImpl.class,
                org.codehaus.swizzle.confluence.SearchResult.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#search(java.lang.String, java.util.Map, int)
     */
    public List search(String query, Map parameters, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.search(query, parameters, maxResults),
                SearchResultImpl.class, org.codehaus.swizzle.confluence.SearchResult.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPermissions(java.lang.String)
     */
    public List getPermissions(String spaceKey) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getPermissions(spaceKey), PermissionImpl.class,
                org.codehaus.swizzle.confluence.Permission.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPermissionsForUser(java.lang.String,
     *      java.lang.String)
     */
    public List getPermissionsForUser(String spaceKey, String userName)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getPermissionsForUser(spaceKey, userName),
                PermissionImpl.class, org.codehaus.swizzle.confluence.Permission.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getPagePermissions(java.lang.String)
     */
    public List getPagePermissions(String pageId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getPagePermissions(pageId), PermissionImpl.class,
                org.codehaus.swizzle.confluence.Permission.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getSpaceLevelPermissions()
     */
    public List getSpaceLevelPermissions() throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getSpaceLevelPermissions(), PermissionImpl.class,
                org.codehaus.swizzle.confluence.Permission.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addPermissionToSpace(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public boolean addPermissionToSpace(String permission, String remoteEntityName,
        String spaceKey) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.addPermissionToSpace(permission, remoteEntityName, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addPermissionsToSpace(java.util.List,
     *      java.lang.String, java.lang.String)
     */
    public boolean addPermissionsToSpace(List permissions, String remoteEntityName,
        String spaceKey) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.addPermissionsToSpace(permissions, remoteEntityName, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removePermissionFromSpace(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public boolean removePermissionFromSpace(String permission, String remoteEntityName,
        String spaceKey) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removePermissionFromSpace(permission, remoteEntityName, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addAnonymousPermissionToSpace(java.lang.String,
     *      java.lang.String)
     */
    public boolean addAnonymousPermissionToSpace(String permission, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.addAnonymousPermissionToSpace(permission, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addAnonymousPermissionsToSpace(java.util.List,
     *      java.lang.String)
     */
    public boolean addAnonymousPermissionsToSpace(List permissions, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            // TODO what is the type of the elements in permissions ? do they need wrapping?
            return swizzle.addAnonymousPermissionsToSpace(permissions, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeAnonymousPermissionFromSpace(java.lang.String,
     *      java.lang.String)
     */
    public boolean removeAnonymousPermissionFromSpace(String permission, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeAnonymousPermissionFromSpace(permission, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeAllPermissionsForGroup(java.lang.String)
     */
    public boolean removeAllPermissionsForGroup(String groupname) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeAllPermissionsForGroup(groupname);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getUser(java.lang.String)
     */
    public User getUser(String username) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return new UserImpl(swizzle.getUser(username));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addUser(org.codehaus.swizzle.confluence.User,
     *      java.lang.String)
     */
    public void addUser(User user, String password) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            swizzle.addUser(((UserImpl) user).getTarget(), password);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addGroup(java.lang.String)
     */
    public void addGroup(String group) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            swizzle.addGroup(group);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getUserGroups(java.lang.String)
     */
    public List getUserGroups(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.getUserGroups(username);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addUserToGroup(java.lang.String,
     *      java.lang.String)
     */
    public void addUserToGroup(String username, String groupname) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            swizzle.addUserToGroup(username, groupname);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeUserFromGroup(java.lang.String,
     *      java.lang.String)
     */
    public boolean removeUserFromGroup(String username, String groupname)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeUserFromGroup(username, groupname);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeUser(java.lang.String)
     */
    public boolean removeUser(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeUser(username);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeGroup(java.lang.String, java.lang.String)
     */
    public boolean removeGroup(String groupname, String defaultGroupName)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeGroup(groupname, defaultGroupName);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getGroups()
     */
    public List getGroups() throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.getGroups();
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#hasUser(java.lang.String)
     */
    public boolean hasUser(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.hasUser(username);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#hasGroup(java.lang.String)
     */
    public boolean hasGroup(String groupname) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.hasGroup(groupname);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#editUser(org.codehaus.swizzle.confluence.User)
     */
    public boolean editUser(User remoteUser) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.editUser(((UserImpl) remoteUser).getTarget());
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#deactivateUser(java.lang.String)
     */
    public boolean deactivateUser(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.deactivateUser(username);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#reactivateUser(java.lang.String)
     */
    public boolean reactivateUser(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.reactivateUser(username);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getActiveUsers(boolean)
     */
    public List getActiveUsers(boolean viewAll) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.getActiveUsers(viewAll);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#setUserInformation(org.codehaus.swizzle.confluence.UserInformation)
     */
    public boolean setUserInformation(UserInformation userInfo) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.setUserInformation(((UserInformationImpl) userInfo).getTarget());
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getUserInformation(java.lang.String)
     */
    public UserInformation getUserInformation(String username) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return new UserInformationImpl(swizzle.getUserInformation(username));
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#changeMyPassword(java.lang.String,
     *      java.lang.String)
     */
    public boolean changeMyPassword(String oldPass, String newPass) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.changeMyPassword(oldPass, newPass);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#changeUserPassword(java.lang.String,
     *      java.lang.String)
     */
    public boolean changeUserPassword(String username, String newPass)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.changeUserPassword(username, newPass);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getLabelsById(long)
     */
    public List getLabelsById(long objectId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getLabelsById(objectId), LabelImpl.class,
                org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getMostPopularLabels(int)
     */
    public List getMostPopularLabels(int maxCount) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getMostPopularLabels(maxCount), LabelImpl.class,
                org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getMostPopularLabelsInSpace(java.lang.String,
     *      int)
     */
    public List getMostPopularLabelsInSpace(String spaceKey, int maxCount)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getMostPopularLabelsInSpace(spaceKey, maxCount),
                LabelImpl.class, org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getRecentlyUsedLabels(int)
     */
    public List getRecentlyUsedLabels(int maxResults) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getRecentlyUsedLabels(maxResults), LabelImpl.class,
                org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getRecentlyUsedLabelsInSpace(java.lang.String,
     *      int)
     */
    public List getRecentlyUsedLabelsInSpace(String spaceKey, int maxResults)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getRecentlyUsedLabelsInSpace(spaceKey, maxResults),
                LabelImpl.class, org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getSpacesWithLabel(java.lang.String)
     */
    public List getSpacesWithLabel(String labelName) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getSpacesWithLabel(labelName), SpaceImpl.class,
                org.codehaus.swizzle.confluence.Space.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getRelatedLabels(java.lang.String, int)
     */
    public List getRelatedLabels(String labelName, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getRelatedLabels(labelName, maxResults), LabelImpl.class,
                org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getRelatedLabelsInSpace(java.lang.String,
     *      java.lang.String, int)
     */
    public List getRelatedLabelsInSpace(String labelName, String spaceKey, int maxResults)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getRelatedLabelsInSpace(labelName, spaceKey, maxResults),
                LabelImpl.class, org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getLabelsByDetail(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     */
    public List getLabelsByDetail(String labelName, String namespace, String spaceKey,
        String owner) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getLabelsByDetail(labelName, namespace, spaceKey, owner),
                LabelImpl.class, org.codehaus.swizzle.confluence.Label.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getLabelContentById(long)
     */
    public List getLabelContentById(long labelId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            // TODO what is the type of the elements returned here ? most likely they need wrapping
            return swizzle.getLabelContentById(labelId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getLabelContentByName(java.lang.String)
     */
    public List getLabelContentByName(String labelName) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            // TODO what is the type of the elements returned here ? most likely they need wrapping
            return swizzle.getLabelContentByName(labelName);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getLabelContentByObject(org.codehaus.swizzle.confluence.Label)
     */
    public List getLabelContentByObject(Label labelObject) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            // TODO what is the type of the elements returned here ? most likely they need wrapping
            return swizzle.getLabelContentByObject(((LabelImpl) labelObject).getTarget());
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getSpacesContainingContentWithLabel(java.lang.String)
     */
    public List getSpacesContainingContentWithLabel(String labelName)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return wrapList(swizzle.getSpacesContainingContentWithLabel(labelName),
                SpaceImpl.class, org.codehaus.swizzle.confluence.Space.class);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addLabelByName(java.lang.String, long)
     */
    public boolean addLabelByName(String labelName, long objectId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.addLabelByName(labelName, objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addLabelById(long, long)
     */
    public boolean addLabelById(long labelId, long objectId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.addLabelById(labelId, objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addLabelByObject(org.codehaus.swizzle.confluence.Label,
     *      long)
     */
    public boolean addLabelByObject(Label labelObject, long objectId)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.addLabelByObject(((LabelImpl) labelObject).getTarget(), objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#addLabelByNameToSpace(java.lang.String,
     *      java.lang.String)
     */
    public boolean addLabelByNameToSpace(String labelName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.addLabelByNameToSpace(labelName, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeLabelByName(java.lang.String, long)
     */
    public boolean removeLabelByName(String labelName, long objectId)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeLabelByName(labelName, objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeLabelById(long, long)
     */
    public boolean removeLabelById(long labelId, long objectId) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.removeLabelById(labelId, objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeLabelByObject(org.codehaus.swizzle.confluence.Label,
     *      long)
     */
    public boolean removeLabelByObject(Label labelObject, long objectId)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeLabelByObject(((LabelImpl) labelObject).getTarget(), objectId);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#removeLabelByNameFromSpace(java.lang.String,
     *      java.lang.String)
     */
    public boolean removeLabelByNameFromSpace(String labelName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.removeLabelByNameFromSpace(labelName, spaceKey);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command) throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.call(command);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command, Object arg1) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.call(command, arg1);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command, Object arg1, Object arg2) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.call(command, arg1, arg2);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command, Object arg1, Object arg2, Object arg3)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.call(command, arg1, arg2, arg3);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command, Object arg1, Object arg2, Object arg3, Object arg4)
        throws XWikiClientException, XWikiClientRemoteException
    {
        try {
            return swizzle.call(command, arg1, arg2, arg3, arg4);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private Object call(String command, Object[] args) throws XWikiClientException,
        XWikiClientRemoteException
    {
        try {
            return swizzle.call(command, args);
        } catch (ConfluenceException e) {
            throw new XWikiClientRemoteException(e);
        } catch (SwizzleConfluenceException e) {
            throw new XWikiClientException(e);
        }
    }

    private List wrapList(List list, Class wrapperClass, Class targetClass)
        throws XWikiClientException
    {
        List result = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object target = list.get(i);
            try {
                result.add(wrapperClass.getConstructor(new Class[] {targetClass}).newInstance(
                    new Object[] {target}));
            } catch (Exception e) {
                throw new XWikiClientException(e);
            }
        }
        return result;
    }

    // XWiki-only methods

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#getAttachmentVersions(java.lang.String,
     *      java.lang.String)
     */
    public List getAttachmentVersions(String pageId, String fileName)
        throws XWikiClientRemoteException, XWikiClientException
    {
        Object[] vector = (Object[]) call("getAttachmentVersions", pageId, fileName);
        return Arrays.asList(vector);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.client.XWikiClient#setNoConversion()
     */
    public void setNoConversion() throws XWikiClientRemoteException, XWikiClientException
    {
        call("setNoConversion");
        swizzle.setConvertor(new MapConvertor(new IdentityObjectConvertor()));
    }
}
