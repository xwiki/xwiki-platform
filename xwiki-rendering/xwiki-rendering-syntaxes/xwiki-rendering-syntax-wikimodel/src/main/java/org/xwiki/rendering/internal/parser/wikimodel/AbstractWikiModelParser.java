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
package org.xwiki.rendering.internal.parser.wikimodel;

import java.io.Reader;

import org.wikimodel.wem.IWikiParser;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMGeneratorListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Common code for all WikiModel-based parsers.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractWikiModelParser extends AbstractLogEnabled implements Parser, WikiModelStreamParser
{
    /**
     * Used by the XWiki Generator Listener to generate unique header ids.
     */
    @Requirement("plain/1.0")
    protected PrintRendererFactory plainRendererFactory;

    /**
     * @return the WikiModel parser instance to use to parse input content.
     * @throws ParseException when there's a problem creating an instance of the parser to use
     */
    public abstract IWikiParser createWikiModelParser() throws ParseException;

    /**
     * @return the parser to use when parsing link references. We need to parse link references to transform them from
     *         a string representation coming from WikiModel into a
     *         {@link org.xwiki.rendering.listener.reference.ResourceReference} object.
     * @since 2.5RC1
     */
    public abstract ResourceReferenceParser getLinkReferenceParser();

    /**
     * @return the parser to use when parsing image references. We need to parse image references to transform them from
     *         a string representation coming from WikiModel into a
     *         {@link org.xwiki.rendering.listener.reference.ResourceReference} object.
     * @since 2.5RC1
     */
    public abstract ResourceReferenceParser getImageReferenceParser();

    /**
     * @return the syntax parser to use for parsing link labels, since wikimodel does not support wiki syntax in links
     *         and they need to be handled in the {@link XWikiGeneratorListener}. By default, the link label parser is
     *         the same one as the source parser (this), but you should overwrite this method if you need to use a
     *         special parser.
     * @see XDOMGeneratorListener
     * @see <a href="http://code.google.com/p/wikimodel/issues/detail?id=87">wikimodel issue 87</a>
     * @since 2.1RC1
     */
    // TODO: Remove this method when the parser will not need to be passed to the XDOMGeneratorListener anymore.
    protected StreamParser getLinkLabelParser()
    {
        return this;
    }

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
     * {@inheritDoc}
     *
     * @see WikiModelStreamParser#createXWikiGeneratorListener(Listener, IdGenerator)
     */
    public XWikiGeneratorListener createXWikiGeneratorListener(Listener listener, IdGenerator idGenerator)
    {
        return new DefaultXWikiGeneratorListener(getLinkLabelParser(), listener, getLinkReferenceParser(),
            getImageReferenceParser(), this.plainRendererFactory, idGenerator, getSyntax());
    }

    /**
     * @param source the content to parse
     * @param listener receive event for each element
     * @param idGenerator unique id tool generator
     * @throws ParseException if the source cannot be read or an unexpected error happens during the parsing. Parsers
     *             should be written to not generate any error as much as possible.
     * @since 2.1RC1
     */
    private void parse(Reader source, Listener listener, IdGenerator idGenerator) throws ParseException
    {
        IWikiParser parser = createWikiModelParser();
        try {
            parser.parse(source, createXWikiGeneratorListener(listener, idGenerator));
        } catch (Exception e) {
            throw new ParseException("Failed to parse input source", e);
        }
    }
}
