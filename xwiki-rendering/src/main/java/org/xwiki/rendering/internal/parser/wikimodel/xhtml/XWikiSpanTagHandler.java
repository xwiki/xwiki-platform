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
package org.xwiki.rendering.internal.parser.wikimodel.xhtml;

import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.xhtml.handler.SpanTagHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext;

/**
 * Handle XWiki span elements (we need to ensure we skip link content when we generate them
 * and also skip the spans for the "?" for links pointing to non existing pages).
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class XWikiSpanTagHandler extends SpanTagHandler
{
    @Override
    protected void begin(TagContext context)
    {
        // If we're on a span for unknown links then skip the event for its content.
        // Ex: <a href="..."><span class="wikicreatelinktext">...</span><span class="wikicreatelinkqm">?</span></a>
        WikiParameter classParam = context.getParams().getParameter("class");
        if (classParam != null) {
            if (classParam.getValue().contains("wikigeneratedlinkcontent"))
            {
                setAccumulateContent(true);
            } else if (classParam.getValue().equals("wikilink") 
                || classParam.getValue().equals("wikicreatelink")
                || classParam.getValue().equals("wikiexternallink"))
            {
                // Nothing to do 
            } else if (classParam.getValue().equals("xwikirenderingerror")) {
                setAccumulateContent(true);
            } else  {
                super.begin(context);
            }
        } else {
            super.begin(context);
        }
    }

    @Override
    protected void end(TagContext context)
    {
        WikiParameter classParam = context.getParams().getParameter("class");
        if (classParam != null) {
            if (classParam.getValue().contains("wikigeneratedlinkcontent"))
            {
                setAccumulateContent(false);
            } else if (classParam.getValue().equals("wikilink") 
                || classParam.getValue().equals("wikicreatelink")
                || classParam.getValue().equals("wikiexternallink"))
            {
                // Nothing to do 
            } else if (classParam.getValue().equals("xwikirenderingerror")) {
                setAccumulateContent(false);
            } else {
                super.end(context);
            }
        } else {
            super.end(context);
        }
    }
}
