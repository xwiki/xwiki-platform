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
package com.xpn.xwiki.plugin.applicationmanager.core.plugin;

import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Arrays;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Plugin internationalization service based {@link XWikiMessageTool}.
 * 
 * @version $Id$
 */
@Deprecated
public class XWikiPluginMessageTool extends XWikiMessageTool
{
    /**
     * @param locale the locale.
     * @param plugin the plugin.
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    public XWikiPluginMessageTool(Locale locale, XWikiPluginInterface plugin, XWikiContext context)
    {
        this(ResourceBundle.getBundle(plugin.getName() + "/ApplicationResources", locale == null ? Locale.ENGLISH
            : locale), context);
    }

    /**
     * @param bundle the default Resource Bundle to fall back to if no document bundle is found when trying to get a key
     */
    public XWikiPluginMessageTool(ResourceBundle bundle)
    {
        this(bundle, null);
    }

    /**
     * @param bundle the default Resource Bundle to fall back to if no document bundle is found when trying to get a key
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    public XWikiPluginMessageTool(ResourceBundle bundle, XWikiContext context)
    {
        super(bundle, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Start calling <code>context</code>'s {@link XWikiMessageTool#get(String)} then if nothing is found use
     * plugin's {@link ResourceBundle}.
     * 
     * @see com.xpn.xwiki.web.XWikiMessageTool#getTranslation(java.lang.String)
     */
    @Override
    protected String getTranslation(String key)
    {
        String translation = key;

        if (context != null) {
            translation = this.context.getMessageTool().get(key);
        }

        // Want to know if XWikiMessageTool.get return exactly the provided key string (means it
        // found nothing).
        if (translation == key) {
            try {
                translation = this.bundle.getString(key);
            } catch (Exception e) {
                translation = null;
            }
        }

        return translation;
    }

    /**
     * Find a translation and then replace any parameters found in the translation by the passed params parameters. The
     * format is the one used by {@link java.text.MessageFormat}.
     * 
     * @param key the key of the string to find
     * @param params the array of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     * @return the translated string with parameters resolved
     * @see com.xpn.xwiki.web.XWikiMessageTool#get(String, List)
     */
    public String get(String key, String[] params)
    {
        return get(key, Arrays.asList(params));
    }

    /**
     * Find a translation and then replace any parameters found in the translation by the passed param parameter. The
     * format is the one used by {@link java.text.MessageFormat}.
     * 
     * @param key the key of the string to find
     * @param param the parameter to use for replacing "{0}" element in the string. See {@link java.text.MessageFormat}
     *            for the full syntax
     * @return the translated string with parameters resolved
     * @see com.xpn.xwiki.web.XWikiMessageTool#get(String, List)
     */
    public String get(String key, String param)
    {
        return get(key, Collections.singletonList(param));
    }
}
