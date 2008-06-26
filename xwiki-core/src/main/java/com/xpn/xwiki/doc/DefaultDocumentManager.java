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

import org.xwiki.context.Execution;
import org.xwiki.rendering.DocumentManager;
import com.xpn.xwiki.XWikiContext;

/**
 * Temporary class used as a bridge between the old architecture and the new component-based and
 * module-separated one. This is temporary till we remodel the Model classes and the Document
 * services. It's used by the Rendering module.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultDocumentManager implements DocumentManager
{
    private Execution execution;

    public String getDocumentContent(String documentName) throws Exception
    {
        XWikiContext xcontext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        return xcontext.getWiki().getDocument(documentName, xcontext).getContent();
    }

    public boolean exists(String documentName) throws Exception
    {
        XWikiContext xcontext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument doc = xcontext.getWiki().getDocument(documentName, xcontext);
        return !doc.isNew();
    }

    public String getURL(String documentName, String action) throws Exception
    {
        XWikiContext xcontext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        return xcontext.getWiki().getDocument(documentName, xcontext).getURL(action, xcontext);
    }
}