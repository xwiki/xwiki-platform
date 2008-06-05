package org.xwiki.rendering.parser;

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

import java.util.List;

public class DefaultSyntaxFactoryTest extends AbstractRenderingTestCase
{
    public void testGetAvailableSyntaxes() throws Exception
    {
        SyntaxFactory syntaxFactory = (SyntaxFactory) getComponentManager().lookup(SyntaxFactory.ROLE);
        List<Syntax> syntaxes = syntaxFactory.getAvailableSyntaxes();
        assertEquals(4, syntaxes.size());
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
}
