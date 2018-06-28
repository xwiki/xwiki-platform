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
package org.xwiki.model.internal.reference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.test.TestConstants;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the deprecated {@link DefaultDocumentReferenceProvider}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultDocumentReferenceProviderTest implements TestConstants
{
    @MockComponent
    private EntityReferenceProvider entityProvider;

    @MockComponent
    private Provider<SpaceReference> spaceProvider;

    @InjectMockComponents
    private DefaultDocumentReferenceProvider provider;

    @BeforeEach
    public void beforeEach()
    {
        when(this.entityProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(DEFAULT_DOCUMENT_REFERENCE);
        when(this.spaceProvider.get())
            .thenReturn(new SpaceReference(DEFAULT_SPACE_REFERENCE.appendParent(DEFAULT_WIKI_REFERENCE)));
    }

    @Test
    public void testGetDefaultValue()
    {
        assertEquals(
            DEFAULT_DOCUMENT_REFERENCE.appendParent(DEFAULT_SPACE_REFERENCE.appendParent(DEFAULT_WIKI_REFERENCE)),
            this.provider.get());
    }
}
