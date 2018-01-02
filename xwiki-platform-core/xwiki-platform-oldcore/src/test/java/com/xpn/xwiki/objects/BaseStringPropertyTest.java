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
package com.xpn.xwiki.objects;

import org.junit.Assert;
import org.junit.Test;

import com.xpn.xwiki.objects.BaseStringProperty;

/**
 * Unit tests for the {@link BaseStringProperty} class.
 *
 * @version $Id$
 */
public class BaseStringPropertyTest
{

    @Test
    public void testHashCode()
    {
        final String value = "test value";

        BaseStringProperty p1 = new BaseStringProperty();
        BaseStringProperty p2 = new BaseStringProperty();

        p1.setValue(value);
        p2.setValue(value);

        Assert.assertEquals(p1.hashCode(), p2.hashCode());
    }
}
