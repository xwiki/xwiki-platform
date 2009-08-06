package org.xwiki.rendering.internal.renderer.event;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

@Component("event/1.0")
public class EventBlockRenderer extends AbstractBlockRenderer
{
    @Requirement("event/1.0")
    private PrintRendererFactory eventRendererFactory;

    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.eventRendererFactory;
    }
}
