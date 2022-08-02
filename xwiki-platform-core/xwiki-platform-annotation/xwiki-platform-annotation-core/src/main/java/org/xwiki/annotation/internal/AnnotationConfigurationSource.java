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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Provides configuration from the Annotation Application's configuration document, for the current wiki.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("annotation")
@Singleton
public class AnnotationConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * The local reference of the class containing space preferences.
     */
    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(
        AnnotationConfiguration.CONFIGURATION_PAGE_SPACE_NAME, AnnotationConfiguration.CONFIGURATION_PAGE_NAME);

    @Override
    protected String getCacheId()
    {
        return "configuration.document.annotation";
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        return this.wikiManager.getCurrentWikiId();
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(CLASS_REFERENCE, getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CLASS_REFERENCE;
    }
}
