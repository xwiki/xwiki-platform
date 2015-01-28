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
package com.xpn.xwiki.plugin.webdav.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * Holds context information about a webdav request.
 * <p>
 * TODO: Get rid of this class (Move to components).
 * 
 * @version $Id$
 */
public class XWikiDavContext
{
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDavContext.class);

    /**
     * Global per-user based storage.
     */
    private static Cache<XWikiDavUserStorage> davCache;

    /**
     * Dav request.
     */
    private DavServletRequest request;

    /**
     * XWiki context.
     */
    private XWikiContext xwikiContext;

    /**
     * DAV resource factory.
     */
    private DavResourceFactory resourceFactory;

    /**
     * DAV session.
     */
    private DavSession davSession;

    /**
     * Lock manager.
     */
    private LockManager lockManager;

    /**
     * Creates a new xwiki webdav context.
     * 
     * @param request dav request.
     * @param response dav response.
     * @param servletContext servlet context.
     * @param resourceFactory dav resource factory.
     * @param davSession dav session.
     * @param lockManager lock manager.
     * @throws DavException if an error occurs while initializing the xwiki context.
     */
    public XWikiDavContext(DavServletRequest request, DavServletResponse response, ServletContext servletContext,
        DavResourceFactory resourceFactory, DavSession davSession, LockManager lockManager) throws DavException
    {
        this.request = request;
        this.resourceFactory = resourceFactory;
        this.davSession = davSession;
        this.lockManager = lockManager;
        // Initialize XWikiContext.
        try {
            XWikiEngineContext xwikiEngine = new XWikiServletContext(servletContext);
            XWikiRequest xwikiRequest = new XWikiServletRequest(request);
            XWikiResponse xwikiResponse = new XWikiServletResponse(response);

            xwikiContext = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiEngine);
            xwikiContext.setMode(XWikiContext.MODE_SERVLET);
            xwikiContext.setWikiId("xwiki");

            ServletContainerInitializer containerInitializer = Utils.getComponent(ServletContainerInitializer.class);
            containerInitializer.initializeRequest(xwikiContext.getRequest().getHttpServletRequest(), xwikiContext);
            containerInitializer.initializeResponse(xwikiContext.getResponse());
            containerInitializer.initializeSession(xwikiContext.getRequest().getHttpServletRequest());
            containerInitializer.initializeApplicationContext(servletContext);

            XWiki xwiki = XWiki.getXWiki(xwikiContext);
            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(xwikiContext.getMode(), xwikiContext);
            xwikiContext.setURLFactory(urlf);
            xwiki.prepareResources(xwikiContext);

            String username = "XWiki.XWikiGuest";
            XWikiUser user = xwikiContext.getWiki().checkAuth(xwikiContext);
            if (user != null) {
                username = user.getUser();
            }
            xwikiContext.setUser(username);

            if (xwikiContext.getDoc() == null) {
                xwikiContext.setDoc(new XWikiDocument("Fake", "Document"));
            }
            xwikiContext.put("ajax", Boolean.TRUE);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        } catch (ServletContainerException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
        // Initialize the cache.
        if (null == davCache) {
            initCache();
        }
    }

    /**
     * Initializes global webdav cache.
     * 
     * @throws DavException if an error occurs while initializing the cache.
     */
    private static void initCache() throws DavException
    {
        try {
            CacheManager cacheManager = Utils.getComponent(CacheManager.class, "default");
            CacheConfiguration conf = new CacheConfiguration();
            LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
            lec.setTimeToLive(300);
            conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);
            davCache = cacheManager.createNewCache(conf);
        } catch (CacheException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Returns the session storage allocated for the current user.
     * 
     * @return Session storage.
     */
    public XWikiDavUserStorage getUserStorage()
    {
        String user = xwikiContext.getUser();
        if (null == davCache.get(user)) {
            davCache.set(user, new XWikiDavUserStorage());
        }
        return davCache.get(user);
    }

    /**
     * Returns if the user (in the context) has the given access level on the document in question.
     * 
     * @param right Access level.
     * @param fullDocName Name of the document.
     * @return True if the user has the given access level for the document in question, false otherwise.
     */
    public boolean hasAccess(String right, String fullDocName)
    {
        boolean hasAccess = false;
        try {
            if (right.equals("overwrite")) {
                String overwriteAccess = exists(fullDocName) ? "delete" : "edit";
                hasAccess = hasAccess(overwriteAccess, fullDocName);
            } else if (xwikiContext.getWiki().getRightService()
                .hasAccessLevel(right, xwikiContext.getUser(), fullDocName, xwikiContext)) {
                hasAccess = true;
            }
        } catch (XWikiException ex) {
            LOGGER.error("Error while validating access level.", ex);
        }
        return hasAccess;
    }

    /**
     * Validates if the user (in the context) has the given access level on the document in question, if not, throws a
     * {@link DavException}.
     * 
     * @param right Access level.
     * @param fullDocName Name of the document.
     * @throws DavException If the user doesn't have enough access rights on the given document or if the access
     *             verification code fails.
     */
    public void checkAccess(String right, String fullDocName) throws DavException
    {
        if (!hasAccess(right, fullDocName)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Returns the mime type of the given attachment.
     * 
     * @param attachment xwiki attachment.
     * @return a mime type string.
     */
    public String getMimeType(XWikiAttachment attachment)
    {
        return attachment.getMimeType(xwikiContext);
    }

    /**
     * Returns the content of the attachment.
     * 
     * @param attachment xwiki attachment.
     * @return attachment content as a byte array.
     * @throws DavException if an error occurs while reading the attachment.
     */
    public byte[] getContent(XWikiAttachment attachment) throws DavException
    {
        try {
            return attachment.getContent(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Utility method for reading a given input stream into a byte array.
     * 
     * @param in input stream.
     * @return a byte array holding data from the given stream.
     * @throws DavException if an error occurs while reading the input stream.
     */
    public byte[] getFileContentAsBytes(InputStream in) throws DavException
    {
        try {
            return IOUtils.toByteArray(in);
        } catch (IOException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Adds an attachment to the {@link XWikiDocument} represented by this resource.
     * 
     * @param attachmentName Name of this attachment.
     * @param data Data to be put into the attachment (file content).
     * @param doc The document to which the attachment is made.
     * @throws DavException Indicates an internal error.
     */
    public void addAttachment(XWikiDocument doc, byte[] data, String attachmentName) throws DavException
    {
        int i = attachmentName.indexOf("\\");
        if (i == -1) {
            i = attachmentName.indexOf(XWikiDavUtils.URL_SEPARATOR);
        }
        String filename = attachmentName.substring(i + 1);

        XWikiAttachment attachment = doc.getAttachment(filename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
        }

        attachment.setContent(data);
        attachment.setFilename(filename);
        attachment.setAuthor(xwikiContext.getUser());

        // Add the attachment to the document
        attachment.setDoc(doc);

        doc.setAuthor(xwikiContext.getUser());
        if (doc.isNew()) {
            doc.setCreator(xwikiContext.getUser());
        }

        try {
            xwikiContext.getWiki().saveDocument(doc, "[WEBDAV] Attachment " + filename + " added.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Moves the given attachment under the target document.
     * 
     * @param attachment xwiki attachment.
     * @param destinationDoc target document.
     * @param newAttachmentName new attachment name.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public void moveAttachment(XWikiAttachment attachment, XWikiDocument destinationDoc, String newAttachmentName)
        throws DavException
    {
        try {
            // Delete the current attachment
            XWikiDocument document = attachment.getDoc();
            document.removeAttachment(attachment);
            this.xwikiContext.getWiki().saveDocument(
                document,
                "Move attachment [" + attachment.getFilename() + "] to document ["
                    + destinationDoc.getDocumentReference() + "]", this.xwikiContext);
            // Rename the (in memory) attachment.
            attachment.setFilename(newAttachmentName);
            // Add the attachment to destination doc.
            destinationDoc.getAttachmentList().add(attachment);
            attachment.setDoc(destinationDoc);
            // Save the attachment.
            destinationDoc.saveAttachmentContent(attachment, xwikiContext);
            xwikiContext.getWiki().saveDocument(destinationDoc, "[WEBDAV] Attachment moved / renamed.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Deletes the given attachment from it's document.
     * 
     * @param attachment xwiki attachment.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public void deleteAttachment(XWikiAttachment attachment) throws DavException
    {
        try {
            XWikiDocument document = attachment.getDoc();

            document.removeAttachment(attachment);
            this.xwikiContext.getWiki().saveDocument(document, "Deleted attachment [" + attachment.getFilename() + "]",
                this.xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Checks whether the specified xwiki document exists or not.
     * 
     * @param fullDocName name of the document.
     * @return true if the documents exists.
     */
    public boolean exists(String fullDocName)
    {
        return xwikiContext.getWiki().exists(fullDocName, xwikiContext);
    }

    /**
     * Finds the xwiki document matching the given document name.
     * 
     * @param fullDocName name of the xwiki document.
     * @return xwiki document matching the given document name.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public XWikiDocument getDocument(String fullDocName) throws DavException
    {
        try {
            return xwikiContext.getWiki().getDocument(fullDocName, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Converts the given xwiki document into an xml representation.
     * 
     * @param document xwiki document.
     * @return the xml representation of the document.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public String toXML(XWikiDocument document) throws DavException
    {
        try {
            return document.toXML(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Renames the given xwiki document into the new document name provided.
     * 
     * @param document xwiki document to be renamed.
     * @param newDocumentName new document name.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public void renameDocument(XWikiDocument document, String newDocumentName) throws DavException
    {
        if (document.isCurrentUserPage(xwikiContext)) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            try {
                document.rename(newDocumentName, xwikiContext);
            } catch (XWikiException ex) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
            }
        }
    }

    /**
     * A shortcut to {@linkXWikiStoreInterface#searchDocumentsNames(String, int, int, XWikiContext)}, returns all the
     * results found.
     * 
     * @param sql the HQL query string.
     * @return document names matching the given criterion.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public List<String> searchDocumentsNames(String sql) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore().searchDocumentsNames(sql, 0, 0, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * A shortcut to {@linkXWikiStoreInterface#searchDocumentsNames(String, int, int, XWikiContext)}.
     * 
     * @param sql the HQL where clause.
     * @param nb number of results expected.
     * @param start offset.
     * @return document names matching the given criterion.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public List<String> searchDocumentsNames(String sql, int nb, int start) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore().searchDocumentsNames(sql, nb, start, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * A shortcut to {@link com.xpn.xwiki.store.XWikiStoreInterface#search(String, int, int, XWikiContext)}.
     * 
     * @param sql the HQL query.
     * @return search results.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public List<Object> search(String sql) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore().search(sql, 0, 0, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Saves the given xwiki document into current xwiki.
     * 
     * @param document xwiki document to be saved.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public void saveDocument(XWikiDocument document) throws DavException
    {
        try {
            xwikiContext.getWiki().saveDocument(document, "[WEBDAV] Modified.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * Deletes the specified xwiki document from the current xwiki.
     * 
     * @param document the xwiki document.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public void deleteDocument(XWikiDocument document) throws DavException
    {
        if (document.isCurrentUserPage(xwikiContext)) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            try {
                xwikiContext.getWiki().deleteDocument(document, xwikiContext);
            } catch (XWikiException ex) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
            }
        }
    }

    /**
     * @return a list of spaces available in the current xwiki.
     * @throws DavException if an error occurs while accessing the wiki.
     */
    public List<String> getSpaces() throws DavException
    {
        try {
            return xwikiContext.getWiki().getSpaces(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    /**
     * @return true if the current webdav request is trying to create a collection resource (DAV_MKCOL).
     */
    public boolean isCreateCollectionRequest()
    {
        return DavMethods.isCreateCollectionRequest(request);
    }

    /**
     * @return true if the current webdav request is trying to create a file resource (DAV_PUT or DAV_POST).
     */
    public boolean isCreateFileRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_PUT || methodCode == DavMethods.DAV_POST;
    }

    /**
     * @return true if the current webdav request is trying to create a resource.
     */
    public boolean isCreateResourceRequest()
    {
        return isCreateCollectionRequest() || isCreateFileRequest();
    }

    /**
     * @return true if the current webdav request is trying to move (rename) a resource.
     */
    public boolean isMoveResourceRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_MOVE;
    }

    /**
     * @return true if the current webdav request is trying to create or move (rename) a resource.
     */
    public boolean isCreateOrMoveRequest()
    {
        return isMoveResourceRequest() || isCreateResourceRequest();
    }

    /**
     * Utility method for checking whether the current webdav request is trying to move / rename an attachment.
     * 
     * @param doc the xwiki document to which the attachment belongs to.
     * @return true if the current webdav request is about moving (or renaming) an attachment from the given xwiki
     *         document.
     */
    public boolean isMoveAttachmentRequest(XWikiDocument doc)
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        if (methodCode == DavMethods.DAV_MOVE) {
            String rPath = request.getRequestLocator().getResourcePath();
            rPath = (rPath.endsWith(XWikiDavUtils.URL_SEPARATOR)) ? rPath.substring(0, rPath.length() - 1) : rPath;
            String resourceName = rPath.substring(rPath.lastIndexOf(XWikiDavUtils.URL_SEPARATOR) + 1);
            return doc.getAttachment(resourceName) != null;
        }
        return false;
    }

    /**
     * @return true if the current webdav request is a DAV_DELETE request.
     */
    public boolean isDeleteResourceRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_DELETE;
    }

    /**
     * @return name of the webdav method executed by the current request.
     */
    public String getMethod()
    {
        return request.getMethod();
    }

    /**
     * @return current xwiki user name.
     */
    public String getUser()
    {
        return xwikiContext.getUser();
    }

    /**
     * @return dav resource factory.
     */
    public DavResourceFactory getResourceFactory()
    {
        return resourceFactory;
    }

    /**
     * @return the dav session.
     */
    public DavSession getDavSession()
    {
        return davSession;
    }

    /**
     * @return global lock manager.
     */
    public LockManager getLockManager()
    {
        return lockManager;
    }

    /**
     * @return the internal xwiki context.
     */
    public XWikiContext getXwikiContext()
    {
        return xwikiContext;
    }

    /**
     * Release any resources acquired.
     */
    public void cleanUp()
    {
        if ((xwikiContext != null) && (xwikiContext.getWiki() != null)) {
            xwikiContext.getWiki().getStore().cleanUp(xwikiContext);
        }
    }
}
