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
package org.xwiki.rendering.internal.parser;

import java.io.Reader;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;

/**
 * Common code for {@link Parser} implementation that produce a {@link XDOM} from {@link StreamParser}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public abstract class AbstractBlockParser implements Parser
{
    /**
     * Used to lookup the {@link StreamParser} for the syntax.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * @return the {@link StreamParser} to use to parser the input content
     */
    protected StreamParser getStreamParser()
    {
        StreamParser streamParser;
        try {
            streamParser = this.componentManager.lookup(StreamParser.class, getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to create [" + getSyntax().toString() + "] renderer", e);
        }

        return streamParser;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#parse(java.io.Reader)
     */
    public XDOM parse(Reader source) throws ParseException
    {
        XDOMGeneratorListener listener = new XDOMGeneratorListener();

        getStreamParser().parse(source, listener);

        return listener.getXDOM();
    }
}
