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
 *
 */

package com.xpn.xwiki.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.codehaus.swizzle.confluence.ConfluenceObjectConvertor;
import org.codehaus.swizzle.confluence.IdentityObjectConvertor;
import org.codehaus.swizzle.confluence.MapConvertor;
import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.xmlrpc.model.Attachment;
import com.xpn.xwiki.xmlrpc.model.Comment;
import com.xpn.xwiki.xmlrpc.model.MapObject;
import com.xpn.xwiki.xmlrpc.model.Page;
import com.xpn.xwiki.xmlrpc.model.PageHistorySummary;
import com.xpn.xwiki.xmlrpc.model.PageSummary;
import com.xpn.xwiki.xmlrpc.model.SearchResult;
import com.xpn.xwiki.xmlrpc.model.Space;
import com.xpn.xwiki.xmlrpc.model.SpaceSummary;
import com.xpn.xwiki.xmlrpc.model.swizzle.AttachmentImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.CommentImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.PageImpl;
import com.xpn.xwiki.xmlrpc.model.swizzle.SpaceImpl;

/**
 * Implements the <a href="http://confluence.atlassian.com/display/DOC/Remote+API+Specification">
 * Confluence XML-RPC interface</a>. Provides all the operations that can be done remotely on a
 * XWiki instance using the XML-RPC protocol.
 * 
 * <p>
 * Note: Lots of the Javadoc comments below are borrowed from the <a
 * href="http://www.atlassian.com/software/jira/docs/api/rpc-jira-plugin/latest/index.html?com/atlassian/jira/rpc/xmlrpc/XmlRpcService.html">
 * Confluence Javadoc</a>.
 * </p>
 * 
 * <p>Note: Ids (eg. page ids) should be treated as opaque handlers. If you ever find yourself parsing them
 * then you are doing something wrong.</p>
 * 
 * @version $Id: $
 * 
 */
public class ConfluenceRpcHandler extends BaseRpcHandler
{
    // TODO Also run the tests for unprivileged users with just enough rights
    // -- user management could be a part of the test setup

    // Q: Where are access rights checked ? Ensure they are !

    // TODO Use the log for more than login, logout and errors

    // TODO Refactor - Get rid of duplicate code:
    // - inside the xml-rpc implementation itself (there is too much plumbing)
    // - between xml-rpc and actions (important but hard!)

    // TODO enabling exceptions doesn't really work in xml-rpc versions before 3.1 

    private static final Log log = LogFactory.getLog(ConfluenceRpcHandler.class);
    
    private DomainObjectFactory factory = new DomainObjectFactory();
    
    private MapConvertor convertor;

    private class RemoteUser
    {

        public RemoteUser(String username, String ip)
        {
            this.ip = ip;
            this.username = username;
        }

        public String ip;

        public String username;
    }

    public void init(Servlet servlet, ServletRequest request) throws XWikiException
    {
        super.init(servlet, request);
        
        // we are interoperable with Confluence by default
        setConvertor(new MapConvertor( new ConfluenceObjectConvertor()));
    }
    

    // Authentication Methods
    

    /**
     * Logs the user into XWiki. The security token which is returned is used in all subsequent
     * method calls. You can supply an empty string as the token to be treated as being the
     * anonymous user (XWiki.XWikiGuest).
     * 
     * @param username the username of the person logged in as
     * @param password the appropriate password
     * @return A string which is a security token to be used in all subsequent calls
     * @throws XWikiException in case of error
     */
    public String login(String username, String password) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        String token;

        if (xwiki.getAuthService().authenticate(username, password, context) != null) {
            // Generate "unique" token using a random number
            token = xwiki.generateValidationKey(128);
            String ip = context.getRequest().getRemoteAddr();
            getTokens(context).put(token, new RemoteUser("XWiki." + username, ip));
            log.info("Logged in '" + username + "'");
            return token;
        } else {
            throw exception("Failed authentication for user '" + username + "'.");
        }
    }

    /**
     * Logs the user out of XWiki.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return whether the logging out was successful or not
     * @throws XWikiException in case of error
     */
    public boolean logout(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        checkToken(token, context);

        return getTokens(context).remove(token) != null;
    }

    
    // General

    
    /**
     * Retrieve some basic information about the server being connected to. Useful for clients that
     * need to turn certain features on or off depending on the version of the server.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return the Server information such as Server base URL and Server version. The returned map
     *         contains the fields from {@linkServerInfo}.
     * @throws XWikiException in case of error
     */
    public Map getServerInfo(String token) throws XWikiException
    {
        // TODO this should return as if this was Confluence
        // the version should be the largest one we implement fully
        // ServerInfo info = new ServerInfo();
        // info.setBaseUrl(baseUrl);
        //        
        // info.setMajorVersion(1);
        // info.setMinorVersion(0);
        // info.setPatchLevel(0);
        //                
        // return info.getParameters();

        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    
    // Space Retrieval

    
    /**
     * Get the summaries of all spaces the user can view.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return all the SpaceSummaries that the current user can see.
     * @throws XWikiException in case of error
     */
    public Object[] getSpaces(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        List spaces = xwiki.getSpaces(context);
        ArrayList result = new ArrayList(spaces.size());
        for (int i = 0; i < spaces.size(); i++) {
            String key = (String) spaces.get(i);
            String fullName = key + ".WebHome";
            XWikiDocument doc = xwiki.getDocument(fullName, context);
            String name = doc.getTitle();
            String url = doc.getURL("view", context);
            SpaceSummary spacesum = factory.createSpaceSummary(key, name, url);
            result.add(convert(spacesum));
        }
        return result.toArray();
    }

    /**
     * Get one Space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey identifier for space
     * @return a single Space object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Map getSpace(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        String fullName = spaceKey + "." + "WebHome";
        if (xwiki.exists(fullName, context)) {
            XWikiDocument doc = xwiki.getDocument(fullName, context);
            String url = doc.getURL("view", context);
            String name = doc.getTitle();
            Space space = factory.createSpace(spaceKey, name, url, "", fullName);
            return convert(space);
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }

    }


    // Space Management


    // TODO JavaDoc: we SAY we require description, but we discard it!
    // we can also live without a space name
    /**
     * Create a new space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceProperties Map containing all informations, we need to create a new space. We
     *            need the following keys: - key "name": the name of the space - key "key": the
     *            space key - key "description": the space description
     * @return created Space as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Map addSpace(String token, Map spaceMap) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("save");
        checkToken(token, context);

        Space space = new SpaceImpl(spaceMap, convertor);
        String spaceKey = space.getKey();
        String fullName = spaceKey + "." + "WebHome";
        if (!xwiki.exists(fullName, context)) {
            // Create a new WebHome document and store it
            XWikiDocument doc = new XWikiDocument(spaceKey, "WebHome");
            doc.setAuthor(context.getUser());
            if (space.getName() != null) {
                doc.setTitle(space.getName());
            }
            xwiki.saveDocument(doc, context);
            String url = doc.getURL("view", context);
            String name = doc.getTitle();
            Space resultSpace = factory.createSpace(spaceKey, name, url, "", fullName);
            return convert(resultSpace);
        } else {
            throw exception("The space '" + spaceKey + "' already exists.");
        }
    }

    /**
     * Remove a space completely by removing all of it's child documents.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey the space to be deleted
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException in case of error
     */
    public boolean removeSpace(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("delete");
        checkToken(token, context);

        if (xwiki.exists(spaceKey + "." + "WebHome", context)) {
            // Delete each page individually
            List docNames = xwiki.getSpaceDocsName(spaceKey, context);
            for (int i = 0; i < docNames.size(); i++) {
                removePage(token, spaceKey + "." + docNames.get(i));
            }
            return true;
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }
    }


    // Page Retrieval


    /**
     * Get the summaries of all the pages in the space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey to look for pages in
     * @return a vector of PageSummaries as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] getPages(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        String fullName = spaceKey + "." + "WebHome";
        if (xwiki.exists(fullName, context)) {
            List docNames = xwiki.getSpaceDocsName(spaceKey, context);
            ArrayList pages = new ArrayList(docNames.size());
            for (int i = 0; i < docNames.size(); i++) {
                String docFullName = spaceKey + "." + docNames.get(i);
                XWikiDocument doc = xwiki.getDocument(docFullName, context);
                PageSummary pagesum = factory.createPageSummary(doc, context);
                pages.add(convert(pagesum));
            }
            return pages.toArray();
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }
    }

    /**
     * Get one Page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to look for
     * @return a Page object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Map getPage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();

        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        Page page = factory.createPage(doc, context);
        return convert(page);
    }
    
    /**
     * Get one Page.
     * @param token token the authentication token retrieved when calling the login method
     * @param spaceKey the parent space of the page 
     * @param pageTitle the title of the page
     * @return a Page object represented as a Map
     * @throws XWikiException 
     */
    public Map getPage(String token, String spaceKey, String pageTitle) throws XWikiException
    {
        return getPage(token, factory.getPageId(spaceKey, pageTitle));
    }

    /**
     * Returns all the previous versions of a page as a list of PageHistorySummaries.
     * We only consider the *old* versions of the document in the page history.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to look for
     * @return a vector of PageHistories as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] getPageHistory(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);

        // We only consider the old(!) versions of the document in the page history
        Version[] versions = doc.getRevisions(context);
        List result = new LinkedList();
        for (int i = 0; i < versions.length && !versions[i].toString().equals(doc.getVersion()); i++) {
            String version = versions[i].toString();
            XWikiDocument revdoc = xwiki.getDocument(doc, version, context);
            PageHistorySummary phs = factory.createPageHistorySummary(revdoc);
            result.add(0, convert(phs));
        }
        return result.toArray();
    }


    // Page Dependencies

    
    /**
     * Get all the Attachments for a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId id of page from where we want all Attachments
     * @return a vector of Attachment objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] getAttachments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        // TODO isn't this default ?
        // -- if yes, then don't set it explicitly. Or maybe still useful
        // -- if not then ensure it is set everywhere!
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);

        List attachlist = doc.getAttachmentList();
        ArrayList result = new ArrayList(attachlist.size());
        for (int i = 0; i < attachlist.size(); i++) {
            XWikiAttachment xAttach = (XWikiAttachment) attachlist.get(i);
            Attachment attach = factory.createAttachment(doc, xAttach, context);
            result.add(convert(attach));
        }
        return result.toArray();
    }

    // TODO test page history + comments
    // problem: confluence and xwiki behave differently and there seems to be no way to reconcile
    // Confluence: creating, modifying, removing objects (i.e. comments, tags/labels) and
    // attachments does not generate new versions. Reverting the page does not revert objects and
    // attachments. Comparing 2 versions of a page does not take objects and attachments into
    // account.
    // -> the test would be xwiki specific and won't work with confluence!

    // TODO generalize this to arbitrary objects (there the class name is no longer fixed)
    // - this would be an extension to the confluence api, still adding new methods is not(!) a
    // problem for interoperability
    /**
     * Get all the comments for a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to get comments from
     * @return a vector of Comment objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] getComments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);

        // TODO: here we can also use getComments
        // TODO: overkill for getting "XWikiComments" (which is hard-coded everywhere anyway)
        String className = xwiki.getCommentsClass(context).getName();
        List commentlist = doc.getObjects(className);
        if (commentlist != null) {
            ArrayList result = new ArrayList();
            for (int i = 0; i < commentlist.size(); i++) {
                BaseObject obj = (BaseObject) commentlist.get(i);
                if (obj != null) {
                    // Note: checking for null values here is crucial
                    // because comments are just set to null when deleted
                    Comment comment = factory.createComment(doc, obj, context);
                    result.add(convert(comment));
                }
            }
            return result.toArray();
        } else {
            return new Object[0];
        }
    }

    // TODO How is this useful ? Is there a way to get a comment id without getting the comment
    // itself ?
    // -- commentIds are long lived, so they may be useful to save for later use
    
    // TODO improve javadoc
    /**
     * Returns a comment given a comment id.
     * 
     * @param token
     * @param commentId
     * @return
     * @throws XWikiException
     */
    public Map getComment(String token, String commentId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        checkToken(token, context);

        Object[] pair = factory.getDocObjectPair(commentId, context);
        Comment comment = factory.createComment((XWikiDocument)pair[0], (BaseObject)pair[1], context);
        return convert(comment);
    }

    // TODO improve javadoc
    /**
     * Adds a comment to a page.
     * 
     * @param token
     * @param commentParams
     * @return
     * @throws XWikiException
     */
    public Map addComment(String token, Map commentParams) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("commentadd");

        checkToken(token, context);

        Comment comment = new CommentImpl(commentParams, convertor);
        XWikiDocument doc = factory.getDocFromPageId(comment.getPageId(), context);
        if (doc.isMostRecent()) {
            // TODO Q: does this really have to be so complex ? (taken from CommentAddAction)
            Map map = new HashMap();
            map.put("author", context.getUser());
            map.put("date", ""); // dummy value needed
            map.put("comment", comment.getContent());
            BaseClass baseclass = xwiki.getCommentsClass(context);
            String className = baseclass.getName();
            int nb = doc.createNewObject(className, context);
            BaseObject oldobject = doc.getObject(className, nb);
            BaseObject newobject = (BaseObject) baseclass.fromMap(map, oldobject);

            newobject.setNumber(oldobject.getNumber());
            newobject.setName(doc.getFullName());
            doc.setObject(className, nb, newobject);
            String msg = context.getMessageTool().get("core.comment.addComment");
            xwiki.saveDocument(doc, msg, context);

            return convert(factory.createComment(doc, newobject, context));
        } else {
            throw exception("You can only comment on the latest version of a page");
        }
    }

    // TODO improve javadoc
    /**
     * Removes a comment given a comment id.
     * 
     * @param token
     * @param commentId
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException
     */
    public boolean removeComment(String token, String commentId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("objectremove");
        checkToken(token, context);

        Object[] pair = factory.getDocObjectPair(commentId, context);
        XWikiDocument doc = (XWikiDocument)pair[0];
        BaseObject obj = (BaseObject)pair[1];
        if (doc.isMostRecent()) {
            doc.removeObject(obj);
            String msg = context.getMessageTool().get("core.comment.deleteObject");
            xwiki.saveDocument(doc, msg, context);
            return true;
        } else {
            throw exception("You can only remove comments attached to the latest version of a page");
        }
    }


    // Page Management


    // TODO Why is the version needed for updating ?
    /**
     * Add or update a page.
     * 
     * For adding, the Page given as an argument should have - space - title - content
     * 
     * For updating, the Page given should have - id - space - title - content - version
     * 
     * The parentId field is always optional. All other fields will be ignored.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param page a xml-rpc Page
     * @return a Page object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Map storePage(String token, Map pageMap) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("save");
        checkToken(token, context);

        Page page = new PageImpl(pageMap, convertor);
        XWikiDocument doc = null;
        if (page.getId() != null) {
            // page id is set -> save page
            doc = factory.getDocFromPageId(page.getId(), context);
            if (!doc.isMostRecent()) {
                throw exception("You can only edit the latest version of a page");
            }
        } else {
            // page id not set -> create new page or overwrite existing one
            String space = page.getSpace();
            String title = page.getTitle();
            if (space == null || title == null) {
                throw exception("Space and title are required when calling storePage with no id");
            }

            // if page already exists then overwrite it -- could also raise exception
            doc = xwiki.getDocument(space + "." + title, context);

            if (doc == null) {
                // otherwise create a new page
                doc = new XWikiDocument(space, title);
            }
        }
        if (page.getParentId() != null) {
            doc.setParent(page.getParentId());
        }
        doc.setAuthor(context.getUser());
        doc.setContent(page.getContent());
        // TODO "" was page.getComment() (removed)
        context.getWiki().saveDocument(doc, "", context);
        return convert(factory.createPage(doc, context));
    }

    /**
     * Returns the HTML rendered content for a page.
     * 
     * If 'content' is provided, then that is rendered as if it were the body of the page (useful
     * for a 'preview page' function). If it's not provided, then the existing content of the page
     * is used instead (ie useful for 'view page' function).
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey in which space is our page
     * @param pageId id of page to get rendered HTML
     * @param content if this is set, it will replace the original content for rendering
     * @return string representing rendered content of page as HTML
     * @throws XWikiException in case of error
     */
    public String renderContent(String token, String spaceKey, String pageId, String content)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        context.setDoc(doc);
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        xwiki.prepareDocuments(context.getRequest(), context, vcontext);
        if (content.length() == 0) {
            // If content is not provided, then the existing content of the page is used
            content = doc.getContent();
        }
        String result = xwiki.getRenderingEngine().renderText(content, doc, context);
        return result;
    }

    /**
     * Remove a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId id of page to delete
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException in case of error
     */
    public boolean removePage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("delete");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        if (doc.isMostRecent()) {
            context.setDoc(doc);
            // do not use recycle bin (because it has bugs)
            context.getWiki().deleteDocument(doc, false, context);
            return true;
        } else {
            throw exception("You cannot remove an old version of a page");
        }
    }

    // Attachments Retrieval

    /**
     * - get information about an attachment.
     * 
     * @param token
     * @param pageId
     * @param fileName
     * @param versionNumber - what are the possible values here? guessing :( -- xwiki has a
     *            viewattachrev action which would be really useful
     * @return
     * @throws XWikiException
     */
    public Map getAttachment(String token, String pageId, String fileName, String versionNumber)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        XWikiAttachment xAttach = doc.getAttachment(fileName);
        xAttach = xAttach.getAttachmentRevision(versionNumber, context);
        Attachment attachment = factory.createAttachment(doc, xAttach, context);
        return convert(attachment);
    }

    /**
     * - get the contents of an attachment.
     * 
     * @param token
     * @param pageId
     * @param fileName
     * @param versionNumber - what are the possible values here? guessing :( -- xwiki has a
     *            viewattachrev action which would be really useful
     * @return
     * @throws XWikiException
     */
    public byte[] getAttachmentData(String token, String pageId, String fileName,
        String versionNumber) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("download");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        XWikiAttachment xAttach = doc.getAttachment(fileName);
        xAttach = xAttach.getAttachmentRevision(versionNumber, context);
        return xAttach.getContent(context);
    }

    // Attachments Management

    /**
     * - add a new attachment to a content entity object.
     * 
     * @param token
     * @param pageId (redundant)
     * @param attachment
     * @param attachmentData
     * @return
     * @throws XWikiException
     */
    public Map addAttachment(String token, String pageId, Map attachmentMap, byte[] attachmentData)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("upload");
        checkToken(token, context);

        Attachment attachment = new AttachmentImpl(attachmentMap, convertor);

        // TODO Does this really have to be so complex ? (taken from UploadAction)
        XWikiDocument doc = factory.getDocFromPageId(attachment.getPageId(), context);
        // Read XWikiAttachment
        XWikiAttachment xAttachment = doc.getAttachment(attachment.getFileName());
        if (xAttachment == null) {
            xAttachment = new XWikiAttachment();
            doc.getAttachmentList().add(xAttachment);
        }
        xAttachment.setContent(attachmentData);
        xAttachment.setFilename(attachment.getFileName());
        xAttachment.setAuthor(context.getUser());

        // Add the attachment to the document
        xAttachment.setDoc(doc);
        xAttachment.setComment(attachment.getTitle());

        doc.setAuthor(context.getUser()); // TODO WTF ?
        if (doc.isNew()) {
            doc.setCreator(context.getUser());
        }

        // Adding a comment with a link to the download URL
        String comment;
        String nextRev = xAttachment.getNextVersion();
        ArrayList params = new ArrayList();
        params.add(attachment.getFileName());
        params.add(doc.getAttachmentRevisionURL(attachment.getFileName(), nextRev, context));
        if (xAttachment.isImage(context)) {
            comment = context.getMessageTool().get("core.comment.uploadImageComment", params);
        } else {
            comment =
                context.getMessageTool().get("core.comment.uploadAttachmentComment", params);
        }
        doc.setComment(comment);

        doc.saveAttachmentContent(xAttachment, context);

        Attachment resultAttachment = factory.createAttachment(doc, xAttachment, context);
        return convert(resultAttachment);
    }

    /**
     * - remove an attachment from a content entity object.
     * 
     * @param token
     * @param pageId
     * @param fileName
     * @return true
     * @throws XWikiException
     */
    public boolean removeAttachment(String token, String pageId, String fileName)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("delattachment");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        XWikiAttachment xAttachment = doc.getAttachment(fileName);

        doc.setAuthor(context.getUser());

        ArrayList params = new ArrayList();
        params.add(fileName);
        String comment;
        if (xAttachment.isImage(context)) {
            comment = context.getMessageTool().get("core.comment.deleteImageComment", params);
        } else {
            comment =
                context.getMessageTool().get("core.comment.deleteAttachmentComment", params);
        }
        doc.setComment(comment);

        doc.deleteAttachment(xAttachment, context);

        return true;
    }

    /**
     * - move an attachment to a different content entity object and/or give it a new name.
     * 
     * @param token
     * @param originalPageId
     * @param originalName
     * @param newPageId
     * @param newName
     * @return true
     * @throws XWikiException
     */
    public boolean moveAttachment(String token, String originalPageId, String originalName,
        String newPageId, String newName) throws XWikiException
    {
        Map map = getAttachment(token, originalPageId, originalName, null);
        byte[] data = getAttachmentData(token, originalPageId, originalName, null);

        addAttachment(token, newPageId, map, data);
        removeAttachment(token, originalPageId, originalName);

        return true;
    }

    // Search //

    /**
     * Get a list of SearchResults which match a given search query.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param query search query
     * @param maxResults number of maximal results
     * @return a Vector of SearchResults as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] search(String token, String query, int maxResults) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        List doclist =
            xwiki.getStore().searchDocumentsNames(
                "where doc.content like '%" + Utils.SQLFilter(query) + "%' or doc.name like '%"
                    + Utils.SQLFilter(query) + "%'", context);

        // TODO: This is not enough and search is implemented in velocity (SUPER STUPID!!!)
        // http://localhost:8080/xwiki/bin/edit/XWiki/WebSearchCode
        // xwiki.search(sql, context)
        // Q: Would using the Lucene search be a solution?

        if (doclist == null)
            return new Object[0];

        List result = new ArrayList(doclist.size());
        for (int i = 0; i < doclist.size(); i++) {
            String docName = (String) doclist.get(i);
            XWikiDocument doc = xwiki.getDocument(docName, context);
            SearchResult sresult = factory.createSearchResult(doc, context);
            result.add(convert(sresult));
        }
        return result.toArray();
    }

    // /////////////////////
    // Rights Management //
    // /////////////////////

    // ///////////////////
    // User Management //
    // ///////////////////

    /**
     * Get a single User.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username the name of the user we want the User Object
     * @return a User object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Map getUser(String token, String username) throws XWikiException
    {
        // TODO implement!
        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    /**
     * Add a new user with the given password.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param user object of new user
     * @param password of the new user
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException in case of error
     */
    public boolean addUser(String token, Map user, String password) throws XWikiException
    {
        // TODO implement!
        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    /**
     * Add a new group.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param group name of group to add
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException in case of error
     */
    public boolean addGroup(String token, String group) throws XWikiException
    {
        // TODO implement!
        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    /**
     * Get a user's current groups.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username for which we want to recive all groups
     * @return a Vector of Group objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    public Object[] getUserGroups(String token, String username) throws XWikiException
    {
        // TODO implement!
        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    /**
     * Add a user to a particular group.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username name of user to add to a group
     * @param groupname name of group to add user
     * @return true (xml-rpc methods have to return something)
     * @throws XWikiException in case of error
     */
    public boolean addUserToGroup(String token, String username, String groupname)
        throws XWikiException
    {
        // TODO implement!
        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    private Map getTokens(XWikiContext context)
    {
        Map tokens = (Map) context.getEngineContext().getAttribute("xmlrpc_tokens");
        if (tokens == null) {
            tokens = new HashMap();
            context.getEngineContext().setAttribute("xmlrpc_tokens", tokens);
        }
        return tokens;
    }

    /**
     * Verify the authentication token
     * 
     * @param token
     * @param context
     * @throws XWikiException
     */
    private void checkToken(String token, XWikiContext context) throws XWikiException
    {
        RemoteUser user = null;
        String ip = context.getRequest().getRemoteAddr();

        if (token != null) {
            if (!token.equals("")) {
                user = (RemoteUser) getTokens(context).get(token);
            } else {
                // anonymous access
                user = new RemoteUser("XWiki.XWikiGuest", ip);
            }
        }

        if ((user == null) || (!user.ip.equals(ip))) {
            throw exception("Access Denied: authentification token {" + token + "} for ip {" + ip
                + "} is invalid", XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID);
        }

        context.setUser(user.username);
    }

    private XWikiException exception(String message)
    {
        return exception(message, 0);
    }

    private XWikiException exception(String message, int code) {
        return exception(message, code, null);        
    }
    
    private XWikiException exception(String message, Throwable cause) {
        return exception(message, 0, cause);        
    }
    
    private XWikiException exception(String message, int code, Throwable cause)
    {
        log.info("Exception thrown to XML-RPC client: " + message);
        XWikiException ex = new XWikiException();
        ex.setModule(XWikiException.MODULE_XWIKI_XMLRPC);
        ex.setCode(code);
        ex.setMessage(message);
        ex.setException(cause);
        return ex;
    }

    // XWiki-objects and XWiki-classes

    public Object[] getClasses(String token, String pageId) throws XWikiException
    {
        // TODO What action is this ?
        XWikiContext context = getXWikiContext();
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        return doc.getxWikiClasses(context).toArray();
    }

    // TODO: getComments can't really call this now ... refactor the common part out ?
    // -- the creation of the DSO is/(will be) different
    // this DSO scheme needs to be extensible ... leave them Maps ?
    // --- or have structured objects but use toMap for getting extra properties
    // ---- constructor needs to store all extra properties (and fields won't work!)
    // -- if we manage to implement this in a general enough way we can reuse it for a LOT of things
    public Object[] getObjects(String token, String pageId, String className)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);

        List commentlist = doc.getObjects(className);
        if (commentlist != null) {
            ArrayList result = new ArrayList();
            for (int i = 0; i < commentlist.size(); i++) {
                BaseObject obj = (BaseObject) commentlist.get(i);
                if (obj != null) {
                    // Note: checking for null values here is crucial
                    // because comments are just set to null when deleted
                    // TODO - we need a new DSO type!
                    Comment object = factory.createComment(doc, obj, context);
                    result.add(object.toMap());
                }
            }
            return result.toArray();
        } else {
            return new Object[0];
        }
    }

    // TODO this is NOT trivial to implement!
    public Map addObject(String token, Map objectMap) throws XWikiException
    {
        // TODO the class will be one of the fields in the params

        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("objectadd");

        checkToken(token, context);

        Comment object = new CommentImpl(objectMap); // TODO Create another type of DSO ...
        // factory/reflection needed?
        XWikiDocument doc = factory.getDocFromPageId(object.getPageId(), context);
        if (doc.isMostRecent()) {
            // TODO Q: does this really have to be so complex ? (taken from CommentAddAction)
            // TODO Check ObjectAddAction here!!!
            Map map = new HashMap();
            map.put("author", context.getUser());
            map.put("date", ""); // dummy value needed
            map.put("comment", object.getContent()); // probably not
            String className = "";// object.getClassName(); // like this class name includes
            // "XWiki."
            BaseClass baseclass = xwiki.getClass(className, context);
            int nb = doc.createNewObject(className, context);
            BaseObject oldobject = doc.getObject(className, nb);
            BaseObject newobject = (BaseObject) baseclass.fromMap(map, oldobject);

            newobject.setNumber(oldobject.getNumber());
            newobject.setName(doc.getFullName());
            doc.setObject(className, nb, newobject);
            String msg = context.getMessageTool().get("core.comment.addObject"); // TODO the
            // message also
            // depends on
            // the DSO
            xwiki.saveDocument(doc, msg, context);

            return (factory.createComment(doc, newobject, context)).toMap(); // TODO Create another type of
            // DSO here ..
            // factory/reflection needed ?
        } else {
            throw exception("You can only add objects to the latest version of a page");
        }
    }

    // TODO class should also be included in the objectId !!!
    // -- More complex way to construct objectIds needed
    public boolean removeObject(String token, String objectId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("objectremove");
        checkToken(token, context);

        Object[] pair = factory.getDocObjectPair(objectId, context); // Class should also be used
        XWikiDocument doc = (XWikiDocument)pair[0];
        BaseObject obj = (BaseObject)pair[1];
        // here!!!
        if (doc.isMostRecent()) {
            doc.removeObject(obj);
            String msg = context.getMessageTool().get("core.comment.deleteObject");
            xwiki.saveDocument(doc, msg, context);
            return true;
        } else {
            throw exception("You can only remove objects from the latest version of a page");
        }
    }

    // TODO There also needs to be a way to add and remove classes easily
    // --- but how ?

    
    /**
     * Function needed, but missing from the confluence api.
     * Supported only by XWiki.
     * @return A list of strings
     */
    public Object[] getAttachmentVersions(String token, String pageId, String fileName)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("viewattachrev");
        checkToken(token, context);

        XWikiDocument doc = factory.getDocFromPageId(pageId, context);
        XWikiAttachment xAttach = doc.getAttachment(fileName);
        Version[] versions = xAttach.getVersions();
        Object[] result = new Object[versions.length];
        for (int i = 0; i<versions.length; i++) {
            result[i] = versions[i].toString();
        }
        return result;
    }
        
    /**
     * No longer perform useless conversions to String
     * Supported only by XWiki.
     * @return true
     * @throws XWikiException 
     */
    public boolean setNoConversion(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("viewattachrev");
        checkToken(token, context);
        
        setConvertor(new MapConvertor(new IdentityObjectConvertor()));
        return true;
    }
    
    public void setConvertor(MapConvertor convertor) {
        this.convertor = convertor;
    }
    
    protected Map convert(MapObject mo) {
        return convertor.convert(mo.toMap());
    }
    
//    protected Map revert(Map map, Map typeMap) throws XWikiException {
//        try {
//            return mapConvertor.revert(map, typeMap);
//        } catch (SwizzleConversionException e) {
//            throw exception(e.getMessage(), e);
//        }
//    }
//    
//    protected List toList(Object[] vector, Class type) throws XWikiException  {
//        try {
//            List list = new ArrayList(vector.length);
//            
//            Constructor constructor = type.getConstructor(new Class[]{Map.class});
//            for (int i = 0; i < vector.length; i++) {
//                Map data = revert((Map) vector[i], (Map)type.getField("FIELD_TYPES").get(null));
//                Object object = constructor.newInstance(new Object[]{data});
//                list.add(object);
//            }
//            
//            return list;
//
//        } catch (Exception e) {
//            throw exception(e.getMessage(), e);
//        }
//    }
    
    protected Object[] toArray(List list) throws XWikiException  {
        Object[] array = new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            MapObject mo = (MapObject)list.get(i);
            array[i] = convert(mo);
        }
        return array;
    }
}
