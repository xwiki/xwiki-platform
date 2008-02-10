package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;

public class SyntaxTestsParent extends MockObjectTestCase
{
    private XWikiContext context;

    private XWikiRadeoxRenderer renderer;

    private Mock mockXWiki;

    private Mock mockDocument;

    private XWikiDocument document;

    protected void setUp()
    {
        this.renderer = new XWikiRadeoxRenderer();
        this.context = new XWikiContext();

        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {
            new XWikiConfig(), context});
        this.context.setWiki((XWiki) this.mockXWiki.proxy());

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        this.context.setDoc(document);
    }

    protected void test(ArrayList tests, ArrayList expects)
    {
        for (int i = 0; i < tests.size(); ++i) {
            String result = renderer.render(tests.get(i).toString(), document, document, context);
            String expected = expects.get(i).toString();
            if (expected.startsWith("...")) {
                assertTrue(result.indexOf(expected.substring(3, expected.length() - 3)) > 0);
            } else {
                assertEquals(expects.get(i).toString(), result);
            }
        }
    }
}
