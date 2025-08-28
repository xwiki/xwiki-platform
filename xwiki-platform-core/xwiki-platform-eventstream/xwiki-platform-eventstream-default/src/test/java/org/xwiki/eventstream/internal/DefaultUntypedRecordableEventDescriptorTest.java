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
package org.xwiki.eventstream.internal;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockitoComponentManagerExtension;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.6RC1
 * @since 9.11.6
 */
@Extensions({ @ExtendWith({ MockitoComponentManagerExtension.class }) })
class DefaultUntypedRecordableEventDescriptorTest
{
    @Mock
    private NamespaceContextExecutor namespaceContextExecutor;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private static final EntityReference ENTITY_REFERENCE = new ObjectReference("someObject", new DocumentReference(
        "someWiki", "someSpace",
        "somePage"));

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private DefaultUntypedRecordableEventDescriptor descriptor;

    @BeforeEach
    void setUp() throws Exception
    {
        ContextualLocalizationManager contextualLocalizationManager = mock(ContextualLocalizationManager.class);

        BaseObject baseObject = mock(BaseObject.class);

        when(baseObject.getStringValue(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        when(baseObject.getListValue(anyString())).thenAnswer(
            invocationOnMock -> List.of((String) invocationOnMock.getArgument(0)));

        this.descriptor =
            new DefaultUntypedRecordableEventDescriptor(ENTITY_REFERENCE, AUTHOR_REFERENCE, baseObject,
                contextualLocalizationManager, this.namespaceContextExecutor);

        when(this.namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenAnswer(
            invocationOnMock -> String.format("Namespace [%s] -> [%s]", invocationOnMock.getArgument(0),
                ((Callable) invocationOnMock.getArgument(1)).call())
        );
        when(contextualLocalizationManager.getTranslationPlain(anyString())).thenAnswer(
            invocationOnMock -> invocationOnMock.getArgument(0)
        );
    }

    @Test
    void getEventType()
    {
        assertEquals("eventType", this.descriptor.getEventType());
    }

    @Test
    void getEventTypeIcon()
    {
        assertEquals("eventTypeIcon", this.descriptor.getEventTypeIcon());
    }

    @Test
    void getApplicationId()
    {
        assertEquals("applicationId", this.descriptor.getApplicationId());
    }

    @Test
    void getAuthorReference()
    {
        assertEquals(AUTHOR_REFERENCE, this.descriptor.getAuthorReference());
    }

    @Test
    void getRoleHint()
    {
        assertEquals("eventType", this.descriptor.getRoleHint());
    }

    @Test
    void getValidationExpression()
    {
        assertEquals("validationExpression", this.descriptor.getValidationExpression());
    }

    @Test
    void getTargetExpression()
    {
        assertEquals("target", this.descriptor.getTargetExpression());
    }

    @Test
    void getEntityReference()
    {
        assertEquals(ENTITY_REFERENCE, this.descriptor.getEntityReference());
    }

    @Test
    void getEventTriggers()
    {
        assertEquals(List.of("listenTo"), this.descriptor.getEventTriggers());
    }

    @Test
    void getScope()
    {
        assertEquals(WikiComponentScope.WIKI, this.descriptor.getScope());
    }

    @Test
    void getObjectTypes()
    {
        assertEquals(List.of("objectType"), this.descriptor.getObjectTypes());
    }

    @Test
    void getApplicationIcon()
    {
        assertEquals("applicationIcon", this.descriptor.getApplicationIcon());
    }

    @Test
    void getDocumentReference()
    {
        assertEquals("someWiki:someSpace.somePage", this.descriptor.getDocumentReference().toString());
    }

    @Test
    void getApplicationName() throws Exception
    {
        assertEquals("Namespace [wiki:someWiki] -> [applicationName]", this.descriptor.getApplicationName());

        // With an exception
        when(this.namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class)))
            .thenThrow(new Exception("Some error"));
        assertEquals("applicationName", this.descriptor.getApplicationName());
        assertEquals("Failed to render the translation key [applicationName] in the namespace "
            + "[wiki:someWiki] for the event descriptor of [eventType].", this.logCapture.getMessage(0));
    }

    @Test
    void getDescription() throws Exception
    {
        assertEquals("Namespace [wiki:someWiki] -> [eventDescription]", this.descriptor.getDescription());

        // With an exception
        Exception e = new Exception("Some error");
        when(this.namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenThrow(e);
        assertEquals("eventDescription", this.descriptor.getDescription());
        assertEquals("Failed to render the translation key [eventDescription] in the namespace "
            + "[wiki:someWiki] for the event descriptor of [eventType].", this.logCapture.getMessage(0));
    }
}
