package org.xwiki.rendering.internal.renderer.xhtml;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

@Component("xhtml/1.0")
public class XHTMLBlockRenderer extends AbstractBlockRenderer
{
    @Requirement("xhtml/1.0")
    private PrintRendererFactory xhtmlRendererFactory;

    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.xhtmlRendererFactory;
    }
}
