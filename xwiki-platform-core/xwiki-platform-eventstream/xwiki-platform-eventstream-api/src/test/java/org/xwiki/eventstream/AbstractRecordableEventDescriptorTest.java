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
package org.xwiki.eventstream;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AbstractRecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 10.6RC1
 * @since 10.5
 * @since 9.11.6
 */
@ComponentTest
public class AbstractRecordableEventDescriptorTest
{
    @InjectMockComponents
    private FakeRecordableEventDescriptor fakeRecordableEventDescriptor;

    @MockComponent(classToMock = NamespacedComponentManager.class)
    private ComponentManager componentManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private NamespaceContextExecutor namespaceContextExecutor;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getApplicationIcon() throws Exception
    {
        assertEquals("applicationIcon :)", fakeRecordableEventDescriptor.getApplicationIcon());
    }

    @Test
    void getEventType() throws Exception
    {
        assertEquals("fake", fakeRecordableEventDescriptor.getEventType());
    }

    @Test
    void getDescriptionAndApplicationTest() throws Exception
    {
        // Mocks
        when(namespaceContextExecutor.execute(eq(new WikiNamespace("subwiki")), any(Callable.class))).thenAnswer(
                invocationOnMock -> String.format("On namespace [%s]: %s",
                    invocationOnMock.getArgument(0),
                        ((Callable) invocationOnMock.getArgument(1)).call())
        );
        when(contextualLocalizationManager.getTranslationPlain("descriptionKey"))
                .thenReturn("My nice description");
        when(contextualLocalizationManager.getTranslationPlain("applicationKey"))
                .thenReturn("My nice application name");

//        // On main wiki
//        assertEquals("My nice description",
//                fakeRecordableEventDescriptor.getDescription());
//        assertEquals("My nice application name",
//                fakeRecordableEventDescriptor.getApplicationName());

        // On sub wiki
        when(((NamespacedComponentManager) componentManager).getNamespace())
            .thenReturn(new WikiNamespace("subwiki").toString());

        assertEquals("On namespace [wiki:subwiki]: My nice description",
                fakeRecordableEventDescriptor.getDescription());
        assertEquals("On namespace [wiki:subwiki]: My nice application name",
                fakeRecordableEventDescriptor.getApplicationName());
    }

    @Test
    public void getDescriptionAndApplicationWithExceptionTest() throws Exception
    {
        // Mocks
        Exception e = new Exception("some error");
        when(namespaceContextExecutor.execute(any(Namespace.class), any(Callable.class))).thenThrow(e);
        when(contextualLocalizationManager.getTranslationPlain("descriptionKey"))
                .thenReturn("My nice description");
        when(contextualLocalizationManager.getTranslationPlain("applicationKey"))
                .thenReturn("My nice application name");

        // On main wiki
        assertEquals("My nice description",
                fakeRecordableEventDescriptor.getDescription());
        assertEquals(0, this.logCapture.size());
        assertEquals("My nice application name",
                fakeRecordableEventDescriptor.getApplicationName());
        assertEquals(0, this.logCapture.size());

        // On sub wiki
        when(((NamespacedComponentManager) componentManager).getNamespace())
            .thenReturn(new WikiNamespace("subwiki").toString());

        assertEquals("My nice description",
                fakeRecordableEventDescriptor.getDescription());
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to render the translation key [descriptionKey] in the namespace [wiki:subwiki] "
                + "for the event descriptor of [fake].", this.logCapture.getMessage(0));
        assertEquals("My nice application name",
                fakeRecordableEventDescriptor.getApplicationName());
        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to render the translation key [applicationKey] in the namespace [wiki:subwiki] "
                + "for the event descriptor of [fake].", this.logCapture.getMessage(1));
    }

    private class OtherFakeRecordableEventDescriptor extends AbstractRecordableEventDescriptor
    {
        private String eventType;
        private String applicationId;

        public OtherFakeRecordableEventDescriptor(String applicationId, String eventType)
        {
            super(null, null);
            this.applicationId = applicationId;
            this.eventType = eventType;
        }

        @Override
        public String getApplicationId()
        {
            // For test reason we just reuse the same string
            return applicationId;
        }

        @Override
        public String getEventType()
        {
            return eventType;
        }

        @Override
        public String getApplicationIcon()
        {
            return null;
        }

        @Override
        public String getApplicationName()
        {
            return this.applicationTranslationKey;
        }
    }

    @Test
    public void equalsAndHashCodeTest()
    {
        OtherFakeRecordableEventDescriptor descriptor1 = new OtherFakeRecordableEventDescriptor(
                "app1",  "type1");
        OtherFakeRecordableEventDescriptor descriptor2 = new OtherFakeRecordableEventDescriptor(
                "app1", "type1");
        OtherFakeRecordableEventDescriptor descriptor3 = new OtherFakeRecordableEventDescriptor(
                "app2", "type1");
        OtherFakeRecordableEventDescriptor descriptor4 = new OtherFakeRecordableEventDescriptor(
                "app1", "type2");


        assertTrue(descriptor1.equals(descriptor1));
        assertTrue(descriptor1.equals(descriptor2));
        assertFalse(descriptor1.equals(descriptor3));
        assertFalse(descriptor1.equals(descriptor4));
        assertFalse(descriptor1.equals(new Object()));

        assertEquals(descriptor1.hashCode(), descriptor1.hashCode());
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
        assertNotEquals(descriptor1.hashCode(), descriptor3.hashCode());
        assertNotEquals(descriptor1.hashCode(), descriptor4.hashCode());
    }
}
