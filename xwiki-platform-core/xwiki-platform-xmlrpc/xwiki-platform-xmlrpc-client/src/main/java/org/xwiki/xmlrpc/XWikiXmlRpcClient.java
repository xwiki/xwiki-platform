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
package org.xwiki.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Comment;
import org.codehaus.swizzle.confluence.Page;
import org.codehaus.swizzle.confluence.PageSummary;
import org.codehaus.swizzle.confluence.SearchResult;
import org.codehaus.swizzle.confluence.ServerInfo;
import org.codehaus.swizzle.confluence.Space;
import org.codehaus.swizzle.confluence.SpaceSummary;
import org.xwiki.xmlrpc.model.XWikiClass;
import org.xwiki.xmlrpc.model.XWikiClassSummary;
import org.xwiki.xmlrpc.model.XWikiExtendedId;
import org.xwiki.xmlrpc.model.XWikiObject;
import org.xwiki.xmlrpc.model.XWikiObjectSummary;
import org.xwiki.xmlrpc.model.XWikiPage;
import org.xwiki.xmlrpc.model.XWikiPageHistorySummary;
import org.xwiki.xmlrpc.model.XWikiPageSummary;

/**
 * This class implements the entry point for using the XWiki XMLRPC API. Methods compatible with Confluence are tagged
 * with the category 'ConfluenceAPI'
 */
public class XWikiXmlRpcClient
{
    private XmlRpcClient xmlRpcClient;

    private String token;

    private String rpcHandler;

    /**
     * Constructor.
     * 
     * @param endpoint The endpoint for the XMLRPC servlet.
     * @throws MalformedURLException
     */
    public XWikiXmlRpcClient(String endpoint) throws MalformedURLException
    {
        this(endpoint, "confluence1");
    }

    /**
     * Constructor.
     * 
     * @param endpoint The endpoint for the XMLRPC servlet.
     * @param rpcHandler The id of the XMLRPC handler.
     * @throws MalformedURLException
     */
    public XWikiXmlRpcClient(String endpoint, String rpcHandler) throws MalformedURLException
    {
        XmlRpcClientConfigImpl clientConfig = new XmlRpcClientConfigImpl();
        clientConfig.setServerURL(new URL(endpoint));

        this.xmlRpcClient = new XmlRpcClient();
        this.xmlRpcClient.setConfig(clientConfig);

        this.rpcHandler = rpcHandler;

        this.token = "";
    }

    /**
     * Login with the provided user. If the method is not called anonymous user is used.
     * 
     * @param username the user id
     * @param password the user password
     * @throws XmlRpcException If an invalid username/password was specified or communication problem.
     */
    public void login(String username, String password) throws XmlRpcException
    {
        this.token = (String) invokeRpc("login", username, password);
    }

    /**
     * Logout.
     * 
     * @return True if logout was succesfull.
     * @throws XmlRpcException
     */
    public boolean logout() throws XmlRpcException
    {
        Boolean value = (Boolean) invokeRpc("logout", this.token);
        this.token = "";

        return value.booleanValue();
    }

    /**
     * Get server info.
     * 
     * @return Server information.
     * @throws XmlRpcException
     */
    public ServerInfo getServerInfo() throws XmlRpcException
    {
        Object object = invokeRpc("getServerInfo", this.token);
        return new ServerInfo((Map) object);
    }

    /**
     * Get spaces.
     * 
     * @return A list of SpaceSummaries for each space defined in the remote XWiki.
     * @throws XmlRpcException
     */
    public List<SpaceSummary> getSpaces() throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getSpaces", this.token);
        List<SpaceSummary> result = new ArrayList<SpaceSummary>();
        for (Object object : objects) {
            Map<String, Object> spaceSummaryMap = (Map) object;
            SpaceSummary spaceSummary = new SpaceSummary(spaceSummaryMap);
            result.add(spaceSummary);
        }

        return result;
    }

    /**
     * Get information about a given space.
     * 
     * @param spaceName
     * @return A Space object containing extended space information.
     * @throws XmlRpcException
     */
    public Space getSpace(String spaceName) throws XmlRpcException
    {
        Object object = invokeRpc("getSpace", this.token, spaceName);
        return new Space((Map) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getSpace(String)
     */
    public Space getSpace(SpaceSummary spaceSummary) throws XmlRpcException
    {
        return getSpace(spaceSummary.getKey());
    }

    /**
     * Add a new space.
     * 
     * @param space The Space object containing information about the new space.
     * @return A Space object with up-to-date information about the newly created space.
     * @throws XmlRpcException
     */
    public Space addSpace(Space space) throws XmlRpcException
    {
        Object object = invokeRpc("addSpace", this.token, space.toMap());
        return new Space((Map) object);
    }

    /**
     * Remove space and all of its pages.
     * 
     * @param spaceKey
     * @return True if the space was successfully deleted.
     * @throws XmlRpcException
     */
    public Boolean removeSpace(String spaceKey) throws XmlRpcException
    {
        return (Boolean) invokeRpc("removeSpace", this.token, spaceKey);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#removeSpace(String)
     */
    public Boolean removeSpace(SpaceSummary spaceSummary) throws XmlRpcException
    {
        return removeSpace(spaceSummary.getKey());
    }

    /**
     * Get pages in space.
     * 
     * @param spaceName
     * @return A list of SpaceSummaries for each page in the given space.
     * @throws XmlRpcException
     */
    public List<XWikiPageSummary> getPages(String spaceName) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getPages", this.token, spaceName);
        List<XWikiPageSummary> result = new ArrayList<XWikiPageSummary>();
        for (Object object : objects) {
            Map pageSummaryMap = (Map) object;
            XWikiPageSummary pageSummary = new XWikiPageSummary(pageSummaryMap);
            result.add(pageSummary);
        }

        return result;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPages(String)
     */
    public List<XWikiPageSummary> getPages(SpaceSummary space) throws XmlRpcException
    {
        return getPages(space.getKey());
    }

    /**
     * Get full page information and content.
     * 
     * @param pageId
     * @return A Page object containing all the information.
     * @throws XmlRpcException
     */
    public XWikiPage getPage(String pageId) throws XmlRpcException
    {
        Object object = invokeRpc("getPage", this.token, pageId);
        return new XWikiPage((Map) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPage(String)
     */
    public Page getPage(XWikiPageSummary pageSummary) throws XmlRpcException
    {
        return getPage(pageSummary.getId());
    }

    /**
     * Get full page information and content in a given language.
     * 
     * @param pageId
     * @param language
     * @return A Page object containing all the information.
     * @throws XmlRpcException
     */
    public XWikiPage getPage(String pageId, String language) throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("language", language);

        Object object = invokeRpc("getPage", this.token, extendedId.toString()); // ,
        // language);
        return new XWikiPage((Map) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPage(String, String)
     */
    public Page getPage(XWikiPageSummary pageSummary, String language) throws XmlRpcException
    {
        return getPage(pageSummary.getId(), language);
    }

    /**
     * Get full page information and content at a given version.
     * 
     * @param pageId
     * @param version
     * @return A Page object containing all the information.
     * @throws XmlRpcException
     */
    public XWikiPage getPage(String pageId, Integer version) throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());

        Object object = invokeRpc("getPage", this.token, extendedId.toString()); // pageId,
        // version);
        return new XWikiPage((Map) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPage(String, Integer)
     */
    public Page getPage(XWikiPageSummary pageSummary, Integer version) throws XmlRpcException
    {
        return getPage(pageSummary.getId(), version);
    }

    /**
     * Get full page information and content at a given version.
     * 
     * @param pageId
     * @param version
     * @return A Page object containing all the information.
     * @throws XmlRpcException
     */
    public XWikiPage getPage(String pageId, Integer version, Integer minorVersion) throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());
        extendedId.setParameter("minorVersion", minorVersion.toString());
        Object object = invokeRpc("getPage", this.token, extendedId.toString());// pageId,
        // version,
        // minorVersion);
        return new XWikiPage((Map) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPage(String, Integer, Integer)
     */
    public XWikiPage getPage(XWikiPageHistorySummary pageHistorySummary) throws XmlRpcException
    {
        return getPage(pageHistorySummary.getId(), pageHistorySummary.getVersion(), pageHistorySummary
            .getMinorVersion());
    }

    /**
     * Get the history of a page.
     * 
     * @param pageId
     * @return A list of PageHistorySummary for each revision of the page.
     * @throws XmlRpcException
     */
    public List<XWikiPageHistorySummary> getPageHistory(String pageId) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getPageHistory", this.token, pageId);
        List<XWikiPageHistorySummary> result = new ArrayList<XWikiPageHistorySummary>();
        for (Object object : objects) {
            Map<String, Object> pageHistorySummaryMap = (Map) object;
            XWikiPageHistorySummary pageHistorySummary = new XWikiPageHistorySummary(pageHistorySummaryMap);
            result.add(pageHistorySummary);
        }

        return result;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getPageHistory(String)
     */
    public List<XWikiPageHistorySummary> getPageHistory(XWikiPageSummary pageSummary) throws XmlRpcException
    {
        return getPageHistory(pageSummary.getId());
    }

    /**
     * Store a page.
     * 
     * @param page
     * @return The Page object with the up-to-date information about the newly stored page.
     * @throws XmlRpcException
     */
    public XWikiPage storePage(Page page) throws XmlRpcException
    {
        Object object = invokeRpc("storePage", this.token, page.toMap());
        return new XWikiPage((Map) object);
    }

    /**
     * Remove a page.
     * 
     * @param pageId
     * @return True if the page has been successfully removed.
     * @throws XmlRpcException
     */
    public Boolean removePage(String pageId) throws XmlRpcException
    {
        Object object = invokeRpc("removePage", this.token, pageId);
        return (Boolean) object;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#removePage(String)
     */
    public Boolean removePage(XWikiPageSummary page) throws XmlRpcException
    {
        return removePage(page.getId());
    }

    /**
     * Render a page or content in HTML.
     * 
     * @param space Ignored
     * @param pageId The page id in the form of Space.Page
     * @param content The content to be rendered. If content == "" then the page content is rendered.
     * @return The rendered content.
     * @throws XmlRpcException XmlRpcException If the page does not exist or the user has not the right to access it.
     */
    public String renderContent(String space, String pageId, String content) throws XmlRpcException
    {
        return (String) invokeRpc("renderContent", this.token, space, pageId, content);
    }

    /**
     * Get page comments.
     * 
     * @param pageId
     * @return A list of Comment objects for each comment associated to the page.
     * @throws XmlRpcException
     */
    public List<Comment> getComments(String pageId) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getComments", this.token, pageId);
        List<Comment> result = new ArrayList<Comment>();
        for (Object object : objects) {
            Map<String, Object> commentMap = (Map) object;
            Comment comment = new Comment(commentMap);
            result.add(comment);
        }

        return result;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getComments(String)
     */
    public List<Comment> getComments(PageSummary pageSummary) throws XmlRpcException
    {
        return getComments(pageSummary.getId());
    }

    public Comment getComment(String commentId) throws XmlRpcException
    {
        Object object = invokeRpc("getComment", this.token, commentId);
        return new Comment((Map) object);
    }

    /**
     * Add a new comment.
     * 
     * @param comment A Comment object containing the comment to be added.
     * @return A Comment object containing the information about the newly created comment.
     * @throws XmlRpcException
     */
    public Comment addComment(Comment comment) throws XmlRpcException
    {
        Object object = invokeRpc("addComment", this.token, comment.toMap());
        return new Comment((Map) object);
    }

    /**
     * Remove a comment.
     * 
     * @param commentId
     * @return True if the comment has been successfully removed.
     * @throws XmlRpcException
     */
    public Boolean removeComment(String commentId) throws XmlRpcException
    {
        Object object = invokeRpc("removeComment", this.token, commentId);
        return (Boolean) object;
    }

    /**
     * Get attachments.
     * 
     * @param pageId
     * @return A list of Attachment objects describing the attachments associated to the page.
     * @throws XmlRpcException
     */
    public List<Attachment> getAttachments(String pageId) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getAttachments", this.token, pageId);
        List<Attachment> result = new ArrayList<Attachment>();
        for (Object object : objects) {
            Map<String, Object> attachmentMap = (Map) object;
            Attachment attachment = new Attachment(attachmentMap);
            result.add(attachment);
        }

        return result;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getAttachments(String)
     */
    public List<Attachment> getAttachments(PageSummary pageSummary) throws XmlRpcException
    {
        return getAttachments(pageSummary.getId());
    }

    /**
     * Get the binary data associated to the attachment.
     * 
     * @param pageId
     * @param fileName
     * @param versionNumber Ignored.
     * @return The actual attachment data.
     * @throws XmlRpcException
     */
    public byte[] getAttachmentData(String pageId, String fileName, String versionNumber) throws XmlRpcException
    {
        return (byte[]) invokeRpc("getAttachmentData", this.token, pageId, fileName, versionNumber);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getAttachmentData(String, String, String)
     */
    public byte[] getAttachmentData(Attachment attachment) throws XmlRpcException
    {
        return getAttachmentData(attachment.getPageId(), attachment.getFileName(), "");
    }

    /**
     * Add a new attachment.
     * 
     * @param contentId Ignored. (It is here because the Confluence API signature declares it)
     * @param attachment The Attachment object describing the attachment.
     * @param attachmentData The actual attachment data.
     * @return An Attachment object describing the newly added attachment.
     * @throws XmlRpcException
     */
    public Attachment addAttachment(Integer contentId, Attachment attachment, byte[] attachmentData)
        throws XmlRpcException
    {
        Object object = invokeRpc("addAttachment", this.token, contentId, attachment.toMap(), attachmentData);
        return new Attachment((Map<String, Object>) object);
    }

    /**
     * Remove attachment.
     * 
     * @param pageId
     * @param fileName
     * @return True if the attachment has been successfully removed.
     * @throws XmlRpcException
     */
    public Boolean removeAttachment(String pageId, String fileName) throws XmlRpcException
    {
        return (Boolean) invokeRpc("removeAttachment", this.token, pageId, fileName);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#removeAttachment(String, String)
     */
    public Boolean removeAttachment(Attachment attachment) throws XmlRpcException
    {
        return removeAttachment(attachment.getPageId(), attachment.getFileName());
    }

    /**
     * Get classes.
     * 
     * @return A list of XWikiClassSummaries for each class defined in the XWiki instance.
     * @throws XmlRpcException
     */
    public List<XWikiClassSummary> getClasses() throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getClasses", this.token);
        List<XWikiClassSummary> result = new ArrayList<XWikiClassSummary>();
        for (Object object : objects) {
            Map<String, Object> xwikiClassSummaryMap = (Map<String, Object>) object;
            XWikiClassSummary xwikiClassSummary = new XWikiClassSummary(xwikiClassSummaryMap);
            result.add(xwikiClassSummary);
        }

        return result;
    }

    /**
     * Get extended information about a class.
     * 
     * @param className
     * @return A XWikiClass object with the extended information.
     * @throws XmlRpcException
     */
    public XWikiClass getClass(String className) throws XmlRpcException
    {
        Object object = invokeRpc("getClass", this.token, className);
        return new XWikiClass((Map<String, Object>) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getClass(String)
     */
    public XWikiClass getClass(XWikiClassSummary classSummary) throws XmlRpcException
    {
        return getClass(classSummary.getId());
    }

    /**
     * Get XWiki objects associated with a page.
     * 
     * @param pageId
     * @return A list containing an XWikiObject summary for every object found in the page.
     * @throws XmlRpcException
     */
    public List<XWikiObjectSummary> getObjects(String pageId) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("getObjects", this.token, pageId);
        List<XWikiObjectSummary> result = new ArrayList<XWikiObjectSummary>();
        for (Object object : objects) {
            Map<String, Object> xwikiObjectSummaryMap = (Map<String, Object>) object;
            XWikiObjectSummary xwikiObjectSummary = new XWikiObjectSummary(xwikiObjectSummaryMap);
            result.add(xwikiObjectSummary);
        }

        return result;
    }

    /**
     * Get XWiki objects associated with a page at a given version.
     * 
     * @param pageId
     * @param version
     * @return A list containing an XWikiObject summary for every object found in the page.
     * @throws XmlRpcException
     */
    public List<XWikiObjectSummary> getObjects(String pageId, Integer version) throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());

        Object[] objects = (Object[]) invokeRpc("getObjects", this.token, extendedId.toString());
        List<XWikiObjectSummary> result = new ArrayList<XWikiObjectSummary>();
        for (Object object : objects) {
            Map<String, Object> xwikiObjectSummaryMap = (Map<String, Object>) object;
            XWikiObjectSummary xwikiObjectSummary = new XWikiObjectSummary(xwikiObjectSummaryMap);
            result.add(xwikiObjectSummary);
        }

        return result;
    }

    /**
     * Get XWiki objects associated with a page at a given version.
     * 
     * @param pageId
     * @param version
     * @param minorVersion
     * @return A list containing an XWikiObject summary for every object found in the page.
     * @throws XmlRpcException
     */
    public List<XWikiObjectSummary> getObjects(String pageId, Integer version, Integer minorVersion)
        throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());
        extendedId.setParameter("minorVersion", minorVersion.toString());

        Object[] objects = (Object[]) invokeRpc("getObjects", this.token, extendedId.toString());
        List<XWikiObjectSummary> result = new ArrayList<XWikiObjectSummary>();
        for (Object object : objects) {
            Map<String, Object> xwikiObjectSummaryMap = (Map<String, Object>) object;
            XWikiObjectSummary xwikiObjectSummary = new XWikiObjectSummary(xwikiObjectSummaryMap);
            result.add(xwikiObjectSummary);
        }

        return result;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getObjects(PageSummary)
     */
    public List<XWikiObjectSummary> getObjects(PageSummary pageSummary) throws XmlRpcException
    {
        return getObjects(pageSummary.getId());
    }

    /**
     * Get XWiki object.
     * 
     * @param pageId
     * @param className
     * @param id The XWiki object id.
     * @return An XWiki object with all the association property -> values.
     * @throws XmlRpcException
     */
    public XWikiObject getObject(String pageId, String className, Integer id) throws XmlRpcException
    {
        Object object = invokeRpc("getObject", this.token, pageId, className, id);
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Get an XWiki object identified by its GUID.
     * 
     * @param pageId the document holding the object.
     * @param guid the object's GUID.
     * @return An XWiki object with all the association property -> values.
     * @throws XmlRpcException
     */
    public XWikiObject getObject(String pageId, String guid) throws XmlRpcException
    {
        Object object = invokeRpc("getObject", this.token, pageId, guid);
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Get the XWiki object associated with a page at a given version.
     * 
     * @param pageId
     * @param className
     * @param id
     * @param version
     * @return An XWiki object with all the association property -> values.
     * @throws XmlRpcException
     */
    public XWikiObject getObject(String pageId, String className, Integer id, Integer version) throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());

        Object object = invokeRpc("getObject", this.token, extendedId.toString(), className, id);
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Get XWiki objects associated with a page at a given version.
     * 
     * @param pageId
     * @param className
     * @param id
     * @param version
     * @param minorVersion
     * @return An XWiki object with all the association property -> values.
     * @throws XmlRpcException
     */
    public XWikiObject getObject(String pageId, String className, Integer id, Integer version, Integer minorVersion)
        throws XmlRpcException
    {
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        extendedId.setParameter("version", version.toString());
        extendedId.setParameter("minorVersion", minorVersion.toString());

        Object object = invokeRpc("getObject", this.token, extendedId.toString(), className, id);
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#getObject(String, String, Integer)
     */
    public XWikiObject getObject(XWikiObjectSummary xwikiObjectSummary) throws XmlRpcException
    {
        return getObject(xwikiObjectSummary.getPageId(), xwikiObjectSummary.getClassName(), xwikiObjectSummary.getId());
    }

    /**
     * Add an XWiki object.
     * 
     * @param xwikiObject
     * @return An XWikiObject with the up-to-date information about the stored object.
     * @throws XmlRpcException
     */
    public XWikiObject storeObject(XWikiObject xwikiObject) throws XmlRpcException
    {
        Object object = invokeRpc("storeObject", this.token, xwikiObject.toRawMap());
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Remove an XWiki Object.
     * 
     * @param pageId
     * @param className
     * @param id
     * @return True if the XWiki objec has been successfully removed.
     * @throws XmlRpcException
     */
    public Boolean removeObject(String pageId, String className, Integer id) throws XmlRpcException
    {
        Object object = invokeRpc("removeObject", this.token, pageId, className, id);
        return (Boolean) object;
    }

    /**
     * Convenience method.
     * 
     * @see XWikiXmlRpcClient#removeObject(String, String, Integer)
     */
    public Boolean removeObject(XWikiObjectSummary objectSummary) throws XmlRpcException
    {
        return removeObject(objectSummary.getPageId(), objectSummary.getClassName(), objectSummary.getId());
    }

    /**
     * @param query The string to be looked for.
     * @param maxResults (0 for all)
     * @return
     * @throws XmlRpcException
     */
    public List<SearchResult> search(String query, Integer maxResults) throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("search", this.token, query, maxResults);
        List<SearchResult> result = new ArrayList<SearchResult>();
        for (Object object : objects) {
            Map<String, Object> searchResultMap = (Map<String, Object>) object;
            SearchResult searchResult = new SearchResult(searchResultMap);
            result.add(searchResult);
        }

        return result;
    }

    /**
     * @return All the page ids available in the wiki.
     * @throws XmlRpcException
     */
    public List<SearchResult> searchAllPagesIds() throws XmlRpcException
    {
        Object[] objects = (Object[]) invokeRpc("search", this.token, "__ALL_PAGES__", 0);
        List<SearchResult> result = new ArrayList<SearchResult>();
        for (Object object : objects) {
            Map<String, Object> searchResultMap = (Map<String, Object>) object;
            SearchResult searchResult = new SearchResult(searchResultMap);
            result.add(searchResult);
        }

        return result;
    }

    /**
     * Returns a list of XWikiPageHistorySummary containing all the pages that have been modified since a given date in
     * all their versions.
     * 
     * @param date The starting date
     * @param numberOfResults The number of results to be returned
     * @param start The start offset in the result set
     * @param fromLatest True if the result set will list recent changed pages before.
     * @return A list of XWikiPageHistorySummary
     * @throws XmlRpcException
     * @throws QueryException
     */
    public List<XWikiPageHistorySummary> getModifiedPagesHistory(Date date, Integer numberOfResults, Integer start,
        Boolean fromLatest) throws XmlRpcException
    {
        Object[] objects =
            (Object[]) invokeRpc("getModifiedPagesHistory", this.token, date, numberOfResults, start, fromLatest);
        List<XWikiPageHistorySummary> result = new ArrayList<XWikiPageHistorySummary>();
        for (Object object : objects) {
            Map<String, Object> pageHistorySummaryMap = (Map) object;
            XWikiPageHistorySummary pageHistorySummary = new XWikiPageHistorySummary(pageHistorySummaryMap);
            result.add(pageHistorySummary);
        }

        return result;
    }

    /**
     * Equivalent to getModifiedPagesHistory(new Date(0), numberOfResults, start, fromLatest). Takes into account all
     * the modifications.
     */
    public List<XWikiPageHistorySummary> getModifiedPagesHistory(Integer numberOfResults, Integer start,
        Boolean fromLatest) throws XmlRpcException
    {
        return getModifiedPagesHistory(new Date(0), numberOfResults, start, fromLatest);
    }

    /**
     * Equivalent to getModifiedPagesHistory(new Date(0), numberOfResults, start, true). Takes into account all the
     * modifications in descending order (i.e., latest first).
     */
    public List<XWikiPageHistorySummary> getModifiedPagesHistory(Integer numberOfResults, Integer start)
        throws XmlRpcException
    {
        return getModifiedPagesHistory(new Date(0), numberOfResults, start, true);
    }

    /**
     * Store a page and check if the current page's version matches with the one of the page to be stored.
     * 
     * @param page The page to be stored.
     * @param checkVersion True if the version has to be checked.
     * @return The updated XWikiPage or an XWikiPage with all its fields empty in case of version mismatch.
     * @throws XmlRpcException
     */
    public XWikiPage storePage(Page page, Boolean checkVersion) throws XmlRpcException
    {
        Object object = invokeRpc("storePage", this.token, page.toMap(), checkVersion);
        return new XWikiPage((Map) object);
    }

    /**
     * Store an object and check if the current object's page version matches with the one of the object to be stored.
     * 
     * @param xwikiObject The object to be stored.
     * @param checkVersion True if the version has to be checked.
     * @return The updated XWikiObject or an XWikiObject with all its fields empty in case of version mismatch.
     * @throws XmlRpcException
     */
    public XWikiObject storeObject(XWikiObject xwikiObject, Boolean checkVersion) throws XmlRpcException
    {
        Object object = invokeRpc("storeObject", this.token, xwikiObject.toRawMap(), checkVersion);
        return new XWikiObject((Map<String, Object>) object);
    }

    /**
     * Converts a wiki source from a syntax to another syntax.
     *
     * @param source The content to be converted.
     * @param initialSyntaxId The initial syntax of the source.
     * @param targetSyntaxId The final syntax of the returned content.
     * @return The converted source.
     * @throws XmlRpcException An invalid token is provided, the syntaxId is not supported, the source is invalid or the
     * conversion fails.
     */
    public String convert( String source, String initialSyntaxId, String targetSyntaxId)
        throws XmlRpcException
    {
        return (String) invokeRpc("convert", this.token, source, initialSyntaxId, targetSyntaxId);
    }

    /**
     * Gets all syntaxes supported by the rendering parsers as an input for a syntax conversion.
     *
     * @return A list containing all syntaxes supported by rendering parsers.
     * @throws Exception An invalid token is provided or the syntax lookup fails.
     */
    public List<String> getInputSyntaxes() throws XmlRpcException
    {
        List<String> result = new ArrayList<String>();
        Object[] objects = (Object[]) invokeRpc("getInputSyntaxes", this.token);
        for(Object object:objects)
        {
            result.add((String)object);        
        }
        return result;           
    }

    /**
     * Gets all syntaxes supported by the rendering as an output for a syntax conversion.
     *
     * @return A list containing all syntaxes supported by renderers.
     * @throws XmlRpcException An invalid token is provided or the syntax lookup fails.
     */
    public List<String> getOutputSyntaxes() throws XmlRpcException
    {
        List<String> result = new ArrayList<String>();
        Object[] objects = (Object[]) invokeRpc("getOutputSyntaxes", this.token);
        for(Object object:objects)
        {
            result.add((String)object);
        }
        return result;
    }

    /**
     * Renders a text in the context of a wiki page.
     *
     * @param pageId The id of the page.
     * @param content The contenxt to be rendered.
     * @param sourceSyntaxId The syntax of the content.
     * @param targetSyntaxId The target syntax of the rendered content
     * @return The rendered content.
     * @throws XmlRpcException If a invalid token is provided, an unsuported syntax id is given or the rendering fails.
     */
    public String renderPageContent(String pageId, String content, String sourceSyntaxId,
        String targetSyntaxId) throws XmlRpcException
    {
        return (String) invokeRpc("renderPageContent", this.token, pageId, content, sourceSyntaxId, targetSyntaxId);
    }

    /**
     * Gets the rendered content of an existing document.
     *
     * @param pageId The id of the page.
     * @param syntaxId The target syntax of the rendered content
     * @return The renderded content
     * @throws Exception If a invalid token is provided, an unsuported syntax id is given or the rendering fails.
     */
    public String getRenderedContent(String pageId, String syntaxId) throws XmlRpcException
    {
        return (String) invokeRpc("getRenderedContent", this.token, pageId, syntaxId);
    }

    /**
     * Utility method for invoking remote RPC methods.
     * 
     * @param methodName
     * @param args
     * @return
     * @throws XmlRpcException
     */
    private synchronized Object invokeRpc(String methodName, Object... args) throws XmlRpcException
    {
        return this.xmlRpcClient.execute(String.format("%s.%s", this.rpcHandler, methodName), args);
    }
}
