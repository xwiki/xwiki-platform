package org.xwiki.rendering.internal.parser.xwiki10.macro;

import org.xwiki.rendering.parser.xwiki10.macro.AbstractVelocityMacroConverter;

public class IncludeInContextVelocityMacroConverter extends AbstractVelocityMacroConverter
{
    public IncludeInContextVelocityMacroConverter()
    {
        super("include");

        addParameterName("document");
    }
}
