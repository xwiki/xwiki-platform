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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class ColorThemeFullnameGetterTest
{
    @Rule
    public MockitoComponentMockingRule<ColorThemeFullNameGetter> mocker =
            new MockitoComponentMockingRule<>(ColorThemeFullNameGetter.class);

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null,
                EntityReferenceSerializer.class, String.class));
    }

    @Test
    public void getColorThemeFullName() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference documentReference = new DocumentReference("wikiId", "ColorThemes", "Azure");
        when(documentReferenceResolver.resolve(eq("ColorThemes.Azure"), eq(new WikiReference("wikiId")))).
            thenReturn(documentReference);
        when(entityReferenceSerializer.serialize(documentReference)).thenReturn("wikiId:ColorThemes.Azure");

        // Tests
        assertEquals("default", mocker.getComponentUnderTest().getColorThemeFullName("default"));
        assertEquals("wikiId:ColorThemes.Azure",
            mocker.getComponentUnderTest().getColorThemeFullName("ColorThemes.Azure"));
    }


}

