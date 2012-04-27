/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.autotag;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Plugin which extracts a set of tags from a text.
 *
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class AutoTagPluginAPI extends PluginApi<AutoTagPlugin>
{
    /**
     * API constructor. The API must know the plugin object it wraps, and the request context.
     *
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     */
    public AutoTagPluginAPI(AutoTagPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Analyze a piece of text, and extract the most common words into a "tag cloud". In detail, this splits the text
     * into tokens, counts how many times each token appears in the text, removes the "stop-words", joins together words
     * from the same root (stemming), and prepares an HTML tag cloud which can be printed in the response.
     *
     * @param text the text to analyze
     * @param lang the language in which the text is written, {@code 0} for French or {@code 1} for English
     * @return the resulting TagCloud with all the analyzed data, including the HTML tag cloud
     */
    public TagCloud generateTagCloud(String text, int lang)
    {
        return getProtectedPlugin().generateTagCloud(text, lang);
    }

    /**
     * Analyze a piece of text, and extract the most common words into a "tag cloud". In detail, this splits the text
     * into tokens, counts how many times each token appears in the text, removes the "stop-words", joins together words
     * from the same root (stemming), and prepares an HTML tag cloud which can be printed in the response.
     *
     * @param text the text to analyze
     * @param lang the language in which the text is written, one of {@code en} or {@code fr}
     * @return the resulting TagCloud with all the analyzed data, including the HTML tag cloud
     */
    public TagCloud generateTagCloud(String text, String lang)
    {
        return getProtectedPlugin().generateTagCloud(text, getProtectedPlugin().getLanguageConstant(lang));
    }
}
