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
package com.xpn.xwiki.plugin.webdav.resources.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavResource;
import com.xpn.xwiki.util.Util;

/**
 * The collection resource which represents a page {@link XWikiDocument} of XWiki.
 * 
 * @version $Id$
 */
public class DavPage extends AbstractDavResource
{    
    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DavPage.class);

    /**
     * The name of the space to which this page belongs to.
     */
    private String spaceName;

    /**
     * The {@link XWikiDocument} represented by this resource.
     */
    private XWikiDocument doc;

    /**
     * {@inheritDoc}
     */
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        super.init(parent, name, relativePath);
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            this.spaceName = name.substring(0, dot);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        try {
            this.doc = xwikiContext.getWiki().getDocument(this.name, xwikiContext);
        } catch (XWikiException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
        String timeStamp = DavConstants.creationDateFormat.format(doc.getCreationDate());
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        timeStamp = DavConstants.modificationDateFormat.format(doc.getContentUpdateDate());
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE,
            "text/directory"));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, doc
            .getLanguage()));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, 0));
    }

    /**
     * {@inheritDoc}
     */
    public void decode(Stack<XWikiDavResource> stack, String[] tokens, int next) throws DavException
    {
        if (next < tokens.length) {
            boolean last = (next + 1 == tokens.length);
            String nextToken = tokens[next];
            String relativePath = "/" + nextToken;
            int dot = nextToken.indexOf('.');
            if (dot != -1) {
                if (dot == 0) {
                    // For the moment we'll assume that it can only be a file.
                    DavTempFile davTempFile = new DavTempFile();
                    davTempFile.init(this, nextToken, relativePath);
                    stack.push(davTempFile);
                } else if (!last
                    || xwikiContext.getWiki().exists(nextToken, xwikiContext)
                    || DavMethods.isCreateCollectionRequest((DavServletRequest) xwikiContext
                        .getRequest().getHttpServletRequest())) {
                    DavPage davPage = new DavPage();
                    davPage.init(this, nextToken, relativePath);
                    stack.push(davPage);
                    davPage.decode(stack, tokens, next + 1);
                } else if (nextToken.equals(DavWikiFile.WIKI_TXT)
                    || nextToken.equals(DavWikiFile.WIKI_XML)) {
                    DavWikiFile wikiFile = new DavWikiFile();
                    wikiFile.init(this, nextToken, relativePath);
                    stack.push(wikiFile);
                } else {
                    DavAttachment attachment = new DavAttachment();
                    attachment.init(this, nextToken, relativePath);
                    stack.push(attachment);
                }
            } else {
                if (!last
                    || xwikiContext.getWiki().exists(this.spaceName + "." + nextToken,
                        xwikiContext)
                    || DavMethods.isCreateCollectionRequest((DavServletRequest) xwikiContext
                        .getRequest().getHttpServletRequest())) {
                    DavPage davPage = new DavPage();
                    davPage.init(this, this.spaceName + "." + nextToken, relativePath);
                    stack.push(davPage);
                    davPage.decode(stack, tokens, next + 1);
                } else {
                    DavAttachment attachment = new DavAttachment();
                    attachment.init(this, nextToken, relativePath);
                    stack.push(attachment);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists()
    {
        return !doc.isNew();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCollection()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void spool(OutputContext outputContext) throws IOException
    {
        throw new IOException("Spooling is not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public DavResourceIterator getMembers()
    {
        List<DavResource> children = new ArrayList<DavResource>();
        try {
            String sql = "where doc.parent='" + this.name + "'";
            List<String> docNames =
                xwikiContext.getWiki().getStore().searchDocumentsNames(sql, 0, 0, xwikiContext);
            for (String docName : docNames) {
                if (xwikiContext.getWiki().getRightService().hasAccessLevel("view",
                    xwikiContext.getUser(), docName, xwikiContext)) {
                    XWikiDocument childDoc =
                        xwikiContext.getWiki().getDocument(docName, xwikiContext);
                    DavPage page = new DavPage();
                    if (childDoc.getSpace().equals(this.spaceName)) {
                        page.init(this, docName, "/" + childDoc.getName());
                    } else {
                        page.init(this, docName, "/" + docName);
                    }
                    children.add(page);
                }
            }
            DavWikiFile wikiText = new DavWikiFile();
            wikiText.init(this, DavWikiFile.WIKI_TXT, "/" + DavWikiFile.WIKI_TXT);
            children.add(wikiText);
            DavWikiFile wikiXml = new DavWikiFile();
            wikiXml.init(this, DavWikiFile.WIKI_XML, "/" + DavWikiFile.WIKI_XML);
            children.add(wikiXml);
            sql =
                "select attach.filename from XWikiAttachment as attach, "
                    + "XWikiDocument as doc where attach.docId=doc.id and doc.fullName='"
                    + this.name + "'";
            List attachments = xwikiContext.getWiki().getStore().search(sql, 0, 0, xwikiContext);
            for (int i = 0; i < attachments.size(); i++) {
                String filename = (String) attachments.get(i);
                DavAttachment attachment = new DavAttachment();
                attachment.init(this, filename, "/" + filename);
                children.add(attachment);
            }
            // In-memory resources.
            for (DavResource sessionResource : getSessionResources()) {
                children.add(sessionResource);
            }
        } catch (XWikiException e) {
            LOG.error("Unexpected Error : ", e);
        } catch (DavException e) {
            LOG.error("Unexpected Error : ", e);
        }
        return new DavResourceIteratorImpl(children);
    }

    /**
     * Adds an attachment to the {@link XWikiDocument} represented by this resource.
     * 
     * @param fileName Name of this attachment.
     * @param data Data to be put into the attachment (file content).
     * @param doc The document to which the attachment is made.
     * @param xwikiContext XWiki context.
     * @throws XWikiException Indicates an internal error.
     */
    protected void addAttachment(String fileName, byte[] data, XWikiDocument doc,
        XWikiContext xwikiContext) throws XWikiException
    {
        int i = fileName.indexOf("\\");
        if (i == -1) {
            i = fileName.indexOf("/");
        }
        String filename = fileName.substring(i + 1);

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
        doc.saveAttachmentContent(attachment, xwikiContext);
        xwikiContext.getWiki().saveDocument(doc, "Attachment " + filename + " added",
            xwikiContext);
    }

    /**
     * {@inheritDoc}
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        // TODO : Need to check appropriate rights.
        boolean isFile = (inputContext.getInputStream() != null);
        if (isFile) {
            try {
                // Note : We can do more specific type checking here (a.k.a instanceof) to validate
                // the resource parameter (it could be a XWikiDavWikiFile, XWikiDavAttachment or a
                // XWikiDavTempFile) but for the moment i'm keeping it more generic.
                String fName = resource.getDisplayName();
                byte[] data = Util.getFileContentAsBytes(inputContext.getInputStream());
                if (fName.equals(DavWikiFile.WIKI_TXT)) {
                    doc.setContent(new String(data));
                    xwikiContext.getWiki().saveDocument(doc, "Updated from WebDAV", xwikiContext);
                } else if (fName.equals(DavWikiFile.WIKI_XML)) {
                    throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
                } else if (fName.startsWith(".")) {
                    DavTempFile tempFile = (DavTempFile) resource; 
                    tempFile.setdData(data);
                    getSessionResources().add(tempFile);
                } else {
                    addAttachment(fName, data, doc, xwikiContext);
                }
            } catch (IOException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        } else {
            // Here also we're avoiding type checking.
            String pName = resource.getDisplayName();
            try {
                XWikiDocument childDoc = xwikiContext.getWiki().getDocument(pName, xwikiContext);
                childDoc.setContent("This page was created thorugh xwiki-webdav interface.");
                childDoc.setParent(this.name);
                xwikiContext.getWiki().saveDocument(childDoc, xwikiContext);
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeMember(DavResource member) throws DavException
    {
        // TODO : Need to check appropriate rights.
        // We're avoiding type checking (a.k.a instanceof) for the moment.
        String mName = member.getDisplayName();
        if (mName.equals(DavWikiFile.WIKI_TXT) || mName.equals(DavWikiFile.WIKI_XML)) {
            // Wiki files cannot be deleted, but don't do anything! let the client assume that the
            // delete was a success. This is required since some clients try to delete the file
            // before saving a new (edited) file or when deleting the parent. Still, problems might
            // arise if the client tries to verify the delete by re requesting the resource in which
            // case we'll need yet another (elegant) workaround.
        } else if (mName.startsWith(".")) {
            getSessionResources().remove(member);
        } else if (doc.getAttachment(mName) != null) {
            try {
                doc.deleteAttachment(doc.getAttachment(mName), xwikiContext);
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        } else {
            try {
                XWikiDocument childDoc = xwikiContext.getWiki().getDocument(mName, xwikiContext);
                if (!childDoc.isNew()) {
                    xwikiContext.getWiki().deleteDocument(childDoc, xwikiContext);
                }
            } catch (XWikiException e) {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(DavResource destination) throws DavException
    {
        XWikiDavResource dResource = (XWikiDavResource) destination;
        String dSpaceName = null;
        String dPageName = null;
        int dot = dResource.getDisplayName().lastIndexOf('.');
        if (dot != -1) {
            dSpaceName = dResource.getDisplayName().substring(0, dot);
            dPageName = dResource.getDisplayName().substring(dot + 1);
        } else {
            dSpaceName = this.spaceName;
            dPageName = dResource.getDisplayName();
        }
        try {
            List<String> spaces = xwikiContext.getWiki().getSpaces(xwikiContext);
            if (spaces.contains(dSpaceName)) {
                String newDocName = dSpaceName + "." + dPageName;
                String sql = "where doc.parent='" + this.name + "'";
                List<String> childDocNames =
                    xwikiContext.getWiki().getStore().searchDocumentsNames(sql, 0, 0,
                        xwikiContext);
                doc.rename(newDocName, xwikiContext);
                for (String childDocName : childDocNames) {
                    XWikiDocument childDoc =
                        xwikiContext.getWiki().getDocument(childDocName, xwikiContext);
                    childDoc.setParent(newDocName);
                    xwikiContext.getWiki().saveDocument(childDoc, xwikiContext);
                }
            } else {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST);
            }
        } catch (XWikiException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(DavResource destination, boolean shallow) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public MultiStatusResponse alterProperties(List changeList) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public MultiStatusResponse alterProperties(DavPropertySet setProperties,
        DavPropertyNameSet removePropertyNames) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(DavProperty property) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public String getHref()
    {
        return locator.getHref(false);
    }

    /**
     * {@inheritDoc}
     */
    public long getModificationTime()
    {
        if (exists()) {
            return doc.getContentUpdateDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedMethods()
    {
        return "OPTIONS, GET, HEAD, PROPFIND, PROPPATCH, COPY, DELETE, MOVE, LOCK, UNLOCK";
    }

    /**
     * @return The document represented by this resource.
     */
    public XWikiDocument getDocument()
    {
        return this.doc;
    }
}
