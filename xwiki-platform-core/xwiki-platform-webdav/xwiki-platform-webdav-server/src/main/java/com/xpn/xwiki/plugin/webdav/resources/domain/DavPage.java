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

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.partial.AbstractDavResource;

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
    private static final Logger logger = LoggerFactory.getLogger(DavPage.class);

    /**
     * The name of the space to which this page belongs to.
     */
    private String spaceName;

    /**
     * The {@link XWikiDocument} represented by this resource.
     */
    private XWikiDocument doc;

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath) throws DavException
    {
        super.init(parent, name, relativePath);
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            this.spaceName = name.substring(0, dot);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        this.doc = getContext().getDocument(this.name);
        String timeStamp = DavConstants.creationDateFormat.format(doc.getCreationDate());
        getProperties().add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        timeStamp = DavConstants.modificationDateFormat.format(doc.getContentUpdateDate());
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, timeStamp));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETETAG, timeStamp));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, "text/directory"));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE, doc.getLanguage()));
        getProperties().add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, 0));
    }

    @Override
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        XWikiDavResource resource = null;
        String relativePath = "/" + nextToken;
        if (isTempResource(nextToken)) {
            return super.decode(tokens, next);
        } else if (nextToken.equals(DavWikiFile.WIKI_TXT) || nextToken.equals(DavWikiFile.WIKI_XML)) {
            resource = new DavWikiFile();
            resource.init(this, nextToken, relativePath);
        } else if (doc.getAttachment(nextToken) != null || (last && getContext().isCreateFileRequest())
            || (last && getContext().isMoveAttachmentRequest(doc))) {
            resource = new DavAttachment();
            resource.init(this, nextToken, relativePath);
        } else {
            int dot = nextToken.indexOf('.');
            String pageName = (dot != -1) ? nextToken : this.spaceName + "." + nextToken;
            resource = new DavPage();
            resource.init(this, pageName, relativePath);
        }
        return last ? resource : resource.decode(tokens, next + 1);
    }

    @Override
    public boolean exists()
    {
        return !doc.isNew();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public DavResourceIterator getMembers()
    {
        // Protect against direct url referencing.
        List<DavResource> children = new ArrayList<DavResource>();
        if (!getContext().hasAccess("view", this.name)) {
            return new DavResourceIteratorImpl(children);
        }
        try {
            String sql = "where doc.parent='" + this.name + "'";
            List<String> docNames = getContext().searchDocumentsNames(sql);
            for (String docName : docNames) {
                if (!createsCycle(docName) && getContext().hasAccess("view", docName)) {
                    XWikiDocument childDoc = getContext().getDocument(docName);
                    DavPage page = new DavPage();
                    if (childDoc.getSpace().equals(this.spaceName)) {
                        page.init(this, docName, "/" + childDoc.getName());
                    } else {
                        page.init(this, docName, "/" + docName);
                    }
                    children.add(page);
                }
            }
            sql =
                "select attach.filename from XWikiAttachment as attach, "
                    + "XWikiDocument as doc where attach.docId=doc.id and doc.fullName='" + this.name + "'";
            List attachments = getContext().search(sql);
            for (int i = 0; i < attachments.size(); i++) {
                String filename = (String) attachments.get(i);
                DavAttachment attachment = new DavAttachment();
                attachment.init(this, filename, "/" + filename);
                children.add(attachment);
            }
            children.addAll(getVirtualMembers());
        } catch (DavException e) {
            logger.error("Unexpected Error : ", e);
        }
        return new DavResourceIteratorImpl(children);
    }

    @Override
    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        getContext().checkAccess("edit", this.name);
        boolean isFile = (inputContext.getInputStream() != null);
        if (resource instanceof DavTempFile) {
            addVirtualMember(resource, inputContext);
        } else if (resource instanceof DavPage) {
            String pName = resource.getDisplayName();
            getContext().checkAccess("edit", pName);
            XWikiDocument childDoc = getContext().getDocument(pName);
            childDoc.setContent("This page was created through the WebDAV interface.");
            childDoc.setParent(this.name);
            getContext().saveDocument(childDoc);
        } else if (isFile) {
            String fName = resource.getDisplayName();
            byte[] data = getContext().getFileContentAsBytes(inputContext.getInputStream());
            if (fName.equals(DavWikiFile.WIKI_TXT)) {
                doc.setContent(new String(data));
                getContext().saveDocument(doc);
            } else if (fName.equals(DavWikiFile.WIKI_XML)) {
                throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            } else {
                getContext().addAttachment(doc, data, fName);
            }
        } else {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void removeMember(DavResource member) throws DavException
    {
        getContext().checkAccess("edit", this.name);
        XWikiDavResource dResource = (XWikiDavResource) member;
        String mName = dResource.getDisplayName();
        if (dResource instanceof DavTempFile) {
            removeVirtualMember(dResource);
        } else if (dResource instanceof DavWikiFile) {
            getContext().checkAccess("delete", this.name);
            removeVirtualMember(dResource);
        } else if (dResource instanceof DavAttachment) {
            getContext().deleteAttachment(doc.getAttachment(mName));
        } else if (dResource instanceof DavPage) {
            XWikiDocument childDoc = getContext().getDocument(mName);
            getContext().checkAccess("delete", childDoc.getFullName());
            if (!childDoc.isNew()) {
                getContext().deleteDocument(childDoc);
            }
        } else {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        dResource.clearCache();
    }

    @Override
    public void move(DavResource destination) throws DavException
    {
        // Renaming a page requires edit rights on the current document, overwrite rights on the
        // target document and edit rights on all the children of current document.
        getContext().checkAccess("edit", this.name);
        if (destination instanceof DavPage) {
            DavPage dPage = (DavPage) destination;
            XWikiDocument dDoc = dPage.getDocument();
            List<String> spaces = getContext().getSpaces();
            if (spaces.contains(dDoc.getSpace())) {
                String newDocName = dDoc.getFullName();
                String sql = "where doc.parent='" + this.name + "'";
                List<String> childDocNames = getContext().searchDocumentsNames(sql);
                // Validate access rights for the destination page.
                getContext().checkAccess("overwrite", newDocName);
                // Validate access rights for all the child pages.
                for (String childDocName : childDocNames) {
                    getContext().checkAccess("edit", childDocName);
                }
                getContext().renameDocument(doc, newDocName);
                for (String childDocName : childDocNames) {
                    XWikiDocument childDoc = getContext().getDocument(childDocName);
                    childDoc.setParent(newDocName);
                    getContext().saveDocument(childDoc);
                }
            }
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        clearCache();
    }

    @Override
    public List<XWikiDavResource> getInitMembers()
    {
        List<XWikiDavResource> initialMembers = new ArrayList<XWikiDavResource>();
        try {
            DavWikiFile wikiText = new DavWikiFile();
            wikiText.init(this, DavWikiFile.WIKI_TXT, "/" + DavWikiFile.WIKI_TXT);
            initialMembers.add(wikiText);
            DavWikiFile wikiXml = new DavWikiFile();
            wikiXml.init(this, DavWikiFile.WIKI_XML, "/" + DavWikiFile.WIKI_XML);
            initialMembers.add(wikiXml);
        } catch (DavException ex) {
            logger.error("Error while initializing members.", ex);
        }
        return initialMembers;
    }

    @Override
    public boolean isCollection()
    {
        return true;
    }

    @Override
    public void spool(OutputContext outputContext) throws IOException
    {
        throw new IOException("Collection resources can't be spooled");
    }

    @Override
    public long getModificationTime()
    {
        if (exists()) {
            return doc.getContentUpdateDate().getTime();
        }
        return IOUtil.UNDEFINED_TIME;
    }

    /**
     * @return The document represented by this resource.
     */
    public XWikiDocument getDocument()
    {
        return this.doc;
    }

    /**
     * Utility method to verify that a member of this resource doesn't give rise to a cycle.
     * 
     * @param childDocName Name of the want-to-be-member resource.
     * @return True if the childPageName has occured before, false otherwise.
     */
    public boolean createsCycle(String childDocName)
    {
        DavResource ancestor = this;
        while (ancestor instanceof DavPage && ancestor != null) {
            if (ancestor.getDisplayName().equals(childDocName)) {
                return true;
            }
            ancestor = ancestor.getCollection();
        }
        return false;
    }
}
