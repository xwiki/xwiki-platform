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
package org.xwiki.security.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultXWikiBridge}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class DefaultXWikiBridgeTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @InjectMockComponents
    private DefaultXWikiBridge bridge;

    // Tests

    @Test
    public void toCompatibleEntityReferenceWhenNull()
    {
        assertNull(this.bridge.toCompatibleEntityReference(null));
    }

    @Test
    public void toCompatibleEntityReferenceWhenPartial()
    {
        EntityReference entityReference = new EntityReference("document", EntityType.DOCUMENT);
        DocumentReference documentReference = new DocumentReference("currentwiki", "currentspace", "document");

        when(this.currentResolver.resolve(entityReference, entityReference.getType())).thenReturn(documentReference);

        assertEquals(documentReference, this.bridge.toCompatibleEntityReference(entityReference));
    }

    @Test
    public void toCompatibleEntityReferenceWhenPage()
    {
        PageReference pageReference = new PageReference("wiki", "page");
        DocumentReference documentReference = new DocumentReference("wiki", "page", "WebHome");

        when(this.currentResolver.resolve(pageReference, pageReference.getType())).thenReturn(pageReference);
        doReturn(documentReference).when(this.oldcore.getSpyXWiki()).getDocumentReference(pageReference,
            this.oldcore.getXWikiContext());

        assertEquals(documentReference, this.bridge.toCompatibleEntityReference(pageReference));
    }
}
