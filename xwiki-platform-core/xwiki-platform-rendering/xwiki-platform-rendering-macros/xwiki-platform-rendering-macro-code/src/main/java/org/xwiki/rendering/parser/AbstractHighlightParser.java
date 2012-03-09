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
package org.xwiki.rendering.parser;

import java.io.Reader;

import org.xwiki.rendering.block.XDOM;

/**
 * Base class for a highlight parser.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
public abstract class AbstractHighlightParser implements HighlightParser
{
    /**
     * The syntax identifier.
     */
    private String syntaxId = "";

    @Override
    public XDOM parse(Reader source) throws ParseException
    {
        return new XDOM(highlight(getSyntaxId(), source));
    }

    /**
     * @param syntaxId the syntax identifier.
     */
    protected void setSyntaxId(String syntaxId)
    {
        this.syntaxId = syntaxId;
    }

    /**
     * @return the syntax identifier.
     */
    protected String getSyntaxId()
    {
        return this.syntaxId;
    }
}
