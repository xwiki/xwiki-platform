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
package org.xwiki.icon.internal;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IconThemeSkinActionURLParametersProvider}.
 *
 * @version $Id$
 */
@ComponentTest
class IconThemeSkinActionURLParametersProviderTest
{
    @InjectMockComponents
    private IconThemeSkinActionURLParametersProvider provider;

    @MockComponent
    private IconSetManager iconSetManager;

    @Test
    void getParametersWithCurrentIconSet() throws Exception
    {
        when(this.iconSetManager.getCurrentIconSet()).thenReturn(new IconSet("Silk"));

        assertEquals(Map.of("iconTheme", "Silk"), this.provider.getParameters());
    }

    @Test
    void getParametersWithDefaultIconSet() throws Exception
    {
        when(this.iconSetManager.getCurrentIconSet()).thenReturn(null);

        assertEquals(Map.of(), this.provider.getParameters());
    }

    @Test
    void getParametersWithIconSetWithoutName() throws Exception
    {
        when(this.iconSetManager.getCurrentIconSet()).thenReturn(new IconSet(null));

        assertEquals(Map.of(), this.provider.getParameters());
    }

    @Test
    void getParametersWhenError() throws Exception
    {
        IconException exception = new IconException("error");
        when(this.iconSetManager.getCurrentIconSet()).thenThrow(exception);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> this.provider.getParameters());
        assertSame(exception, thrown.getCause());
    }
}
