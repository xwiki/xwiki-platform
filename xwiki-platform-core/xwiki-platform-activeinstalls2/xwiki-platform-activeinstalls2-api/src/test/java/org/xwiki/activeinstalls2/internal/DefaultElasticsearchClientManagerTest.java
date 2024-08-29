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
package org.xwiki.activeinstalls2.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultElasticsearchClientManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultElasticsearchClientManagerTest
{
    @InjectMockComponents
    private DefaultElasticsearchClientManager manager;

    @MockComponent
    private ActiveInstallsConfiguration configuration;

    @Test
    void initializeWhenPingURLIsEmpty() throws Exception
    {
        // Note: Simulate call to initialize() since we cannot use @BeforeComponent annotation as we want different
        // init behaviors for the different tests methods.
        when(this.configuration.getPingInstanceURL()).thenReturn("");
        this.manager.initialize();

        assertNull(this.manager.getClient());
    }

    @Test
    void initializeWhenPingURLIsNotEmpty() throws Exception
    {
        // Note: Simulate call to initialize() since we cannot use @BeforeComponent annotation as we want different
        // init behaviors for the different tests methods.
        when(this.configuration.getPingInstanceURL()).thenReturn("http://something");
        this.manager.initialize();

        assertNotNull(this.manager.getClient());
    }
}
