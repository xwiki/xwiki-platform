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
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Validate {@link EnumConverter} component.
 * 
 * @version $Id$
 */
public class EnumConverterTest extends AbstractComponentTestCase
{
    public Converter enumConverter;

    public enum EnumTest
    {
        VALUE1,
        Value2
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.enumConverter = getComponentManager().lookup(Converter.class, "enum");
    }

    @Test
    public void testConvertValid()
    {
        Assert.assertEquals(EnumTest.VALUE1, this.enumConverter.convert(EnumTest.class, "VALUE1"));
    }

    @Test
    public void testConvertIgnireCase()
    {
        Assert.assertEquals(EnumTest.VALUE1, this.enumConverter.convert(EnumTest.class, "value1"));
    }

    @Test
    public void testConvertInvalid()
    {
        try {
            Assert.assertEquals(null, this.enumConverter.convert(EnumTest.class, "notexistingvalue"));
            Assert.fail("Should have thrown a ConversionException exception");
        } catch (ConversionException expected) {
            // expected
        }
    }

    @Test
    public void testConvertNull()
    {
        Assert.assertEquals(null, this.enumConverter.convert(EnumTest.class, null));
    }
}
