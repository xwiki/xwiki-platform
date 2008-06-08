package org.xwiki.rendering.parser;

import org.xwiki.rendering.listener.Link;

/**
 * Since WikiModel doesn't parse link content we need to do it.
 */
public interface LinkParser
{
    Link parse(String rawLink) throws ParseException;
}
