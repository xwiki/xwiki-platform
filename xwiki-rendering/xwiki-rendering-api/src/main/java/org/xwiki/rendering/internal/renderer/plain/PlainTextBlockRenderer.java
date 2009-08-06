package org.xwiki.rendering.internal.renderer.plain;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

@Component("plain/1.0")
public class PlainTextBlockRenderer extends AbstractBlockRenderer
{
    @Requirement("plain/1.0")
    private PrintRendererFactory plainTextRendererFactory;

    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.plainTextRendererFactory;
    }
}
