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
package org.xwiki.workspace.internal;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.workspace.Workspace;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * Default implementation.
 * 
 * @version $Id$
 */
public class DefaultWorkspace implements Workspace
{
    /** @see Workspace#getWikiDocument() */
    private Wiki wikiDocument;

    /** @see Workspace#getWikiDescriptor() */
    private XWikiServer wikiDescriptor;

    /** @see Workspace#getGroupDocument() */
    private Document groupDocument;

    /**
     * @param wikiDocument the wiki document descriptor
     * @param wikiDescriptor the XWikiServerClass object acting as wiki descriptor contained in the wiki document
     * @param groupDocument the group document that defines user membership to the workspace
     */
    public DefaultWorkspace(Wiki wikiDocument, XWikiServer wikiDescriptor, Document groupDocument)
    {
        this.wikiDocument = wikiDocument;
        this.wikiDescriptor = wikiDescriptor;
        this.groupDocument = groupDocument;
    }

    @Override
    public Wiki getWikiDocument()
    {
        return wikiDocument;
    }

    @Override
    public XWikiServer getWikiDescriptor()
    {
        return wikiDescriptor;
    }

    @Override
    public Document getGroupDocument()
    {
        return groupDocument;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 17).append(wikiDocument == null ? 0 : wikiDocument.getPrefixedFullName())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }

        DefaultWorkspace that = (DefaultWorkspace) object;
        if (this.wikiDocument != null && that.wikiDocument != null) {
            return new EqualsBuilder().append(this.wikiDocument.getPrefixedFullName(),
                that.wikiDocument.getPrefixedFullName()).isEquals();
        } else {
            return false;
        }
    }
}
