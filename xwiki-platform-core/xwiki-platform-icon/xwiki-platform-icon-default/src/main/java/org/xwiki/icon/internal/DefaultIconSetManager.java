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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link org.xwiki.icon.IconSetManager}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultIconSetManager implements IconSetManager
{
    private static final String DEFAULT_ICONSET_NAME = "default";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private IconSetCache iconSetCache;

    @Inject
    private IconSetLoader iconSetLoader;

    @Inject
    private IconSetContext iconSetContext;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    @Named("all")
    private ConfigurationSource configurationSource;

    @Override
    public IconSet getCurrentIconSet() throws IconException
    {
        // Check the context
        IconSet iconSet = this.iconSetContext.getIconSet();
        if (iconSet != null) {
            return iconSet;
        }

        // Get the current icon theme
        String iconTheme = this.configurationSource.getProperty("iconTheme");

        // Get the icon set
        DocumentReference iconThemeDocRef = documentReferenceResolver.resolve(iconTheme);
        if (!StringUtils.isBlank(iconTheme) && documentAccessBridge.exists(iconThemeDocRef)) {
            iconSet = iconSetCache.get(iconThemeDocRef);
            if (iconSet == null) {
                // lazy loading
                iconSet = iconSetLoader.loadIconSet(iconThemeDocRef);
                iconSetCache.put(iconThemeDocRef, iconSet);
                iconSetCache.put(iconSet.getName(), wikiDescriptorManager.getCurrentWikiId(), iconSet);
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

    @Override
    public IconSet getIconSet(String name) throws IconException
    {
        // Special case: the default icon theme
        if (DEFAULT_ICONSET_NAME.equals(name)) {
            return getDefaultIconSet();
        }

        // Get the icon set from the cache
        IconSet iconSet = iconSetCache.get(name, wikiDescriptorManager.getCurrentWikiId());

        // Load it if it is not loaded yet
        if (iconSet == null) {
            try {
                // Search by name
                String xwql = "FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name";
                Query query = queryManager.createQuery(xwql, Query.XWQL);
                query.bindValue("name", name);
                List<String> results = query.execute();
                if (results.isEmpty()) {
                    return null;
                }

                // Get the first result
                String docName = results.get(0);
                DocumentReference docRef = documentReferenceResolver.resolve(docName);

                // Load the icon theme
                iconSet = iconSetLoader.loadIconSet(docRef);

                // Put it in the cache
                iconSetCache.put(docRef, iconSet);
                iconSetCache.put(name, wikiDescriptorManager.getCurrentWikiId(), iconSet);
            } catch (QueryException e) {
                throw new IconException(String.format("Failed to load the icon set [%s].", name), e);
            }
        }

        // Return the icon set
        return iconSet;
    }

    @Override
    public List<String> getIconSetNames() throws IconException
    {
        try {
            String xwql = "SELECT obj.name FROM Document doc, doc.object(IconThemesCode.IconThemeClass) obj "
                    + "ORDER BY obj.name";
            Query query = queryManager.createQuery(xwql, Query.XWQL);
            return query.execute();
        } catch (QueryException e) {
            throw new IconException("Failed to get the name of all icon sets.", e);
        }
    }
}
