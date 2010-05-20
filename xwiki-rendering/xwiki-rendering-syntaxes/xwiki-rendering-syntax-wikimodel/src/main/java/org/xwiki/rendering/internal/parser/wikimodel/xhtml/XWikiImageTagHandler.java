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
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiReference;
import org.wikimodel.wem.xhtml.handler.ImgTagHandler;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack;
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext;

/**
 * Handle IMG tag since we're putting the original image reference into XHTML comments so that we can reconstruct the
 * reference when moving back from XHTML to wiki syntax.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class XWikiImageTagHandler extends ImgTagHandler
{
    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.xhtml.handler.TagHandler#initialize(org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack)
     */
    @Override
    public void initialize(TagStack stack)
    {
        stack.setStackParameter("isInImage", false);
        stack.setStackParameter("isFreeStandingImage", false);
        stack.setStackParameter("imageParameters", WikiParameters.EMPTY);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.xhtml.handler.ReferenceTagHandler#begin(org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext)
     */
    @Override
    protected void begin(TagContext context)
    {
        boolean isInImage = (Boolean) context.getTagStack().getStackParameter("isInImage");

        if (isInImage) {
            // Verify if it's a freestanding image uri and if so save the information so that we can get it in
            // XWikiCommentHandler.
            if (isFreeStandingReference(context)) {
                context.getTagStack().setStackParameter("isFreeStandingImage", true);
            } else {
                // Save the parameters set on the IMG element so that we can generate the correct image
                // in the XWiki Comment handler.
                context.getTagStack().setStackParameter("imageParameters",
                    removeMeaningfulParameters(context.getParams()));
            }
        } else {
            super.begin(context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.wikimodel.wem.xhtml.handler.ImgTagHandler#end(org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack.TagContext)
     */
    @Override
    protected void end(TagContext context)
    {
        boolean isInImage = (Boolean) context.getTagStack().getStackParameter("isInImage");

        if (!isInImage) {
            WikiParameter src = context.getParams().getParameter("src");

            if (src != null) {
                WikiParameters parameters = context.getParams().remove("src");

                if (isFreeStandingReference(context)) {
                    context.getScannerContext().onImage(src.getValue());
                } else {
                    WikiReference reference =
                        new WikiReference(src.getValue(), null, removeMeaningfulParameters(parameters));

                    context.getScannerContext().onImage(reference);
                }
            }
        }
    }

    protected WikiParameters removeMeaningfulParameters(WikiParameters parameters)
    {
        WikiParameter classParam = parameters.getParameter("class");
        boolean isFreeStanding =
            ((classParam != null) && classParam.getValue().equalsIgnoreCase("wikimodel-freestanding"));

        if (isFreeStanding) {
            return removeFreestanding(parameters).remove("alt").remove("src");
        } else {
            return removeFreestanding(parameters).remove("src");
        }
    }
}
