/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 *
 * Created on 25.01.2005
 *
 */

package net.jkraemer.xwiki.plugins.lucene;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of a wiki page to be indexed. The content is
 * retrieved at indexing time, which should save us some memory especially when
 * rebuilding an index for a big wiki.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class DocumentData extends IndexData
{
    private static final Logger LOG = Logger.getLogger (DocumentData.class);

    public DocumentData (final XWikiDocument doc, final XWikiContext context)
    {
        super (doc, context);
        setAuthor (doc.getAuthor ());
        setCreator (doc.getCreator ());
        setModificationDate (doc.getDate ());
        setCreationDate (doc.getCreationDate ());
    }

    /**
     * @see net.jkraemer.xwiki.plugins.lucene.IndexData#getType()
     */
    public String getType ()
    {
        return LucenePlugin.DOCTYPE_WIKIPAGE;
    }

    /**
     * @return a string containing the result of
     *         {@link IndexData#getFullText(XWikiDocument, XWikiContext, String)}
     *         plus the full text content of this document (in the given
     *         language)
     */
    public String getFullText (XWikiDocument doc, XWikiContext context)
    {
        return new StringBuffer (super.getFullText (doc, context)).append (" ").append (doc.getContent ())
                .toString ();
    }

}
