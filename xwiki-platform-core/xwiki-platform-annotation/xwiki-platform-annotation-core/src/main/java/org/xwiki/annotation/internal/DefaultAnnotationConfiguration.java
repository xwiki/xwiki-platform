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
package org.xwiki.annotation.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Default implementation using as configuration source the configuration document on the current wiki.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultAnnotationConfiguration implements AnnotationConfiguration
{
    /**
     * Annotation configuration source that is read from the configuration document on the current wiki.
     */
    @Inject
    @Named("annotation")
    protected Provider<ConfigurationSource> configuration;

    /**
     * Resolver used to read the string values from the configuration as document references.
     */
    @Inject
    @Named("currentmixed")
    protected DocumentReferenceResolver<String> resolver;

    /**
     * @see #getCurrentWikiReference()
     */
    @Inject
    protected ModelContext modelContext;

    @Inject
    @Named("current")
    protected EntityReferenceProvider entityReferenceProvider;

    /**
     * @see #isInstalled()
     */
    @Inject
    protected DocumentAccessBridge dab;

    @Inject
    private Logger logger;

    @Override
    public boolean isInstalled()
    {
        try {
            return dab.exists(new DocumentReference(getCurrentWikiReference().getName(),
                AnnotationConfiguration.CONFIGURATION_PAGE_SPACE_NAME,
                AnnotationConfiguration.CONFIGURATION_PAGE_NAME));
        } catch (Exception e) {
            this.logger.error("Failed to test if the annotation configuration page exists", e);
        }

        return false;
    }

    @Override
    public boolean isActivated()
    {
        return configuration.get().getProperty("activated", 0) == 1;
    }

    @Override
    public List<SpaceReference> getExceptionSpaces()
    {
        List<String> exceptionSpaces = configuration.get().getProperty("exceptionSpaces", List.class);

        List<SpaceReference> result = new ArrayList<>();
        for (String exceptionSpace : exceptionSpaces) {
            result.add(new SpaceReference(exceptionSpace, getCurrentWikiReference()));
        }

        return result;
    }

    @Override
    public boolean isDisplayedByDefault()
    {
        return configuration.get().getProperty("displayed", 0) == 1;
    }

    @Override
    public boolean isDisplayedHighlightedByDefault()
    {
        return configuration.get().getProperty("displayHighlight", 0) == 1;
    }

    @Override
    public DocumentReference getAnnotationClassReference()
    {
        String annotationClassName = configuration.get().getProperty("annotationClass", "XWiki.XWikiComments");

        return resolver.resolve(annotationClassName);
    }

    /**
     * @return the reference pointing to the current wiki
     */
    protected WikiReference getCurrentWikiReference()
    {
        EntityReference wikiEntityReference = this.entityReferenceProvider.getDefaultReference(EntityType.WIKI);

        return wikiEntityReference instanceof WikiReference wikiReference ? wikiReference
            : new WikiReference(wikiEntityReference);
    }
}
