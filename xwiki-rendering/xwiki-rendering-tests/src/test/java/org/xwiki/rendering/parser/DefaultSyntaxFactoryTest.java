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

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

import java.util.List;

public class DefaultSyntaxFactoryTest extends AbstractRenderingTestCase
{
    public void testGetAvailableSyntaxes() throws Exception
    {
        SyntaxFactory syntaxFactory = (SyntaxFactory) getComponentManager().lookup(SyntaxFactory.ROLE);
        List<Syntax> syntaxes = syntaxFactory.getAvailableSyntaxes();
        assertEquals(6, syntaxes.size());
    }

    public void testCreateSyntaxFromSyntaxIdString() throws Exception
    {
        SyntaxFactory syntaxFactory = (SyntaxFactory) getComponentManager().lookup(SyntaxFactory.ROLE);

        // Verify that we can use uppercase in the syntax type name
        Syntax syntax1 = new Syntax(SyntaxType.XWIKI, "1.0");
        assertEquals(syntax1, syntaxFactory.createSyntaxFromIdString("XWiki/1.0"));

        Syntax syntax2 = new Syntax(SyntaxType.XWIKI, "2.0");
        assertEquals(syntax2, syntaxFactory.createSyntaxFromIdString("xwiki/2.0"));
    }

    public void testCreateSyntaxFromSyntaxIdStringWhenInvalidSyntaxSpecified() throws Exception
    {
        SyntaxFactory syntaxFactory = (SyntaxFactory) getComponentManager().lookup(SyntaxFactory.ROLE);

        try {
            syntaxFactory.createSyntaxFromIdString("invalidSyntax/1.0");
            fail("Should have thrown an exception here");
        } catch (ParseException expected) {
            assertEquals("Unknown Syntax id [invalidSyntax]. Valid syntaxes are [xwiki], [confluence], [mediawiki], "
                + "[creole], [jspwiki], [twiki], [xhtml]", expected.getMessage());
        }
    }

}
