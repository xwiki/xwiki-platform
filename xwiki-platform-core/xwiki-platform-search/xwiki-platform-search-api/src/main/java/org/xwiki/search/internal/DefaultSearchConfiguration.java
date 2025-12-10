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
package org.xwiki.search.internal;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.search.SearchConfiguration;

/**
 * Default implementation of {@link SearchConfiguration}.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component
@Singleton
public class DefaultSearchConfiguration implements SearchConfiguration
{
    private static final String XWIKI_PROPERTIES_PREFIX = "search.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    @Named("search")
    private ConfigurationSource configDocument;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    public String getEngine()
    {
        return getProperty("engine", String.class, "database");
    }

    @Override
    public Collection<EntityReference> getExclusions()
    {
        @SuppressWarnings("unchecked")
        List<String> exclusions = getProperty("exclusions", List.class, List.of());
        String defaultDocumentName =
            this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        // We currently support specifying only document references in the exclusions. We could extend this to support
        // any entity reference but we'd have to specify the entity type as well.
        return exclusions.stream().map(this.stringDocumentReferenceResolver::resolve).map(documentReference -> {
            // For nested documents we exclude their children as well (i.e the whole space).
            if (documentReference.getName().equals(defaultDocumentName)) {
                return documentReference.getLastSpaceReference();
            }
            return documentReference;
        }).toList();
    }

    private <T> T getProperty(String key, Class<T> valueClass, T defaultValue)
    {
        if (this.configDocument.containsKey(key)) {
            return this.configDocument.getProperty(key, valueClass, defaultValue);
        } else {
            return this.xwikiProperties.getProperty(XWIKI_PROPERTIES_PREFIX + key, valueClass, defaultValue);
        }
    }
}
