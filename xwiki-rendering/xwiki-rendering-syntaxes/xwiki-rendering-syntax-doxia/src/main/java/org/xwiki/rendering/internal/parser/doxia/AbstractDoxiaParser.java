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
package org.xwiki.rendering.internal.parser.doxia;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMGeneratorListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;

import java.io.Reader;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.util.IdGenerator;

/**
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractDoxiaParser extends AbstractLogEnabled implements Parser, StreamParser
{
    /**
     * Used by the XWikiGeneratorListener to generate unique header ids.
     */
    @Requirement("plain/1.0")
    protected PrintRendererFactory plainRendererFactory;

    @Requirement("plain/1.0")
    private StreamParser plainParser;

    @Requirement("default/link")
    private ResourceReferenceParser linkReferenceParser;

    @Requirement("default/image")
    private ResourceReferenceParser imageReferenceParser;

    public abstract org.apache.maven.doxia.parser.Parser createDoxiaParser();

    /**
     * {@inheritDoc}
     * 
     * @see Parser#parse(Reader)
     */
    public XDOM parse(Reader source) throws ParseException
    {
        IdGenerator idGenerator = new IdGenerator();
        XDOMGeneratorListener listener = new XDOMGeneratorListener();
        parse(source, listener, idGenerator);

        XDOM xdom = listener.getXDOM();
        xdom.setIdGenerator(idGenerator);

        return xdom;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.StreamParser#parse(java.io.Reader, org.xwiki.rendering.listener.Listener)
     */
    public void parse(Reader source, Listener listener) throws ParseException
    {
        IdGenerator idGenerator = new IdGenerator();

        parse(source, listener, idGenerator);
    }

    /**
     * @param source the content to parse
     * @param listener receive event for each element
     * @param idGenerator unique id tool generator
     * @throws ParseException if the source cannot be read or an unexpected error happens during the parsing. Parsers
     *             should be written to not generate any error as much as possible.
     */
    private void parse(Reader source, Listener listener, IdGenerator idGenerator) throws ParseException
    {
        XWikiGeneratorSink doxiaSink =
            new XWikiGeneratorSink(listener, this.linkReferenceParser, this.plainRendererFactory, idGenerator,
                this.plainParser, getSyntax());

        org.apache.maven.doxia.parser.Parser parser = createDoxiaParser();
        try {
            parser.parse(source, doxiaSink);
        } catch (Exception e) {
            throw new ParseException("Failed to parse input source", e);
        }
    }
}
