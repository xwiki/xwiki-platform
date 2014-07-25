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
package org.xwiki.wikistream.internal;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wikistream.internal.type.WikiStreamTypeConverter;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * Unit tests for {@link WikiStreamTypeConverter}.
 * 
 * @version $Id$
 */
public class WikiStreamTypeConverterTest
{
    @Rule
    public MockitoComponentMockingRule<Converter<WikiStreamType>> mocker =
        new MockitoComponentMockingRule<Converter<WikiStreamType>>(WikiStreamTypeConverter.class);

    @Test
    public void convertToTypeObject() throws Exception
    {
        WikiStreamType type =
            this.mocker.getComponentUnderTest().convert(WikiStreamType.class, WikiStreamType.XWIKI_XAR_11.serialize());
        Assert.assertEquals(WikiStreamType.XWIKI_XAR_11, type);
    }

    @Test
    public void convertToTypeObjectWhenNull() throws Exception
    {
        WikiStreamType type = this.mocker.getComponentUnderTest().convert(WikiStreamType.class, null);
        Assert.assertNull(type);
    }

    @Test
    public void convertToString() throws Exception
    {
        String typeId = this.mocker.getComponentUnderTest().convert(String.class, WikiStreamType.XWIKI_XAR_11);
        Assert.assertEquals(WikiStreamType.XWIKI_XAR_11.serialize(), typeId);
    }

    @Test
    public void convertToStringWhenNull() throws Exception
    {
        String typeId = this.mocker.getComponentUnderTest().convert(String.class, null);
        Assert.assertNull(typeId);
    }
}
