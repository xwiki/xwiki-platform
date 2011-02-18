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

import org.wikimodel.wem.WikiReference;
import org.xwiki.rendering.internal.parser.wikimodel.DefaultXWikiGeneratorListener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;

/**
 * WikiModel listener bridge for the XHTML Syntax.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public class XHTMLXWikiGeneratorListener extends DefaultXWikiGeneratorListener
{
    /**
     * @param parser the parser to use to parse link labels
     * @param listener the XWiki listener to which to forward WikiModel events
     * @param linkReferenceParser the parser to parse link references
     * @param imageReferenceParser the parser to parse image references
     * @param plainRendererFactory used to generate header ids
     * @param idGenerator used to generate header ids
     * @param syntax the syntax of the parsed source
     * @since 3.0M3
     */
    public XHTMLXWikiGeneratorListener(StreamParser parser, Listener listener,
        ResourceReferenceParser linkReferenceParser, ResourceReferenceParser imageReferenceParser,
        PrintRendererFactory plainRendererFactory, IdGenerator idGenerator, Syntax syntax)
    {
        super(parser, listener, linkReferenceParser, imageReferenceParser, plainRendererFactory, idGenerator, syntax);
    }

    /**
     * {@inheritDoc}
     *
     * @see DefaultXWikiGeneratorListener#onReference(org.wikimodel.wem.WikiReference)  
     */
    @Override
    public void onReference(WikiReference reference)
    {
        // We need to handle 2 cases:
        // - when the passed reference is an instance of XWikiWikiReference, i.e. when a XHTML comment defining a XWiki
        //   link has been specified
        // - when the passed reference is not an instance of XWikiWikiReference which will happen if there's no special
        //   XHTML comment defining a XWiki link
        if (!(reference instanceof XWikiWikiReference)) {
            super.onReference(reference);
        } else {
            XWikiWikiReference xwikiReference = (XWikiWikiReference) reference;
            ResourceReference resourceReference = xwikiReference.getReference();

            flushFormat();

            onReference(resourceReference, xwikiReference.getLabel(), xwikiReference.isFreeStanding(),
                convertParameters(xwikiReference.getParameters()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see DefaultXWikiGeneratorListener#onImage(org.wikimodel.wem.WikiReference)
     */
    @Override
    public void onImage(WikiReference reference)
    {
        // We need to handle 2 cases:
        // - when the passed reference is an instance of XWikiWikiReference, i.e. when a XHTML comment defining a XWiki
        //   image has been specified
        // - when the passed reference is not an instance of XWikiWikiReference which will happen if there's no special
        //   XHTML comment defining a XWiki image
        if (!(reference instanceof XWikiWikiReference)) {
            super.onImage(reference);
        } else {
            XWikiWikiReference xwikiReference = (XWikiWikiReference) reference;
            ResourceReference resourceReference = xwikiReference.getReference();

            flushFormat();

            onImage(resourceReference, xwikiReference.isFreeStanding(),
                convertParameters(xwikiReference.getParameters()));
        }
    }
}
