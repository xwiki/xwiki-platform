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

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.6RC1
 * @since 9.11.6
 */
public class DefaultUntypedRecordableEventDescriptorTest
{
    private ContextualLocalizationManager contextualLocalizationManager;
    private NamespaceContextExecutor namespaceContextExecutor;
    private Logger logger;

    private EntityReference entityReference;
    private DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "User");
    private BaseObject baseObject;

    private DefaultUntypedRecordableEventDescriptor descriptor;

    @Before
    public void setUp() throws Exception
    {
        contextualLocalizationManager = mock(ContextualLocalizationManager.class);
        namespaceContextExecutor = mock(NamespaceContextExecutor.class);
        logger = mock(Logger.class);

        entityReference = new ObjectReference("someObject", new DocumentReference("someWiki", "someSpace",
                "somePage"));
        baseObject = mock(BaseObject.class);

        when(baseObject.getStringValue(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        when(baseObject.getListValue(anyString())).thenAnswer(
                invocationOnMock -> Arrays.asList((String) invocationOnMock.getArgument(0)));

        descriptor = new DefaultUntypedRecordableEventDescriptor(entityReference, authorReference, baseObject,
                contextualLocalizationManager, namespaceContextExecutor, logger);

        when(namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenAnswer(
                invocationOnMock -> String.format("Namespace [%s] -> [%s]", invocationOnMock.getArgument(0),
                        ((Callable) invocationOnMock.getArgument(1)).call())
        );
        when(contextualLocalizationManager.getTranslationPlain(anyString())).thenAnswer(
                invocationOnMock -> invocationOnMock.getArgument(0)
        );
    }

    @Test
    public void getEventType() throws Exception
    {
        assertEquals("eventType", descriptor.getEventType());
    }

    @Test
    public void getEventTypeIcon() throws Exception
    {
        assertEquals("eventTypeIcon", descriptor.getEventTypeIcon());
    }

    @Test
    public void getApplicationId() throws Exception
    {
        assertEquals("applicationId", descriptor.getApplicationId());
    }

    @Test
    public void getAuthorReference() throws Exception
    {
        assertEquals(authorReference, descriptor.getAuthorReference());
    }

    @Test
    public void getRoleHint() throws Exception
    {
        assertEquals("eventType", descriptor.getRoleHint());
    }

    @Test
    public void getValidationExpression() throws Exception
    {
        assertEquals("validationExpression", descriptor.getValidationExpression());
    }

    @Test
    public void getTargetExpression() throws Exception
    {
        assertEquals("target", descriptor.getTargetExpression());
    }

    @Test
    public void getEntityReference() throws Exception
    {
        assertEquals(entityReference, descriptor.getEntityReference());
    }

    @Test
    public void getEventTriggers() throws Exception
    {
        assertEquals(Arrays.asList("listenTo"), descriptor.getEventTriggers());
    }

    @Test
    public void getScope() throws Exception
    {
        assertEquals(WikiComponentScope.WIKI, descriptor.getScope());
    }

    @Test
    public void getObjectTypes() throws Exception
    {
        assertEquals(Arrays.asList("objectType"), descriptor.getObjectTypes());
    }

    @Test
    public void getApplicationIcon() throws Exception
    {
        assertEquals("applicationIcon", descriptor.getApplicationIcon());
    }

    @Test
    public void getDocumentReference() throws Exception
    {
        assertEquals("someWiki:someSpace.somePage", descriptor.getDocumentReference().toString());
    }

    @Test
    public void getApplicationName() throws Exception
    {
        assertEquals("Namespace [wiki:someWiki] -> [applicationName]", descriptor.getApplicationName());

        // With an exception
        Exception e = new Exception("Some error");
        when(namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenThrow(e);
        assertEquals("applicationName", descriptor.getApplicationName());
        verify(logger).warn("Failed to compute the correct localization with the correct namespace.", e);
    }

    @Test
    public void getDescription() throws Exception
    {
        assertEquals("Namespace [wiki:someWiki] -> [eventDescription]", descriptor.getDescription());

        // With an exception
        Exception e = new Exception("Some error");
        when(namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenThrow(e);
        assertEquals("eventDescription", descriptor.getDescription());
        verify(logger).warn("Failed to compute the correct localization with the correct namespace.", e);
    }
}
