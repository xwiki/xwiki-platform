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
package com.xpn.xwiki.plugin.lucene;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of a wiki page to be indexed. The content is retrieved at indexing
 * time, which should save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id: $
 */
public class DocumentData extends IndexData
{
    public DocumentData(final XWikiDocument doc, final XWikiContext context)
    {
        super(doc, context);

        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    /**
     * @see IndexData#getType()
     */
    public String getType()
    {
        return LucenePlugin.DOCTYPE_WIKIPAGE;
    }

    /**
     * @return a string containing the result of {@link IndexData#getFullText} plus the full text
     *         content of this document (in the given language)
     */
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer text = new StringBuffer(super.getFullText(doc, context));
        text.append(" ");
        text.append(super.getDocumentTitle());
        text.append(" ");
        text.append(doc.getContent());

        return text.toString();
    }
}
