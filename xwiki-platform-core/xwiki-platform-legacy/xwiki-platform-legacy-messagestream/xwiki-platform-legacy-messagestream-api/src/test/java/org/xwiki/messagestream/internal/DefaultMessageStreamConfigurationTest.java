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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of the default implementation of {@link org.xwiki.messagestream.MessageStreamConfiguration}.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@ComponentTest
class DefaultMessageStreamConfigurationTest
{
    @InjectMockComponents
    private DefaultMessageStreamConfiguration defaultMessageStream;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void isActive()
    {
        DocumentReference configClass1 = new DocumentReference("wiki1", "XWiki", "MessageStreamConfig");
        Mockito.when(this.documentAccessBridge.getProperty(configClass1, configClass1, "active")).thenReturn(1);

        DocumentReference configClass2 = new DocumentReference("wiki2", "XWiki", "MessageStreamConfig");
        Mockito.when(this.documentAccessBridge.getProperty(configClass2, configClass2, "active")).thenReturn(0);

        DocumentReference configClass3 = new DocumentReference("wiki3", "XWiki", "MessageStreamConfig");
        Mockito.when(this.documentAccessBridge.getProperty(configClass3, configClass3, "active")).thenReturn(null);

        assertTrue(this.defaultMessageStream.isActive("wiki1"));
        assertFalse(this.defaultMessageStream.isActive("wiki2"));
        assertFalse(this.defaultMessageStream.isActive("wiki3"));
    }
}
