package org.xwiki.rendering.macro;

import org.xwiki.rendering.AbstractRenderingTestCase;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.DOM;
import org.xwiki.velocity.VelocityManager;
import org.jmock.Mock;

import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.StringWriter;

public class IncludeMacroTest extends AbstractRenderingTestCase
{
    public void testIncludeMacroWithCurrentContext() throws Exception
    {
        String expected = "onMacro: [someMacro] [] []\n";

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(VelocityMacro.ROLE, "include/xwiki");
        Mock mockDocumentManager = mock(DocumentManager.class);
        mockDocumentManager.expects(once()).method("getDocumentContent").will(returnValue("{someMacro/}"));
        macro.setDocumentManager((DocumentManager) mockDocumentManager.proxy());

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("document", "wiki:Space.Page");
        properties.put("context", "current");

        List<Block> blocks = macro.execute(properties, null, new DOM(Collections.EMPTY_LIST));

        assertBlocks(expected, blocks);
    }

    public void testIncludeMacroWithNewContext() throws Exception
    {
        String expected = "beginParagraph\n" +
            "onSpecialSymbol: [$]\n" +
            "onWord: [myvar]\n" +
            "endParagraph\n";

        // Since it's not in the same context, we verify that a Velocity variable set in the including page is not
        // seen in the included page.
        VelocityManager velocityManager = (VelocityManager) getComponentManager().lookup(VelocityManager.ROLE);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($myvar = 'hello')");

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(VelocityMacro.ROLE, "include/xwiki");
        Mock mockDocumentManager = mock(DocumentManager.class);
        mockDocumentManager.expects(once()).method("getDocumentContent").will(
            returnValue("{velocity}$myvar{/velocity}"));
        macro.setDocumentManager((DocumentManager) mockDocumentManager.proxy());

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("document", "wiki:Space.Page");
        properties.put("context", "new");

        List<Block> blocks = macro.execute(properties, null, new DOM(Collections.EMPTY_LIST));

        assertBlocks(expected, blocks);
    }
}
