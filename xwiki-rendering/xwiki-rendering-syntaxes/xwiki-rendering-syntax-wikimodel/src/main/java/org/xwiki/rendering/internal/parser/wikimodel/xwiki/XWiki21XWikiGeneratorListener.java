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
package org.xwiki.rendering.internal.parser.wikimodel.xwiki;

import java.util.Map;

import org.xwiki.rendering.internal.parser.wikimodel.DefaultXWikiGeneratorListener;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;

/**
 * WikiModel listener bridge for the XWiki Syntax 2.1.
 * 
 * @version $Id$
 * @since 2.5RC1
 */
public class XWiki21XWikiGeneratorListener extends DefaultXWikiGeneratorListener
{
    /**
     * Parameter name for Query String.
     */
    private static final String QUERY_STRING = "queryString";

    /**
     * Parameter name for Anchor.
     */
    private static final String ANCHOR = "anchor";

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
    public XWiki21XWikiGeneratorListener(StreamParser parser, Listener listener,
        ResourceReferenceParser linkReferenceParser, ResourceReferenceParser imageReferenceParser,
        PrintRendererFactory plainRendererFactory, IdGenerator idGenerator, Syntax syntax)
    {
        super(parser, listener, linkReferenceParser, imageReferenceParser, plainRendererFactory, idGenerator, syntax);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultXWikiGeneratorListener#onReference(String, String, boolean, java.util.Map)
     */
    protected void onReference(ResourceReference reference, String label, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        // Since 2.5M2, handle the special case when the link syntax used for a link to a document has the
        // query string and/or the anchor specified as parameters. This is how the XWiki Syntax 2.1 specifies
        // query string and anchor (ex: [[label>>doc:docReference||queryString="a=b" anchor="anchor"]]).
        if (reference.getType().equals(ResourceType.DOCUMENT)) {
            String queryString = parameters.remove(QUERY_STRING);
            if (queryString != null) {
                reference.setParameter(DocumentResourceReference.QUERY_STRING, queryString);
            }
            String anchor = parameters.remove(ANCHOR);
            if (anchor != null) {
                reference.setParameter(DocumentResourceReference.ANCHOR, anchor);
            }
        }

        super.onReference(reference, label, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultXWikiGeneratorListener#onImage(org.xwiki.rendering.listener.reference.ResourceReference , boolean,
     *      java.util.Map)
     */
    protected void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Since 2.5M2, handle the special case when the image syntax used for an image has a query string specified.
        if (reference.getType().equals(ResourceType.ATTACHMENT)) {
            String queryString = parameters.remove(QUERY_STRING);
            if (queryString != null) {
                reference.setParameter(DocumentResourceReference.QUERY_STRING, queryString);
            }
        }

        super.onImage(reference, isFreeStandingURI, parameters);
    }
}
