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
package org.xwiki.livedata.internal.livetable;

import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.internal.StringLiveDataConfigurationResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultLiveDataConfigurationProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(StringLiveDataConfigurationResolver.class)
class DefaultLiveDataConfigurationProviderTest
{
    @InjectMockComponents
    private DefaultLiveDataConfigurationProvider provider;

    @MockComponent
    private IconManager iconManager;

    @Test
    void get()
    {
        LiveDataConfiguration config = this.provider.get();
        assertEquals("liveTable", config.getQuery().getSource().getId());
    }
}
