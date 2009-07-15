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
package org.xwiki.properties.internal;

import java.awt.Color;

import org.xwiki.properties.ConverterManager;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Validate {@link DefaultConverterManager}.
 * 
 * @version $Id$
 */
public class DefaultConverterManagerTest extends AbstractXWikiComponentTestCase
{
    public ConverterManager defaultConverterManager;

    public enum TestEnum
    {
        ENUMVALUE
    }

    @Override
    public void setUp() throws Exception
    {
        this.defaultConverterManager = getComponentManager().lookup(ConverterManager.class);
    }

    public void testConvert()
    {
        assertEquals(Integer.valueOf(42), this.defaultConverterManager.convert(Integer.class, "42"));
    }

    public void testConvertEnum()
    {
        assertEquals(TestEnum.ENUMVALUE, this.defaultConverterManager.convert(TestEnum.class, "ENUMVALUE"));
    }

    public void testConvertColor()
    {
        assertEquals(Color.WHITE, this.defaultConverterManager.convert(Color.class, "#ffffff"));
    }
}
