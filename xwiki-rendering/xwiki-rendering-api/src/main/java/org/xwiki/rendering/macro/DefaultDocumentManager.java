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
package org.xwiki.rendering.macro;

import org.xwiki.context.Execution;

public class DefaultDocumentManager implements DocumentManager
{
    private Execution execution;

    public DefaultDocumentManager(Execution execution)
    {
        this.execution = execution;
    }

    public String getDocumentContent(String documentName) throws Exception
    {
        // We use reflection so that we don't draw any dependency on XWiki Core since otherwise
        // we would create a circular dependency. Here's the version without reflection:
        //
        // XWikiContext xcontext =
        //    (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        // return xcontext.getWiki().getDocument(documentName, xcontext).getContent();

        Object xcontext = this.execution.getContext().getProperty("xwikicontext");

        Object xwiki = xcontext.getClass().getMethod("getWiki").invoke(xcontext);
        Object document = xwiki.getClass().getMethod("getDocument", String.class, Object.class)
            .invoke(xwiki, documentName, xcontext);

        return (String) document.getClass().getMethod("getContent").invoke(document);
    }
}
