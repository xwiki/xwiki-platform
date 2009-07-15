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

import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Validate {@link EnumConverter} component.
 * 
 * @version $Id: $
 */
public class EnumConverterTest extends AbstractXWikiComponentTestCase
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
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.enumConverter = getComponentManager().lookup(Converter.class, "enum");
    }

    public void testConvertValid()
    {
        assertEquals(EnumTest.VALUE1, this.enumConverter.convert(EnumTest.class, "VALUE1"));
    }

    public void testConvertIgnireCase()
    {
        assertEquals(EnumTest.VALUE1, this.enumConverter.convert(EnumTest.class, "value1"));
    }

    public void testConvertInvalid()
    {
        try {
            assertEquals(null, this.enumConverter.convert(EnumTest.class, "notexistingvalue"));
            fail("Should have thrown a ConversionException exception");
        } catch (ConversionException expected) {
            // expected
        }
    }

    public void testConvertNull()
    {
        assertEquals(null, this.enumConverter.convert(EnumTest.class, null));
    }
}
