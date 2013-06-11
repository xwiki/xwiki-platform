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
package org.xwiki.model.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Unit tests for {@link BridgedObjectPropertyEntity}.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class BridgedObjectPropertyTest
{
    @Test
    public void getValueWhenDefined()
    {
        BaseProperty property = mock(BaseProperty.class);
        when(property.getValue()).thenReturn("test");

        BridgedObjectPropertyEntity bop = new BridgedObjectPropertyEntity(property, mock(XWikiContext.class));
        assertEquals("test", bop.getValue());
    }
}
