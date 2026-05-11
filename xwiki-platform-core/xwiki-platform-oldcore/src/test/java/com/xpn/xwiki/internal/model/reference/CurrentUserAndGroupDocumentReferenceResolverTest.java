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
package com.xpn.xwiki.internal.model.reference;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CurrentUserAndGroupDocumentReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({ CurrentUserAndGroupEntityReferenceResolver.class, DefaultSymbolScheme.class })
class CurrentUserAndGroupDocumentReferenceResolverTest
{
    @InjectMockComponents
    private CurrentUserAndGroupDocumentReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private EntityReferenceProvider currentEntityReferenceProvider;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @BeforeEach
    void beforeEach()
    {
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.WIKI))
            .thenReturn(new WikiReference("currentwiki"));
    }

    @Test
    void resolver()
    {
        assertEquals(new DocumentReference("currentwiki", "XWiki", "Bosse"), this.resolver.resolve("Bosse"));
        assertEquals(new DocumentReference("currentwiki", "bossesSpace", "Bosse"), this.resolver
            .resolve("bossesSpace.Bosse"));
        assertEquals(new DocumentReference("bossesWiki", "XWiki", "Bosse"), this.resolver
            .resolve("Bosse", new WikiReference("bossesWiki")));
        assertEquals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse"), this.resolver
            .resolve("bossesSpace.Bosse", new WikiReference("bossesWiki")));
        assertEquals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse"), this.resolver
            .resolve("bossesWiki:bossesSpace.Bosse"));

        // If null is passed we expect no reference (i.e. the guest user).
        assertNull(this.resolver.resolve(null));
    }
}
