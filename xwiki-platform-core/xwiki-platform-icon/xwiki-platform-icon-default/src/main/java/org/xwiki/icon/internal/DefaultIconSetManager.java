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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
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

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

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

    @Inject
    private Logger logger;

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
        if (!StringUtils.isBlank(iconTheme) && exists(iconThemeDocRef)) {
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

    private boolean exists(DocumentReference iconThemeDocRef) throws IconException
    {
        try {
            return this.documentAccessBridge.exists(iconThemeDocRef);
        } catch (Exception e) {
            throw new IconException("Failed to check if the icon theme age exist", e);
        }
    }

    @Override
    public IconSet getDefaultIconSet() throws IconException
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        IconSet iconSet = iconSetCache.get(DEFAULT_ICONSET_NAME);
        if (iconSet == null) {
            // lazy loading
            try (InputStreamReader reader =
                new InputStreamReader(xwiki.getResourceAsStream("/resources/icons/default.iconset"))) {
                iconSet = iconSetLoader.loadIconSet(reader, DEFAULT_ICONSET_NAME);
                iconSetCache.put(DEFAULT_ICONSET_NAME, iconSet);
            } catch (IOException e) {
                throw new IconException("Failed to load the current default icon set resource.", e);
            } catch (IconException e) {
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
        IconSet iconSet = this.iconSetCache.get(name, this.wikiDescriptorManager.getCurrentWikiId());

        // Load it if it is not loaded yet
        if (iconSet == null) {
            List<String> results;

            try {
                // Search by name
                String xwql = "FROM doc.object(IconThemesCode.IconThemeClass) obj WHERE obj.name = :name";
                Query query = this.queryManager.createQuery(xwql, Query.XWQL);
                query.bindValue("name", name);
                results = query.execute();
            } catch (QueryException e) {
                throw new IconException(String.format("Failed to load the icon set [%s].", name), e);
            }

            iconSet = loadIconSetFromCandidateDocuments(name, results);
        }

        // Return the icon set
        return iconSet;
    }

    private IconSet loadIconSetFromCandidateDocuments(String name, List<String> candidateDocuments) throws IconException
    {
        List<IconException> iconExceptions = new ArrayList<>();
        IconSet iconSet = null;

        // Try all results to find the first one that loads successfully.
        for (String docName : candidateDocuments) {
            DocumentReference docRef = this.documentReferenceResolver.resolve(docName);

            try {
                // Load the icon theme
                iconSet = this.iconSetLoader.loadIconSet(docRef);

                // Put it in the cache
                this.iconSetCache.put(docRef, iconSet);
                this.iconSetCache.put(name, this.wikiDescriptorManager.getCurrentWikiId(), iconSet);

                break;
            } catch (IconException e) {
                // Store the exception first, maybe there is another icon theme with the same name that loads
                // successfully.
                iconExceptions.add(e);
            }
        }

        if (iconSet == null && !iconExceptions.isEmpty()) {
            if (iconExceptions.size() > 1) {
                iconExceptions.stream().skip(1)
                    .forEach(e -> this.logger.warn("Failed loading icon set [{}] from multiple matching "
                            + "documents, ignored this additional exception, reason: [{}].", name,
                        getRootCauseMessage(e)));
                throw new IconException(String.format("Failed to load the icon set [%s] from %d documents, "
                        + "reporting the first exception, see the log for additional errors.",
                    name, candidateDocuments.size()),
                    iconExceptions.get(0));
            } else {
                throw iconExceptions.get(0);
            }
        }

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
