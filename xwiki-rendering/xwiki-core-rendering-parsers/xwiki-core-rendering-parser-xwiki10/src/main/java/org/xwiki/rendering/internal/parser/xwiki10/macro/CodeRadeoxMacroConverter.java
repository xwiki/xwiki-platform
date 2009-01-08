package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.Map;

import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;

public class CodeRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    @Override
    protected void convertParameter(Map<String, String> parameters20, String key, String value)
    {
        if (key.equals("")) {
            super.convertParameter(parameters20, "language", value);
        } else {
            super.convertParameter(parameters20, key, value);
        }
    }

    public boolean supportContent()
    {
        return true;
    }
    
    @Override
    public boolean isInline()
    {
        return false;
    }
}
