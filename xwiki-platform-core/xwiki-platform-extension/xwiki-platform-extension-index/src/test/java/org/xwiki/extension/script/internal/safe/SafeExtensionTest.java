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
package org.xwiki.extension.script.internal.safe;

import org.junit.Test;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.script.internal.safe.ScriptSafeProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SafeExtension}.
 * 
 * @version $Id$
 */
public class SafeExtensionTest
{
    private InstalledExtension extension = mock(InstalledExtension.class);

    @SuppressWarnings("unchecked")
    private ScriptSafeProvider<Object> safeProvider = mock(ScriptSafeProvider.class);

    private SafeExtension<InstalledExtension> safeExtension = new SafeExtension<>(extension, safeProvider);

    @Test
    public void getPropertyWithDefaultValue()
    {
        when(this.extension.getProperty("foo", 23)).thenReturn(16);
        when(this.safeProvider.get(16)).thenReturn(61);
        assertEquals((Object) 61, safeExtension.getProperty("foo", 23));
    }
}
