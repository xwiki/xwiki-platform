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
package org.xwiki.wiki.descriptor.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.wiki.Wiki;

import com.xpn.xwiki.doc.XWikiDocument;

public class DefaultWiki extends Wiki
{
    /**
     * Relative reference to the XWiki.XWikiServerClass containing wiki descriptor metadata.
     */
    public static final EntityReference SERVER_CLASS = new EntityReference("XWikiServerClass", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    private XWikiDocument document;

    public DefaultWiki(String wikiId, String wikiAlias, XWikiDocument document)
    {
        super(wikiId, wikiAlias);
        this.document = document;
    }

    public XWikiDocument getDocument() {
        return document;
    }

}
