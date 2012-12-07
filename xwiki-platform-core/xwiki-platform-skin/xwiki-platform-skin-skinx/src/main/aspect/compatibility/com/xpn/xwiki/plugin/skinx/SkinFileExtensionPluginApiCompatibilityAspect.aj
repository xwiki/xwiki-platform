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
package com.xpn.xwiki.plugin.skinx;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.plugin.skinx.SkinFileExtensionPluginApi} class.
 * 
 * @version $Id$
 */
public aspect SkinFileExtensionPluginApiCompatibilityAspect
{
    /**
     * Compatibility mapping that associates a deprecated resource file to its new relative path
     */
    private static Map<String, String> compatibilityMap = new HashMap<String, String>();

    static {
        // fullScreen script has been moved and renamed in XWiki 2.6RC2 / 2.7M1 See XWIKI-5659
        compatibilityMap.put("js/xwiki/editors/fullScreenEdit.js", "uicomponents/widgets/fullScreen.js");
        compatibilityMap.put("js/xwiki/editors/fullScreenEdit.css", "uicomponents/widgets/fullScreen.css");
        // suggest script has been moved and renamed in XWiki 3.1M3 See XWIKI-6043
        compatibilityMap.put("js/xwiki/suggest/ajaxSuggest.js", "uicomponents/suggest/suggest.js");
        compatibilityMap.put("js/xwiki/suggest/ajaxSuggest.css", "uicomponents/suggest/suggest.css");
    }

    /**
     * Pointcut that catches all skin file extensions "use" join points.
     */
    private pointcut sfxUse(String path) : execution(void SkinFileExtensionPluginApi.use(..)) && args(path, ..);

    /**
     * Advice around skin file extensions "use" join points, that checks if the requested file is declared in the map of
     * deprecated extensions paths. If it is, it logs a warning message in the console, and calls the skin extension
     * plugin with the new path, declared in the map.
     */
    void around(String path): sfxUse(path)
    {
        if (compatibilityMap.containsKey(path)) {

            Logger logger = LoggerFactory.getLogger(thisJoinPoint.getSignature().getDeclaringType());
            logger.warn("Skin file extension with path [{}] is deprecated. Please use [{}] instead.", path,
                compatibilityMap.get(path));

            proceed(compatibilityMap.get(path));
        }

        proceed(path);
    }
}
