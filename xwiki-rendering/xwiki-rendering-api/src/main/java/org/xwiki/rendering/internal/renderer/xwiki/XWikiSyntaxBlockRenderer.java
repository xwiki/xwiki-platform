package org.xwiki.rendering.internal.renderer.xwiki;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

@Component("xwiki/2.0")
public class XWikiSyntaxBlockRenderer extends AbstractBlockRenderer
{
    @Requirement("xwiki/2.0")
    private PrintRendererFactory xwikiSyntaxRendererFactory;

    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.xwikiSyntaxRendererFactory;
    }
}
