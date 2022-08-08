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
package org.xwiki.export.pdf.internal;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

/**
 * Default implementation of {@link RequiredSkinExtensionsRecorder}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultRequiredSkinExtensionsRecorder implements RequiredSkinExtensionsRecorder
{
    private static final List<String> SKIN_EXTENSION_PLUGINS =
        Arrays.asList("ssrx", "ssfx", "ssx", "linkx", "jsrx", "jsfx", "jsx");

    private final Map<String, String> requiredSkinExtensionsMap = new LinkedHashMap<>();

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void start()
    {
        this.requiredSkinExtensionsMap.clear();
        for (String pluginName : SKIN_EXTENSION_PLUGINS) {
            this.requiredSkinExtensionsMap.put(pluginName, getImportString(pluginName));
        }
    }

    @Override
    public String stop()
    {
        StringBuilder requiredSkinExtensions = new StringBuilder();
        for (Map.Entry<String, String> entry : this.requiredSkinExtensionsMap.entrySet()) {
            requiredSkinExtensions
                .append(StringUtils.removeStart(getImportString(entry.getKey()), entry.getValue()).trim());
        }
        return requiredSkinExtensions.toString();
    }

    @SuppressWarnings("deprecation")
    private String getImportString(String skinExtensionPluginName)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        Api pluginApi = xcontext.getWiki().getPluginApi(skinExtensionPluginName, xcontext);
        if (pluginApi instanceof SkinExtensionPluginApi) {
            return ((SkinExtensionPluginApi) pluginApi).getImportString();
        } else {
            return "";
        }
    }
}
