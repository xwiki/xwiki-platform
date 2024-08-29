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
package org.xwiki.user.internal.document;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test of {@link CurrentDocumentStringUserReferenceResolver}.
 *
 * @version $Id$
 * @since 1.8RC1
 */
@ComponentTest
class CurrentDocumentStringUserReferenceResolverTest
{
    @InjectMockComponents
    private CurrentDocumentStringUserReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @BeforeEach
    void setup()
    {
        when(this.entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(new WikiReference("wiki"));
        when(this.documentReferenceResolver.resolve(any(), eq(new EntityReference("XWiki", EntityType.SPACE))))
            .thenAnswer(invocationOnMock -> {
                String pageName = invocationOnMock.getArgument(0);
                String spaceName = "XWiki";
                String wikiName = "wiki";
                if (pageName.contains(":")) {
                    String[] split = pageName.split(":");
                    wikiName = split[0];
                    pageName = split[1];
                }
                if (pageName.contains(".")) {
                    String[] split = pageName.split("\\.");
                    spaceName = split[0];
                    pageName = split[1];
                }
                return new DocumentReference(wikiName, spaceName, pageName);
            });
    }

    @Test
    void resolveWithoutParameter()
    {
        UserReference reference = this.resolver.resolve("page");
        assertNotNull(reference);
        assertTrue(reference instanceof DocumentUserReference);
        assertEquals("wiki:XWiki.page", ((DocumentUserReference) reference).getReference().toString());
        assertTrue(reference.isGlobal());
    }

    @Test
    void resolveWithParameter()
    {
        when(this.documentReferenceResolver.resolve("page", new EntityReference("XWiki", EntityType.SPACE,
            new EntityReference("somewiki", EntityType.WIKI)))).thenReturn(
            new DocumentReference("somewiki", "XWiki", "page"));

        UserReference reference = this.resolver.resolve("page", new WikiReference("somewiki"));
        assertNotNull(reference);
        assertTrue(reference instanceof DocumentUserReference);
        assertEquals("somewiki:XWiki.page", ((DocumentUserReference) reference).getReference().toString());
        assertFalse(reference.isGlobal());
    }

    @Test
    void resolveGuest()
    {
        UserReference reference = this.resolver.resolve("XWikiGuest");
        assertSame(GuestUserReference.INSTANCE, reference);

        reference = this.resolver.resolve("xWiKiGuEsT");
        assertSame(GuestUserReference.INSTANCE, reference);
    }

    @Test
    void resolveSuperAdmin()
    {
        UserReference reference = this.resolver.resolve("superadmin");
        assertSame(SuperAdminUserReference.INSTANCE, reference);

        reference = this.resolver.resolve("sUpErAdMiN");
        assertSame(SuperAdminUserReference.INSTANCE, reference);
    }

    @Test
    void resolveWhenNull()
    {
        UserReference reference = this.resolver.resolve(null);
        assertSame(CurrentUserReference.INSTANCE, reference);
    }

    @Test
    void resolveWhenEmpty()
    {
        UserReference reference = this.resolver.resolve("");
        assertSame(CurrentUserReference.INSTANCE, reference);
    }
}
