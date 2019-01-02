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
package org.xwiki.activeinstalls.internal.client.data;

import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Map;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.junit.Assert;

/**
 * Unit tests for {@link JavaPingDataProvider}.
 *
 * @version: $Id$
 */
public class JavaPingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<PingDataProvider> mocker =
        new MockitoComponentMockingRule<PingDataProvider>(JavaPingDataProvider.class);

    @Test
    public void testProvideMapping() throws Exception
    {
        JSONAssert.assertEquals("{\"javaVendor\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
            + "\"javaVersion\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
            + "\"javaSpecificationVersion\":{\"type\":\"string\",\"index\":\"not_analyzed\"}}",
            new JSONObject(this.mocker.getComponentUnderTest().provideMapping()), false);
    }

    @Test
    public void provideDataReportsJvmIdentity() throws Exception
    {
        Map<String, Object> result = this.mocker.getComponentUnderTest().provideData();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(System.getProperty("java.vendor"), result.get("javaVendor"));
        Assert.assertEquals(System.getProperty("java.version"), result.get("javaVersion"));
        Assert.assertEquals(System.getProperty("java.specification.version"), result.get("javaSpecificationVersion"));
    }
}
