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
package org.xwiki.contrib.wiki30.internal;

import org.xwiki.contrib.wiki30.Workspace;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.wikimanager.doc.Wiki;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * TODO DOCUMENT ME!
 * 
 * @version $Id:$
 */
public class DefaultWorkspace implements Workspace
{
    private Wiki wikiDocument;

    private XWikiServer wikiDescriptor;

    private Document groupDocument;

    public DefaultWorkspace(Wiki wikiDocument, XWikiServer wikiDescriptor, Document groupDocument)
    {
        this.wikiDocument = wikiDocument;
        this.wikiDescriptor = wikiDescriptor;
        this.groupDocument = groupDocument;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.contrib.wiki30.Workspace#getWikiDocument()
     */
    public Wiki getWikiDocument()
    {
        return wikiDocument;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.contrib.wiki30.Workspace#getWikiDescriptor()
     */
    public XWikiServer getWikiDescriptor()
    {
        return wikiDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.contrib.wiki30.Workspace#getGroupDocument()
     */
    public Document getGroupDocument()
    {
        return groupDocument;
    }

}
