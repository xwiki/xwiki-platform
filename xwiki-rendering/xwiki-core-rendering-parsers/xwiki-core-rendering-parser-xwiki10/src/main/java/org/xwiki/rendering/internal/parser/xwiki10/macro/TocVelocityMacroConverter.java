package org.xwiki.rendering.internal.parser.xwiki10.macro;

import org.xwiki.rendering.parser.xwiki10.macro.AbstractVelocityMacroConverter;

public class TocVelocityMacroConverter extends AbstractVelocityMacroConverter
{
    public TocVelocityMacroConverter()
    {
        addParameterName("start");
        addParameterName("depth");
        addParameterName("numbered");
    }

    @Override
    public boolean isInline()
    {
        return false;
    }
}
