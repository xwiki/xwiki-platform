package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.Map;

public interface RadeoxMacroConverter
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = RadeoxMacroConverter.class.getName();

    boolean supportContent();

    boolean protectResult();
    
    boolean isInline();

    String convert(String name, Map<String, String> parameters, String content);
}
