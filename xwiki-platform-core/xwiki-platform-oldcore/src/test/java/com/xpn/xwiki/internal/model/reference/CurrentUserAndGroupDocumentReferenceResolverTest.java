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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CurrentUserAndGroupDocumentReferenceResolver}.
 * 
 * @version $Id$
 */
@AllComponents
public class CurrentUserAndGroupDocumentReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentReferenceResolver<String>> mocker =
        new MockitoComponentMockingRule<DocumentReferenceResolver<String>>(
            CurrentUserAndGroupDocumentReferenceResolver.class);

    @AfterComponent
    public void afterComponent() throws Exception
    {
        EntityReferenceProvider provider = this.mocker.registerMockComponent(EntityReferenceProvider.class, "current");

        when(provider.getDefaultReference(EntityType.WIKI)).thenReturn(new WikiReference("currentwiki"));
    }

    @Test
    public void testResolver() throws ComponentLookupException
    {
        assertEquals(new DocumentReference("currentwiki", "XWiki", "Bosse"), this.mocker.getComponentUnderTest()
            .resolve("Bosse"));
        assertEquals(new DocumentReference("currentwiki", "bossesSpace", "Bosse"), this.mocker.getComponentUnderTest()
            .resolve("bossesSpace.Bosse"));
        assertEquals(new DocumentReference("bossesWiki", "XWiki", "Bosse"), this.mocker.getComponentUnderTest()
            .resolve("Bosse", new WikiReference("bossesWiki")));
        assertEquals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse"), this.mocker.getComponentUnderTest()
            .resolve("bossesSpace.Bosse", new WikiReference("bossesWiki")));
        assertEquals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse"), this.mocker.getComponentUnderTest()
            .resolve("bossesWiki:bossesSpace.Bosse"));

        // If null is passed we expect no reference (i.e. the guest user).
        assertNull(this.mocker.getComponentUnderTest().resolve(null));
    }
}
