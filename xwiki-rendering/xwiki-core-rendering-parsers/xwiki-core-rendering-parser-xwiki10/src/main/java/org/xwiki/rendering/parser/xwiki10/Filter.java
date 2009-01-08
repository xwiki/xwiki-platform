package org.xwiki.rendering.parser.xwiki10;

import org.xwiki.rendering.parser.ParseException;

/**
 * Filter provided content into another content.
 * 
 * @version $Id$
 */
public interface Filter
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = Filter.class.getName();

    int getPriority();

    String filter(String content, FilterContext filterContext) throws ParseException;
}
