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

import org.apache.maven.doxia.module.confluence.ConfluenceParser;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.internal.parser.doxia.AbstractDoxiaParser;

/**
 * @version $Id$
 * @since 1.5M2
 */
@Component("confluence/1.0")
public class DoxiaConfluenceParser extends AbstractDoxiaParser
{
    private static final Syntax SYNTAX = new Syntax(SyntaxType.CONFLUENCE, "1.0");

    /**
     * {@inheritDoc}
     * 
     * @see Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return SYNTAX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractDoxiaParser#createDoxiaParser()
     */
    @Override
    public org.apache.maven.doxia.parser.Parser createDoxiaParser()
    {
        return new ConfluenceParser();
    }
}
