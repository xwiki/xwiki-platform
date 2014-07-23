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
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconManagerException;
import org.xwiki.icon.IconSet;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.skinx.SkinExtension;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

@Component
@Singleton
public class DefaultIconManager implements IconManager
{
    private final static String DEFAULT_ICONSET_NAME = "default";

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

    @Inject
    @Named("ssx")
    private SkinExtension skinExtension;

    @Inject
    @Named("ssrx")
    private SkinExtension externalCSS;

    @Override
    public String render(String iconName) throws IconManagerException
    {
        IconSet iconSet = getCurrentIconSet();
        return render(iconSet, iconName, iconSet.getRenderWiki());
    }

    @Override
    public String renderHTML(String iconName) throws IconManagerException
    {
        IconSet iconSet = getCurrentIconSet();
        return render(iconSet, iconName, iconSet.getRenderWiki());
    }

    private String render(IconSet iconSet, String iconName, String renderer)
    {
        if (!StringUtils.isBlank(iconSet.getCss())) {
            activeCSS(iconSet);
        }
        if (!StringUtils.isBlank(iconSet.getSsx())) {
            activeSSX(iconSet);
        }
        return renderIcon(iconSet, iconName, renderer);
    }

    private void activeCSS(IconSet iconSet)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        String url = xwiki.parseContent(iconSet.getCss(), xcontext);
        externalCSS.use(url);
    }

    private void activeSSX(IconSet iconSet)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        String url = xwiki.parseContent(iconSet.getCss(), xcontext);
        skinExtension.use(url);
    }

    private String renderIcon(IconSet iconSet, String iconName, String renderer)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the icon
        Icon icon = iconSet.getIcon(iconName);

        // Interpret the velocity command
        StringWriter contentToParse = new StringWriter();
        contentToParse.write("#set($icon = \"");
        contentToParse.write(icon.getValue());
        contentToParse.write("\")\n");
        contentToParse.write(renderer);

        return xwiki.parseContent(contentToParse.toString(), xcontext);
    }

    private IconSet getCurrentIconSet() throws IconManagerException
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
        // Fallback
        if (iconSet == null) {
            // Get the default icon set
            iconSet = iconSetCache.get(DEFAULT_ICONSET_NAME);
            if (iconSet == null) {
                try {
                    // lazy loading
                    iconSet = iconSetLoader.loadIconSet(new InputStreamReader(
                        xwiki.getResourceAsStream("/resources/icons/default.iconset")), DEFAULT_ICONSET_NAME);
                    iconSetCache.put(DEFAULT_ICONSET_NAME, iconSet);
                } catch (IconManagerException | MalformedURLException e) {
                    throw new IconManagerException("Failed to get the current default icon set.", e);
                }
            }
        }

        return iconSet;
    }

}
