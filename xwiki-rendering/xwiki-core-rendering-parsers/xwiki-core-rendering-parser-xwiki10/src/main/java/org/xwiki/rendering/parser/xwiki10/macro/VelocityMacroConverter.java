package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.List;

/**
 * Convert velocity macro to XWiki 2.0 syntax or macro.
 * 
 * @version $Id$
 */
public interface VelocityMacroConverter
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = VelocityMacroConverter.class.getName();

    boolean protectResult();

    boolean isInline();

    String convert(String name, List<String> parameters);
}
