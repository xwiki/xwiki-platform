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
package org.xwiki.icon.internal;

import java.io.InputStreamReader;
import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.icon.IconSetManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link org.xwiki.icon.IconSetManager}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
public class DefaultIconSetManager implements IconSetManager
{
    private static final String DEFAULT_ICONSET_NAME = "default";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private IconSetCache iconSetCache;

    @Inject
    private IconSetLoader iconSetLoader;

    @Override
    public IconSet getCurrentIconSet() throws IconException
    {
        // Get the current icon theme
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Load the icon theme
        String iconTheme = xwiki.getXWikiPreference("iconTheme", xcontext);

        // Get the icon set
        IconSet iconSet = null;
        DocumentReference iconThemeDocRef = documentReferenceResolver.resolve(iconTheme);
        if (!StringUtils.isBlank(iconTheme) && documentAccessBridge.exists(iconThemeDocRef)) {
            iconSet = iconSetCache.get(iconThemeDocRef);
            if (iconSet == null) {
                // lazy loading
                iconSet = iconSetLoader.loadIconSet(iconThemeDocRef);
                iconSetCache.put(iconThemeDocRef, iconSet);
            }
        }

        return iconSet;
    }

    @Override
    public IconSet getDefaultIconSet() throws IconException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        IconSet iconSet = iconSetCache.get(DEFAULT_ICONSET_NAME);
        if (iconSet == null) {
            try {
                // lazy loading
                iconSet = iconSetLoader.loadIconSet(new InputStreamReader(
                        xwiki.getResourceAsStream("/resources/icons/default.iconset")), DEFAULT_ICONSET_NAME);
                iconSetCache.put(DEFAULT_ICONSET_NAME, iconSet);
            } catch (IconException | MalformedURLException e) {
                throw new IconException("Failed to get the current default icon set.", e);
            }
        }

        return iconSet;
    }
}
