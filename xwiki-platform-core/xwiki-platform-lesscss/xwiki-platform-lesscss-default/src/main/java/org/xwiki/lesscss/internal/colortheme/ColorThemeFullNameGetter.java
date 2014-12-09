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
package org.xwiki.lesscss.internal.colortheme;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Returns the full name (wiki:Space.Page) of a color theme.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = ColorThemeFullNameGetter.class)
@Singleton
public class ColorThemeFullNameGetter
{
    private static final String DEFAULT_COLORTHEME = "default";

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @param colorTheme the name of a color theme
     * @return the full name of the color theme (or "default", if the color theme is "default)
     */
    public String getColorThemeFullName(String colorTheme)
    {
        if (colorTheme.equals(DEFAULT_COLORTHEME)) {
            return DEFAULT_COLORTHEME;
        }

        // Current Wiki Reference
        WikiReference currentWikiRef = new WikiReference(wikiDescriptorManager.getCurrentWikiId());
        // Get the full reference of the color theme
        DocumentReference colorThemeRef = documentReferenceResolver.resolve(colorTheme, currentWikiRef);
        // Return the serialized reference
        return entityReferenceSerializer.serialize(colorThemeRef);
    }
}
