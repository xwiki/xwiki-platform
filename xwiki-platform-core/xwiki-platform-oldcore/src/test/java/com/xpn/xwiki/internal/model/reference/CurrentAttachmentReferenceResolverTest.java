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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CurrentAttachmentReferenceResolver}.
 *
 * @version $Id$
 * @since 7.2M1
 */
public class CurrentAttachmentReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<CurrentAttachmentReferenceResolver> mocker =
            new MockitoComponentMockingRule<>(CurrentAttachmentReferenceResolver.class);
    
    private EntityReferenceResolver<EntityReference> entityReferenceResolver;
    
    @Before
    public void SetUp() throws Exception
    {
        entityReferenceResolver = mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
    }

    @Test
    public void resolveTest() throws Exception
    {
        // Mocks
        EntityReference entityReference = new EntityReference("hello.txt", EntityType.ATTACHMENT);
        
        AttachmentReference attachmentReference = new AttachmentReference("hello.txt", 
                new DocumentReference("Document", new SpaceReference("Space", new WikiReference("wiki"))));
        
        when(entityReferenceResolver.resolve(entityReference, EntityType.ATTACHMENT)).thenReturn(attachmentReference);
        
        // Test
        AttachmentReference result = mocker.getComponentUnderTest().resolve(entityReference);
        
        // Verify
        assertEquals(attachmentReference, result);
    }
}
