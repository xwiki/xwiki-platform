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
package org.xwiki.properties.internal.converter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Validate {@link ConvertConverter} component.
 * 
 * @version $Id$
 */
public class ConvertUtilsConverterTest extends AbstractComponentTestCase
{
    private Converter convertUtilsConverter;

    public Integer[] field;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.convertUtilsConverter = getComponentManager().lookup(Converter.class);
    }

    @Test
    public void testConvert() throws SecurityException, NoSuchFieldException
    {
        Assert.assertEquals(Integer.valueOf(42), this.convertUtilsConverter.convert(Integer.class, "42"));
    }

    @Test
    public void testConvertArrays() throws SecurityException, NoSuchFieldException
    {
        Assert.assertArrayEquals(new int[] {1, 2, 3},
            this.convertUtilsConverter.<int[]> convert(int[].class, "1, 2, 3"));

        Assert.assertArrayEquals(new Integer[] {1, 2, 3},
            this.convertUtilsConverter.<Integer[]> convert(Integer[].class, "1, 2, 3"));

        Assert.assertArrayEquals(new Integer[] {1, 2, 3}, this.convertUtilsConverter.<Integer[]> convert(
            ConvertUtilsConverterTest.class.getField("field").getGenericType(), "1, 2, 3"));
    }
}
