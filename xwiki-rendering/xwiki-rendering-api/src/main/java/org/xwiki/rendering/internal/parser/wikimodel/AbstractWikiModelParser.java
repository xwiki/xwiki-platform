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

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.wikimodel.XDOMGeneratorListener;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;
import org.wikimodel.wem.IWikiParser;

import java.io.Reader;

/**
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractWikiModelParser extends AbstractLogEnabled implements Parser
{
    protected LinkParser linkParser;

    protected ImageParser imageParser;
    
    public abstract IWikiParser createWikiModelParser() throws ParseException;

    /**
     * @return the parser to use for the link labels, since wikimodel does not support wiki syntax in links and they
     *         need to be handled in the XDOMGeneratorListener. By default, the link label parser is the same one as the
     *         source parser (this), but you should overwrite this method if you need to use a special parser.
     * @see XDOMGeneratorListener#XDOMGeneratorListener(Parser, LinkParser, ImageParser)
     * @see http://code.google.com/p/wikimodel/issues/detail?id=87 
     * TODO: Remove this method when the parser will not need to be passed to the XDOMGeneratorListener anymore.
     */
    public Parser getLinkLabelParser() 
    {
        return this;
    }

    public XDOM parse(Reader source) throws ParseException
    {
        IWikiParser parser = createWikiModelParser();

        // We pass the LinkParser corresponding to the syntax.
        XDOMGeneratorListener listener =
            new XDOMGeneratorListener(this.getLinkLabelParser(), this.linkParser, this.imageParser);

        try {
            parser.parse(source, listener);
        } catch (Exception e) {
            throw new ParseException("Failed to parse input source", e);
        }
        return listener.getXDOM();
    }
}
