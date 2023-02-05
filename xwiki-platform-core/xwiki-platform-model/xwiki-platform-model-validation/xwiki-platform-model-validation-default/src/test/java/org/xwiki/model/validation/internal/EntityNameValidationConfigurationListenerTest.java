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
package org.xwiki.model.validation.internal;

import java.util.Arrays;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.event.filter.RegexEventFilter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link EntityNameValidationConfigurationListener}.
 *
 * @version $Id$
 * @since 13.4
 * @since 12.10.8
 */
@ComponentTest
class EntityNameValidationConfigurationListenerTest
{
    @InjectMockComponents
    private EntityNameValidationConfigurationListener listener;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Test
    void getEvents()
    {
        when(this.entityReferenceSerializer.serialize(EntityNameValidationConfigurationSource.CLASS_REFERENCE))
            .thenReturn("XWiki.EntityNameValidation.ConfigurationClass");
        when(this.entityReferenceSerializer.serialize(EntityNameValidationConfigurationSource.DOC_REFERENCE))
            .thenReturn("XWiki.EntityNameValidation.Configuration");
        RegexEventFilter classFilter =
            new RegexEventFilter(".*:" + Pattern.quote("XWiki.EntityNameValidation.ConfigurationClass"));
        RegexEventFilter objectFilter =
            new RegexEventFilter(".*:" + Pattern.quote("XWiki.EntityNameValidation.Configuration"));
        assertEquals(Arrays.asList(
            new DocumentCreatedEvent(classFilter),
            new DocumentUpdatedEvent(classFilter),
            new DocumentDeletedEvent(classFilter),
            new DocumentCreatedEvent(objectFilter),
            new DocumentUpdatedEvent(objectFilter),
            new DocumentDeletedEvent(objectFilter)
        ), this.listener.getEvents());
    }

    @Test
    void onEvent()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        EntityNameValidation entityNameValidationA = mock(EntityNameValidation.class);
        EntityNameValidation entityNameValidationB = mock(EntityNameValidation.class);

        when(xWikiDocument.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "Test"));
        when(this.entityNameValidationManager.getAvailableEntityNameValidationsComponents())
            .thenReturn(Arrays.asList(entityNameValidationA, entityNameValidationB));

        this.listener.onEvent(null, xWikiDocument, null);

        verify(entityNameValidationA).cleanConfigurationCache("xwiki");
        verify(entityNameValidationB).cleanConfigurationCache("xwiki");
    }
}
