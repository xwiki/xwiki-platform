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
package org.xwiki.wysiwyg.server.internal.plugin.macro;

import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.ParameterDescriptor;
import org.xwiki.wysiwyg.server.plugin.macro.MacroDescriptorTranslator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * XWiki specific implementation of {@link MacroDescriptorTranslator}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class XWikiMacroDescriptorTranslator implements MacroDescriptorTranslator
{
    /**
     * The name field.
     */
    private static final String FIELD_NAME = ".name";

    /**
     * The description field.
     */
    private static final String FIELD_DESCRIPTION = ".description";

    /**
     * The prefix used for all the translation keys.
     */
    private static final String KEY_RENDERING = "rendering";

    /** Execution context handler, needed for accessing the XWikiMessageTool. */
    @Inject
    private Execution execution;

    /**
     * The object used to lookup translation keys.
     */
    private XWikiMessageTool messageTool;

    @Override
    public MacroDescriptor translate(MacroDescriptor macroDescriptor)
    {
        // Update the message tool.
        messageTool = ((XWikiContext) execution.getContext().getProperty("xwikicontext")).getMessageTool();

        String macroKey = KEY_RENDERING + ".macro." + macroDescriptor.getId();
        macroDescriptor.setName(translate(macroKey + FIELD_NAME, macroDescriptor.getName()));
        macroDescriptor.setDescription(translate(macroKey + FIELD_DESCRIPTION, macroDescriptor.getDescription()));

        String macroCategoryKey = KEY_RENDERING + ".macroCategory." + macroDescriptor.getCategory();
        macroDescriptor.setCategory(translate(macroCategoryKey, macroDescriptor.getCategory()));

        ParameterDescriptor contentDescriptor = macroDescriptor.getContentDescriptor();
        if (contentDescriptor != null) {
            contentDescriptor.setName(translate(KEY_RENDERING + ".macroContent", contentDescriptor.getName()));
            contentDescriptor.setDescription(translate(macroKey + ".content.description", contentDescriptor
                .getDescription()));
        }

        for (ParameterDescriptor paramDescriptor : macroDescriptor.getParameterDescriptorMap().values()) {
            String paramKey = macroKey + ".parameter." + paramDescriptor.getId();
            paramDescriptor.setName(translate(paramKey + FIELD_NAME, paramDescriptor.getName()));
            paramDescriptor.setDescription(translate(paramKey + FIELD_DESCRIPTION, paramDescriptor.getDescription()));

            if (paramDescriptor.getType().isEnum()) {
                for (Entry<String, String> entry : paramDescriptor.getType().getEnumConstants().entrySet()) {
                    String paramValueKey = paramKey + ".value." + entry.getKey();
                    entry.setValue(translate(paramValueKey, entry.getValue()));
                }
            }
        }

        return macroDescriptor;
    }

    /**
     * Looks up the given translation key returning the specified default value if no value is found.
     * 
     * @param key the translation key to look up
     * @param defaultValue the value to return when there's no value associated with the given key
     * @return the value associated with the given key if there is one, otherwise the default value
     */
    private String translate(String key, String defaultValue)
    {
        String translation = messageTool.get(key);
        return key.equals(translation) ? defaultValue : translation;
    }
}
