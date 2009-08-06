package org.xwiki.rendering.internal.renderer.tex;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

@Component("tex/1.0")
public class TexBlockRenderer extends AbstractBlockRenderer
{
    @Requirement("tex/1.0")
    private PrintRendererFactory texRendererFactory;

    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.texRendererFactory;
    }
}
