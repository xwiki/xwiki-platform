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

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.doxia.XDOMGeneratorSink;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;

import java.io.Reader;

/**
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractDoxiaParser extends AbstractLogEnabled implements Parser
{
    private LinkParser linkParser;

    protected ImageParser imageParser;

    public abstract org.apache.maven.doxia.parser.Parser createDoxiaParser();

    public XDOM parse(Reader source) throws ParseException
    {
        org.apache.maven.doxia.parser.Parser parser = createDoxiaParser();
        XDOMGeneratorSink sink = new XDOMGeneratorSink(this.linkParser);

        try {
            parser.parse(source, sink);
        } catch (org.apache.maven.doxia.parser.ParseException e) {
            throw new ParseException("Failed to parse input source", e);
        }
        return sink.getDOM();
    }
}
