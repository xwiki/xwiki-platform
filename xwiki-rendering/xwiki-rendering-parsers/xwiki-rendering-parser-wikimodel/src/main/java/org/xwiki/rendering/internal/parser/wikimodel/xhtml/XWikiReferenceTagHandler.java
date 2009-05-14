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

import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.impl.WikiScannerContext;
import org.wikimodel.wem.xhtml.handler.ReferenceTagHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext;
import org.xwiki.rendering.internal.parser.wikimodel.XDOMGeneratorListener;

/**
 * Override the default WikiModel Reference handler to handle XWiki references since we store some information in
 * comments. We also need to handle the span elements introduced by XWiki.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class XWikiReferenceTagHandler extends ReferenceTagHandler
{
    @Override
    public void initialize(TagStack stack)
    {
        stack.setStackParameter("isInLink", false);
        stack.setStackParameter("isFreeStandingLink", false);
        stack.setStackParameter("linkParameters", WikiParameters.EMPTY);
    }

    @Override
    protected void begin(TagContext context)
    {
        boolean isInLink = (Boolean) context.getTagStack().getStackParameter("isInLink");
        if (isInLink) {
            XDOMGeneratorListener listener =
                (XDOMGeneratorListener) context.getTagStack().getStackParameter("xdomGeneratorListener");
            context.getTagStack().pushScannerContext(new WikiScannerContext(listener));

            // Ensure we simulate a new document being parsed
            context.getScannerContext().beginDocument();

            // Verify if it's a freestanding link and if so save the information so that we can get it in
            // XWikiCommentHandler.
            if (isFreeStandingReference(context)) {
                context.getTagStack().setStackParameter("isFreeStandingLink", true);
            } else {
                // Save the parameters set on the A element so that we can generate the correct link
                // in the XWiki Comment handler. Note that we must exclude the href parameter.
                WikiParameters params = context.getParams();
                params = params.remove("href");
                context.getTagStack().setStackParameter("linkParameters", params);
            }

            setAccumulateContent(false);
        } else {
            super.begin(context);
        }
    }

    @Override
    protected void end(TagContext context)
    {
        boolean isInLink = (Boolean) context.getTagStack().getStackParameter("isInLink");
        if (isInLink) {
            // Ensure we simulate a document parsing end
            context.getScannerContext().endDocument();

            context.getTagStack().popScannerContext();
        } else {
            super.end(context);
        }
    }
}
