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

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CurrentAttachmentReferenceResolver}.
 *
 * @version $Id$
 * @since 7.2M1
 */
@ComponentTest
class CurrentAttachmentReferenceResolverTest
{
    @InjectMockComponents
    private CurrentAttachmentReferenceResolver resolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @Test
    void resolveTest()
    {
        EntityReference entityReference = new EntityReference("hello.txt", EntityType.ATTACHMENT);

        AttachmentReference attachmentReference = new AttachmentReference("hello.txt",
                new DocumentReference("Document", new SpaceReference("Space", new WikiReference("wiki"))));

        when(this.entityReferenceResolver.resolve(entityReference, EntityType.ATTACHMENT)).thenReturn(attachmentReference);

        AttachmentReference result = this.resolver.resolve(entityReference);

        assertEquals(attachmentReference, result);
    }
}
