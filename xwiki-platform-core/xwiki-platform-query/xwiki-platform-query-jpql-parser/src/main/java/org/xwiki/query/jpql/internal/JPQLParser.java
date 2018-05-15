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
package org.xwiki.query.jpql.internal;

import java.io.PushbackReader;
import java.io.StringReader;

import org.xwiki.query.internal.jpql.lexer.Lexer;
import org.xwiki.query.internal.jpql.node.Start;
import org.xwiki.query.internal.jpql.parser.Parser;

/**
 * JPQL Parser.
 *
 * @version $Id$
 */
public class JPQLParser
{
    /**
     * Parse JPQL content passed as parameter.
     *
     * @param stmt the statement containing the JPQL content to parse
     * @return the parsed result as an AST tree
     * @throws Exception in case of a parsing error
     */
    public Start parse(String stmt) throws Exception
    {
        Parser p = new Parser(new Lexer(new PushbackReader(new StringReader(stmt))));
        Start start = p.parse();
        return start;
    }
}
