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
package org.xwiki.configuration.internal;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultConfigurationSource}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class DefaultConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultConfigurationSource> mocker =
        new MockitoComponentMockingRule<>(DefaultConfigurationSource.class);

    @Test
    public void containsKey() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.containsKey("key")).thenReturn(false);

        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(xwikiPropertiesSource.containsKey("key")).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().containsKey("key"));

        // Verify that the order call is correct
        InOrder inOrder = inOrder(xwikiPropertiesSource, documentsSource);
        // First call
        inOrder.verify(documentsSource).containsKey("key");
        // Second call
        inOrder.verify(xwikiPropertiesSource).containsKey("key");
    }
}
