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
package org.xwiki.lesscss.internal.cache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.LESSResourceReferenceSerializer;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class CacheKeySerializerTest
{
    @Rule
    public MockitoComponentMockingRule<CacheKeySerializer> mocker =
            new MockitoComponentMockingRule<>(CacheKeySerializer.class);

    private LESSResourceReferenceSerializer lessResourceReferenceSerializer;

    @Before
    public void setUp() throws Exception
    {
        lessResourceReferenceSerializer = mocker.getInstance(LESSResourceReferenceSerializer.class);
    }

    @Test
    public void serialize() throws Exception
    {
        CacheKey cacheKey = new CacheKey("skin", "colorTheme", new LESSSkinFileResourceReference("file"));
        when(lessResourceReferenceSerializer.serialize(eq(new LESSSkinFileResourceReference("file")))).
            thenReturn("FILE[file]");

        assertEquals("4skin_10colorTheme_10FILE[file]", mocker.getComponentUnderTest().serialize(cacheKey));

    }
}
