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
package org.xwiki.notifications.filters.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 10.5RC1
 * @since 10.4
 * @since 9.11.5
 */
public class CachedModelBridgeTest
{
    @Rule
    public final MockitoComponentMockingRule<CachedModelBridge> mocker =
            new MockitoComponentMockingRule<>(CachedModelBridge.class);

    private ModelBridge modelBridge;
    private Execution execution;
    private EntityReferenceSerializer<String> serializer;
    private ExecutionContext executionContext;
    private Map<String, Object> executionContextProperties;

    @Before
    public void setUp() throws Exception
    {
        modelBridge = mocker.getInstance(ModelBridge.class);
        execution = mocker.getInstance(Execution.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        executionContextProperties = new HashMap<>();
        when(executionContext.getProperties()).thenReturn(executionContextProperties);
        executionContextProperties.put("property1", new Object());
        executionContextProperties.put("userAllNotificationFilterPreferences_property2", new Object());
        executionContextProperties.put("userToggleableFilterPreference_property3", new Object());
        doAnswer(invocationOnMock -> {
            String property = invocationOnMock.getArgument(0);
            executionContextProperties.remove(property);
            return null;
        }).when(executionContext).removeProperty(anyString());

    }

    private void verifyClearCache() throws Exception
    {
        assertTrue(executionContextProperties.containsKey("property1"));
        assertFalse(executionContextProperties.containsKey("userAllNotificationFilterPreferences_property2"));
        assertFalse(executionContextProperties.containsKey("userToggleableFilterPreference_property3"));
    }

    @Test
    public void setStartDateForUser() throws Exception
    {
        DocumentReference user = new DocumentReference("x", "W", "iki");
        Date date = new Date();
        mocker.getComponentUnderTest().setStartDateForUser(user, date);

        verify(modelBridge).setStartDateForUser(eq(user), eq(date));
        verifyClearCache();
    }
}
