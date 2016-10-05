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
package org.xwiki.vfs.internal.attach;

import java.io.IOException;
import java.net.URI;

import javax.inject.Provider;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import net.java.truevfs.kernel.spec.FsNodeName;

/**
 * Decorator for an {@code AttachNode} to provide all XWiki Model API to get the Attachment corresponding to the VFS
 * node.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class XWikiModelNode
{
    private ComponentManager componentManager;

    private DocumentReference reference;

    private XWikiAttachment attachment;

    private XWikiContext xcontext;

    private URI uri;

    private String name;

    XWikiModelNode(AttachController controller, FsNodeName name)
    {
        this.name = name.toString();
        this.uri = controller.resolve(name).getUri();
        this.componentManager = controller.getComponentManager();
    }

    private ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    /**
     * @return the reference to the Document holding the archive attachment
     * @throws IOException when an error accessing the Document occurs
     */
    public DocumentReference getDocumentReference() throws IOException
    {
        if (this.reference == null) {
            try {
                // Use a default resolver (and not a current one) since we don't have any context, we're in a new
                // request.
                DocumentReferenceResolver<String> documentReferenceResolver =
                    getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING);
                this.reference = documentReferenceResolver.resolve(this.uri.getAuthority());
            } catch (Exception e) {
                throw new IOException(
                    String.format("Failed to compute Document reference for [%s]", this.uri), e);
            }
        }
        return this.reference;
    }

    /**
     * @return the current XWiki Context
     * @throws IOException if an error occurs retrieving the context
     */
    public XWikiContext getXWikiContext() throws IOException
    {
        if (this.xcontext == null) {
            try {
                Provider<XWikiContext> xcontextProvider = getComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
                this.xcontext = xcontextProvider.get();
            } catch (Exception e) {
                throw new IOException(String.format("Failed to get XWiki Context for [%s]", this.uri), e);
            }
        }
        return this.xcontext;
    }

    /**
     * @return true if the attachment exists or false otherwise
     * @since 7.4.1
     * @since 8.0M1
     */
    public boolean hasAttachment()
    {
        boolean result;
        try {
            getAttachment();
            result = true;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * @return the archive attachment itself
     * @throws IOException when an error accessing the Attachment occurs
     */
    public XWikiAttachment getAttachment() throws IOException
    {
        if (this.attachment == null) {
            try {
                XWikiDocument document = getXWikiContext().getWiki().getDocument(
                    getDocumentReference(), getXWikiContext());
                this.attachment = document.getAttachment(this.name);
            } catch (Exception e) {
                throw new IOException(String.format("Failed to get Attachment for [%s]", this.uri), e);
            }
        }
        return this.attachment;
    }
}
