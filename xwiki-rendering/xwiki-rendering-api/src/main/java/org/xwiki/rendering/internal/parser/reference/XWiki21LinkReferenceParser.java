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
package org.xwiki.rendering.internal.parser.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

/**
 * Parses the content of XWiki Syntax 2.1 resource references. The format of a resource reference is the following:
 * {@code (type):(reference)} where {@code type} represents the type (see
 * {@link org.xwiki.rendering.listener.ResourceType} of the resource pointed to (e.g. document, mailto, attachment, 
 * image, document in another wiki, etc), and {@code reference} defines the target. The syntax of {@code reference}
 * depends on the Resource type and is documented in the javadoc of the various
 * {@link org.xwiki.rendering.parser.ResourceReferenceTypeParser} implementations.
 *
 * Note that the implementation is pluggable and it's allowed plug new resource reference types by implementing
 * {@link org.xwiki.rendering.parser.ResourceReferenceTypeParser}s and registering the implementation as a component.
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xwiki/2.1/link")
public class XWiki21LinkReferenceParser extends AbstractXWiki21ResourceReferenceParser
{
    /**
     * Parser to parse link references pointing to URLs.
     */
    @Requirement("url")
    private ResourceReferenceTypeParser urlResourceReferenceTypeParser;

    /**
     * Parser to parse link references pointing to documents.
     */
    @Requirement("doc")
    private ResourceReferenceTypeParser documentResourceReferenceTypeParser;

    /**
     * {@inheritDoc}
     * @see AbstractXWiki21ResourceReferenceParser#parseDefault(String)
     */
    @Override
    protected ResourceReference parseDefault(String rawReference)
    {
        // Try to guess the link type. It can be either:
        // - a URL (specified without the "url" type)
        // - a reference to a document (specified without the "doc" type)
        ResourceReference parsedResourceReference = this.urlResourceReferenceTypeParser.parse(rawReference);
        if (parsedResourceReference == null) {
            // What remains is considered to be a link to a document, use the document link type parser to parse it.
            parsedResourceReference = this.documentResourceReferenceTypeParser.parse(rawReference);
        }
        parsedResourceReference.setTyped(false);

        return parsedResourceReference;
    }
}
