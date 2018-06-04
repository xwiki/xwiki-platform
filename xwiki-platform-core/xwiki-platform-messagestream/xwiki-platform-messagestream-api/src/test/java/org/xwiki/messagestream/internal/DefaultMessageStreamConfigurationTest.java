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
package org.xwiki.messagestream.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Default implementation of {@link org.xwiki.messagestream.MessageStreamConfiguration}.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
public class DefaultMessageStreamConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMessageStreamConfiguration> mocker =
            new MockitoComponentMockingRule<>(DefaultMessageStreamConfiguration.class);

    private DocumentAccessBridge documentAccessBridge;

    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
    }

    @Test
    public void isActive() throws Exception
    {
        DocumentReference configClass1 = new DocumentReference("wiki1", "XWiki", "MessageStreamConfig");
        Mockito.when(documentAccessBridge.getProperty(configClass1, configClass1, "active")).thenReturn(1);

        DocumentReference configClass2 = new DocumentReference("wiki2", "XWiki", "MessageStreamConfig");
        Mockito.when(documentAccessBridge.getProperty(configClass2, configClass2, "active")).thenReturn(0);

        DocumentReference configClass3 = new DocumentReference("wiki3", "XWiki", "MessageStreamConfig");
        Mockito.when(documentAccessBridge.getProperty(configClass3, configClass3, "active")).thenReturn(null);

        assertTrue(mocker.getComponentUnderTest().isActive("wiki1"));
        assertFalse(mocker.getComponentUnderTest().isActive("wiki2"));
        assertFalse(mocker.getComponentUnderTest().isActive("wiki3"));
    }
}
