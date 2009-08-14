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

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.parser.ParseException;

public class DefaultSyntaxFactoryTest extends AbstractRenderingTestCase
{
    public void testCreateSyntaxFromSyntaxIdString() throws Exception
    {
        SyntaxFactory syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);

        Syntax syntax = syntaxFactory.createSyntaxFromIdString("type/version");
        assertEquals("type", syntax.getType().getId());
        assertEquals("type", syntax.getType().getName());
        assertEquals("version", syntax.getVersion());
    }

    public void testCreateSyntaxFromSyntaxIdStringWhenInvalid() throws Exception
    {
        SyntaxFactory syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);

        try {
            syntaxFactory.createSyntaxFromIdString("invalid");
            fail("Should have thrown an exception");
        } catch (ParseException expected) {
            assertEquals("Invalid Syntax format [invalid]", expected.getMessage());
        }
    }
}
