package com.xpn.xwiki.plugin.webdav.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.xmlrpc.XWikiXmlRpcResponse;

public class XWikiDavContext implements LockManager
{
    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(XWikiDavContext.class);

    private DavServletRequest request;

    private XWikiContext xwikiContext;

    private DavResourceFactory resourceFactory;

    private DavSession davSession;

    private LockManager lockManager;

    /**
     * Global per-user based storage.
     */
    private static Cache<XWikiDavUserStorage> davCache;

    public XWikiDavContext(DavServletRequest request, DavServletResponse response,
        ServletContext servletContext, DavResourceFactory resourceFactory, DavSession davSession,
        LockManager lockManager) throws DavException
    {
        this.request = request;
        this.resourceFactory = resourceFactory;
        this.davSession = davSession;
        this.lockManager = lockManager;
        // Initialize XWikiContext.
        try {
            XWikiEngineContext xwikiEngine = new XWikiServletContext(servletContext);
            XWikiRequest xwikiRequest = new XWikiServletRequest(request);
            XWikiResponse xwikiResponse = new XWikiXmlRpcResponse(response);

            xwikiContext = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiEngine);
            xwikiContext.setMode(XWikiContext.MODE_GWT);
            xwikiContext.setDatabase("xwiki");

            ServletContainerInitializer containerInitializer =
                (ServletContainerInitializer) Utils
                    .getComponent(ServletContainerInitializer.class);
            containerInitializer.initializeRequest(xwikiContext.getRequest()
                .getHttpServletRequest(), xwikiContext);
            containerInitializer.initializeResponse(xwikiContext.getResponse()
                .getHttpServletResponse());
            containerInitializer.initializeSession(xwikiContext.getRequest()
                .getHttpServletRequest());
            containerInitializer.initializeApplicationContext(servletContext);

            XWiki xwiki = XWiki.getXWiki(xwikiContext);
            XWikiURLFactory urlf =
                xwiki.getURLFactoryService().createURLFactory(xwikiContext.getMode(),
                    xwikiContext);
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

    private static void initCache() throws DavException
    {
        try {
            CacheManager cacheManager =
                (CacheManager) Utils.getComponent(CacheManager.class, "default");
            CacheFactory factory = cacheManager.getCacheFactory();
            CacheConfiguration conf = new CacheConfiguration();
            LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
            lec.setTimeToLive(300);
            conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);
            davCache = factory.newCache(conf);
        } catch (ComponentLookupException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
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
     * @return True if the user has the given access level for the document in question, false
     *         otherwise.
     */
    public boolean hasAccess(String right, String fullDocName)
    {
        boolean hasAccess = false;
        try {
            if (right.equals("overwrite")) {
                String overwriteAccess = exists(fullDocName) ? "delete" : "edit";
                hasAccess = hasAccess(overwriteAccess, fullDocName);
            } else if (xwikiContext.getWiki().getRightService().hasAccessLevel(right,
                xwikiContext.getUser(), fullDocName, xwikiContext)) {
                hasAccess = true;
            }
        } catch (XWikiException ex) {
            logger.error("Error while validating access level.", ex);
        }
        return hasAccess;
    }

    /**
     * Validates if the user (in the context) has the given access level on the document in
     * question, if not, throws a {@link DavException}.
     * 
     * @param right Access level.
     * @param fullDocName Name of the document.
     * @throws DavException If the user doesn't have enough access rights on the given document or
     *             if the access verification code fails.
     */
    public void checkAccess(String right, String fullDocName) throws DavException
    {
        if (!hasAccess(right, fullDocName)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
    }

    public String getMimeType(XWikiAttachment attachment)
    {
        return attachment.getMimeType(xwikiContext);
    }

    public byte[] getContent(XWikiAttachment attachment) throws DavException
    {
        try {
            return attachment.getContent(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public byte[] getFileContentAsBytes(InputStream in) throws DavException
    {
        try {
            return Util.getFileContentAsBytes(in);
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
     * @throws XWikiException Indicates an internal error.
     */
    public void addAttachment(XWikiDocument doc, byte[] data, String attachmentName)
        throws DavException
    {
        int i = attachmentName.indexOf("\\");
        if (i == -1) {
            i = attachmentName.indexOf("/");
        }
        String filename = attachmentName.substring(i + 1);

        // TODO : avoid name clearing when encoding problems will be solved
        // JIRA : http://jira.xwiki.org/jira/browse/XWIKI-94
        // filename =
        // xwikiContext.getWiki().clearName(filename, false, true, xwikiContext);

        XWikiAttachment attachment = doc.getAttachment(filename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            // Add the attachment in the current doc
            doc.getAttachmentList().add(attachment);
        }

        attachment.setContent(data);
        attachment.setFilename(filename);
        attachment.setAuthor(xwikiContext.getUser());
        // Add the attachment to the document
        attachment.setDoc(doc);
        try {
            doc.saveAttachmentContent(attachment, xwikiContext);
            xwikiContext.getWiki().saveDocument(doc,
                "[WEBDAV] Attachment " + filename + " added.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public void moveAttachment(XWikiAttachment attachment, XWikiDocument destinationDoc,
        String newAttachmentName) throws DavException
    {
        try {
            // Delete the current attachment
            attachment.getDoc().deleteAttachment(attachment, xwikiContext);
            // Rename the (in memory) attachment.
            attachment.setFilename(newAttachmentName);
            // Add the attachment to destination doc.
            destinationDoc.getAttachmentList().add(attachment);
            attachment.setDoc(destinationDoc);
            // Save the attachment.
            destinationDoc.saveAttachmentContent(attachment, xwikiContext);
            xwikiContext.getWiki().saveDocument(destinationDoc,
                "[WEBDAV] Attachment moved / renamed.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public void deleteAttachment(XWikiAttachment attachment) throws DavException
    {
        try {
            attachment.getDoc().deleteAttachment(attachment, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public boolean exists(String fullDocName)
    {
        return xwikiContext.getWiki().exists(fullDocName, xwikiContext);
    }

    public XWikiDocument getDocument(String fullDocName) throws DavException
    {
        try {
            return xwikiContext.getWiki().getDocument(fullDocName, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public String toXML(XWikiDocument document) throws DavException
    {
        try {
            return document.toXML(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public void fromXML(XWikiDocument document, String xml) throws DavException
    {
        try {
            document.fromXML(xml);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, ex);
        }
    }

    public void renameDocument(XWikiDocument document, String newDocumentName)
        throws DavException
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

    public List<String> searchDocumentsNames(String sql) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore()
                .searchDocumentsNames(sql, 0, 0, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public List<String> searchDocumentsNames(String sql, int nb, int start) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore().searchDocumentsNames(sql, nb, start,
                xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    @SuppressWarnings("unchecked")
    public List search(String sql) throws DavException
    {
        try {
            return xwikiContext.getWiki().getStore().search(sql, 0, 0, xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public void saveDocument(XWikiDocument document) throws DavException
    {
        try {
            xwikiContext.getWiki().saveDocument(document, "[WEBDAV] Modified.", xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

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

    public List<String> getSpaces() throws DavException
    {
        try {
            return xwikiContext.getWiki().getSpaces(xwikiContext);
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }

    public boolean isCreateCollectionRequest()
    {
        return DavMethods.isCreateCollectionRequest(request);
    }

    public boolean isCreateFileRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_PUT || methodCode == DavMethods.DAV_POST;
    }

    public boolean isCreateResourceRequest()
    {
        return isCreateCollectionRequest() || isCreateFileRequest();
    }

    public boolean isMoveResourceRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_MOVE;
    }

    public boolean isCreateOrMoveRequest()
    {
        return isMoveResourceRequest() || isCreateResourceRequest();
    }

    public boolean isMoveAttachmentRequest(XWikiDocument doc)
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        if (methodCode == DavMethods.DAV_MOVE) {
            String rPath = request.getRequestLocator().getResourcePath();
            rPath = (rPath.endsWith("/")) ? rPath.substring(0, rPath.length() - 1) : rPath;
            String resourceName = rPath.substring(rPath.lastIndexOf("/") + 1);
            return doc.getAttachment(resourceName) != null;
        }
        return false;
    }

    public boolean isDeleteResourceRequest()
    {
        int methodCode = DavMethods.getMethodCode(getMethod());
        return methodCode == DavMethods.DAV_DELETE;
    }

    public ActiveLock getLock(Type type, Scope scope, DavResource resource)
    {
        return lockManager.getLock(type, scope, resource);
    }

    public ActiveLock createLock(LockInfo lockInfo, DavResource resource) throws DavException
    {
        return lockManager.createLock(lockInfo, resource);
    }

    public boolean hasLock(String lockToken, DavResource resource)
    {
        return lockManager.hasLock(lockToken, resource);
    }

    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken, DavResource resource)
        throws DavException
    {
        return lockManager.refreshLock(lockInfo, lockToken, resource);
    }

    public void releaseLock(String lockToken, DavResource resource) throws DavException
    {
        lockManager.releaseLock(lockToken, resource);
    }

    public String getMethod()
    {
        return request.getMethod();
    }

    public String getUser()
    {
        return xwikiContext.getUser();
    }

    public DavResourceFactory getResourceFactory()
    {
        return resourceFactory;
    }

    public DavSession getDavSession()
    {
        return davSession;
    }

    public LockManager getLockManager()
    {
        return lockManager;
    }

    public XWikiContext getXwikiContext()
    {
        return xwikiContext;
    }

    public void cleanUp()
    {
        if ((xwikiContext != null) && (xwikiContext.getWiki() != null)) {
            xwikiContext.getWiki().getStore().cleanUp(xwikiContext);
        }
    }
}
