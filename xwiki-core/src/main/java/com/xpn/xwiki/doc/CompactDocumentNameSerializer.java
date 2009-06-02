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
package com.xpn.xwiki.doc;

import org.xwiki.bridge.DocumentName;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;

/**
 * Generate a compact document reference string that doesn't contain the wiki name if the document is in 
 * the current wiki.
 * 
 * @version $Id$
 * @since 1.8.3
 */
@Component("compact")
public class CompactDocumentNameSerializer extends DefaultDocumentNameSerializer
{
    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.DefaultDocumentNameSerializer#appendWikiName(java.lang.StringBuffer,
     *      org.xwiki.bridge.DocumentName)
     */
    @Override
    protected void appendWikiName(StringBuffer result, DocumentName documentName)
    {
        if (!documentName.getWiki().equals(getCurrentWikiName())) {
            super.appendWikiName(result, documentName);
        }
    }

    private String getCurrentWikiName()
    {
        return getContext().getDatabase();
    }

    /**
     * @return the XWiki Context used to bridge with the old API
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
