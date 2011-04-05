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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of a wiki page to be indexed. The content is retrieved at indexing time, which should
 * save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id$
 */
public class WikiData extends AbstractIndexData
{
    private static final Log LOG = LogFactory.getLog(WikiData.class);

    public WikiData(WikiReference wikiReference, boolean deleted)
    {
        super(null, wikiReference, deleted);
    }

    @Override
    public Term getTerm()
    {
        return new Term(IndexFields.DOCUMENT_WIKI, getWiki());
    }

    @Override
    protected void getFullText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        // nothing to do
    }

    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
