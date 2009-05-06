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
 *
 */
package com.xpn.xwiki.plugin.charts;

import java.util.LinkedList;
import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.DefaultXWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.render.XWikiRenderer;

public class CustomXWikiRenderingEngine extends DefaultXWikiRenderingEngine
{
    public CustomXWikiRenderingEngine(XWiki xwiki, XWikiContext context) throws XWikiException
    {
        super(xwiki, context);
        List<String> unneededRenderers = new LinkedList<String>();
        boolean found = false;
        for (String name : getRendererNames()) {
            XWikiRenderer renderer = getRenderer(name);
            if (renderer instanceof XWikiRadeoxRenderer || found) {
                found = true;
                unneededRenderers.add(name);
            }
        }

        for (String name : unneededRenderers) {
            removeRenderer(name);
        }
    }
}
