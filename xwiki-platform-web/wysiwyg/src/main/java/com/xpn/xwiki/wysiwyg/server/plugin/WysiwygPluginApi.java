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
package com.xpn.xwiki.wysiwyg.server.plugin;

import org.apache.ecs.Filter;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.xhtml.input;
import org.xwiki.rendering.parser.Parser;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;

/**
 * Api for the WysiwygPlugin.
 * 
 * @version $Id$
 */
public class WysiwygPluginApi extends Api
{
    /**
     * The plugin instance.
     */
    private WysiwygPlugin plugin;

    /**
     * Creates a new API instance for the given plug-in in the specified context.
     * 
     * @param plugin The underlying plug-in of this plug-in API.
     * @param context The XWiki context.
     */
    public WysiwygPluginApi(WysiwygPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * @return The underlying plug-in of this plug-in API.
     * @see #plugin
     */
    public WysiwygPlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    /**
     * @param plugin The underlying plug-in of this plug-in API.
     * @see #plugin
     */
    public void setPlugin(WysiwygPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @param syntaxId The syntax identifier, like "xwiki/2.0".
     * @return true if the specified syntax is currently supported by the editor.
     */
    public boolean isSyntaxSupported(String syntaxId)
    {
        try {
            Utils.getComponent(Parser.class, syntaxId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Creates an HTML input hidden that could serve as the input for a WYSIWYG editor instance. The editor instance
     * should be configured to have its inputId parameter equal to the id passed to this method.
     * 
     * @param id The id of the generated HTML input.
     * @param source The text that will be converted to HTML and then filled in the value attribute.
     * @param syntax The syntax of the source text.
     * @return The HTML to be included in a page in order to use the input.
     */
    public String getInput(String id, String source, String syntax)
    {
        String value = ((HTMLConverter) Utils.getComponent(HTMLConverter.class)).toHTML(source, syntax);

        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String svalue = filter.process(value);

        input hidden = new input();
        hidden.setType(input.hidden);
        hidden.setFilter(filter);
        hidden.setID(id);
        hidden.setDisabled(true);
        hidden.setValue(svalue);

        return hidden.toString();
    }
}
