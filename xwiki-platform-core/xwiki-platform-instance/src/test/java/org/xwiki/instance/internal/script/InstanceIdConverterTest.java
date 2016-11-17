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
package org.xwiki.instance.internal.script;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link InstanceIdConverter}.
 *
 * @version $Id$
 */
public class InstanceIdConverterTest
{
    @Rule
    public MockitoComponentMockingRule<InstanceIdConverter> mocker =
        new MockitoComponentMockingRule<>(InstanceIdConverter.class);

    @Test
    public void convertFromString() throws Exception
    {
        InstanceId id =
            this.mocker.getComponentUnderTest().convertToType(InstanceId.class, "b6ad6165-daaf-41a1-8a3f-9aa81451c402");
        assertEquals("b6ad6165-daaf-41a1-8a3f-9aa81451c402", id.getInstanceId());
    }

    @Test
    public void convertToString() throws Exception
    {
        InstanceId id = new InstanceId("b6ad6165-daaf-41a1-8a3f-9aa81451c402");
        assertEquals("b6ad6165-daaf-41a1-8a3f-9aa81451c402", this.mocker.getComponentUnderTest().convertToString(id));
    }
}
