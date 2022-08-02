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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentDocumentReferenceUserReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentDocumentReferenceUserReferenceResolverTest
{
    @InjectMockComponents
    private DocumentDocumentReferenceUserReferenceResolver resolver;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @MockComponent
    private EntityReferenceFactory entityReferenceFactory;

    @BeforeEach
    void setup()
    {
        when(this.entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(new WikiReference("wiki"));
        when(this.entityReferenceFactory.getReference(any())).then(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @Test
    void resolve()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        UserReference reference = this.resolver.resolve(documentReference);
        assertNotNull(reference);
        assertTrue(reference instanceof DocumentUserReference);
        assertEquals("wiki:space.page", ((DocumentUserReference) reference).getReference().toString());
        assertTrue(reference.isGlobal());
        verify(this.entityReferenceFactory).getReference(documentReference);
    }

    @Test
    void resolveGuest()
    {
        UserReference reference = this.resolver.resolve(new DocumentReference("wiki", "space", "XWikiGuest"));
        assertSame(GuestUserReference.INSTANCE, reference);

        reference = this.resolver.resolve(new DocumentReference("wiki", "space", "xWiKiGuEsT"));
        assertSame(GuestUserReference.INSTANCE, reference);
    }

    @Test
    void resolveSuperAdmin()
    {
        UserReference reference = this.resolver.resolve(new DocumentReference("wiki", "space", "superadmin"));
        assertSame(SuperAdminUserReference.INSTANCE, reference);

        reference = this.resolver.resolve(new DocumentReference("wiki", "space", "sUpErAdMiN"));
        assertSame(SuperAdminUserReference.INSTANCE, reference);
    }

    @Test
    void resolveWhenNull()
    {
        UserReference reference = this.resolver.resolve(null);
        assertSame(GuestUserReference.INSTANCE, reference);
    }
}
