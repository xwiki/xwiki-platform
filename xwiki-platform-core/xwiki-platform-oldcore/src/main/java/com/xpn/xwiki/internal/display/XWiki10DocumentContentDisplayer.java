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
package com.xpn.xwiki.internal.display;

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentContentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderingEngine;

/**
 * Document content displayer specific for the XWiki 1.0 syntax.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("content/xwiki/1.0")
@Singleton
public class XWiki10DocumentContentDisplayer extends DocumentContentDisplayer
{
    @Override
    protected XDOM display(DocumentModelBridge document, String nameSpace, DocumentDisplayerParameters parameters)
    {
        if (!parameters.isContentTransformed()) {
            return super.display(document, nameSpace, parameters);
        }

        XWikiContext context = getXWikiContext();
        try {
            String content = document.getContent();
            if (parameters.isContentTranslated()) {
                content = ((XWikiDocument) document).getTranslatedContent(context);
            }
            XWikiRenderingEngine renderingEngine = context.getWiki().getRenderingEngine();
            String result = renderingEngine.renderText(content, context.getDoc(), context);
            return generateXDOM(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    @Deprecated
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) getExecution().getContext().getProperty("xwikicontext");
    }

    /**
     * Generates an XDOM from the given rendered content by wrapping it in a {@link RawBlock}.
     * 
     * @param renderedContent the rendered content
     * @return an XDOM that produces the given rendered content when rendered
     */
    private XDOM generateXDOM(String renderedContent)
    {
        return new XDOM(Collections.<Block> singletonList(new RawBlock(renderedContent, Syntax.XHTML_1_0)));
    }
}
