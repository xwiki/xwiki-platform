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
package org.xwiki.image.style.internal.configuration.source;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Current wiki global configuration.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
@Named("image.style.wiki.current")
public class CurrentWikiImageStyleConfigurationSource extends AbstractXClassConfigurationSource
{
    private static final List<String> IMAGE_STYLE_SPACE = List.of("Image", "Style", "Code");

    private static final LocalDocumentReference XOBJECT_REFERENCE =
        new LocalDocumentReference(IMAGE_STYLE_SPACE, "Configuration");

    static final LocalDocumentReference XCLASS_REFERENCE =
        new LocalDocumentReference(IMAGE_STYLE_SPACE, "ConfigurationClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return XCLASS_REFERENCE;
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        return this.wikiManager.getCurrentWikiId();
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(XOBJECT_REFERENCE, getCurrentWikiReference());
    }

    @Override
    protected String getCacheId()
    {
        return "configuration.image.style.wiki.current";
    }
}
