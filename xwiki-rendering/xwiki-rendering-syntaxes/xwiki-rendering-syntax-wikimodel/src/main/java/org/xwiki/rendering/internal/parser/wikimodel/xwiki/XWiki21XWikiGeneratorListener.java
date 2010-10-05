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
import org.xwiki.rendering.listener.DocumentLink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
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
     * @param parser the parser to use to parse link labels
     * @param listener the XWiki listener to which to forward WikiModel events
     * @param linkParser the parser to parse link references
     * @param imageParser the parser to parse image references
     * @param plainRendererFactory used to generate header ids
     * @param idGenerator used to generate header ids
     */
    public XWiki21XWikiGeneratorListener(StreamParser parser, Listener listener, LinkParser linkParser,
        ImageParser imageParser, PrintRendererFactory plainRendererFactory, IdGenerator idGenerator)
    {
        super(parser, listener, linkParser, imageParser, plainRendererFactory, idGenerator);
    }

    /**
     * {@inheritDoc}
     *
     * @see DefaultXWikiGeneratorListener#onReference(String, String, boolean, java.util.Map) 
     */
    protected void onReference(Link link, String label, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Since 2.5M2, handle the special case when the link syntax used for a link to a document has the
        // query string and/or the anchor specified as parameters. This is how the XWiki Syntax 2.1 specifies
        // query string and anchor (ex: [[label>>doc:docReference||queryString="a=b" anchor="anchor"]]).
        if (link.getType().equals(LinkType.DOCUMENT)) {
            String queryString = parameters.remove("queryString");
            if (queryString != null) {
                link.setParameter(DocumentLink.QUERY_STRING, queryString);
            }
            String anchor = parameters.remove("anchor");
            if (anchor != null) {
                link.setParameter(DocumentLink.ANCHOR, anchor);
            }
        }

        super.onReference(link, label, isFreeStandingURI, parameters);
    }
}
