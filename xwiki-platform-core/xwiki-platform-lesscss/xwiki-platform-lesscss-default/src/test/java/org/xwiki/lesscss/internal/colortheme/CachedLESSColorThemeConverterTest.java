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
package org.xwiki.lesscss.internal.colortheme;

import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.IntegratedLESSCompiler;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.CachedLESSColorThemeConverter}.
 *
 * @since 6.4M2
 * @version $Id$
 */
public class CachedLESSColorThemeConverterTest
{
    @Rule
    public MockitoComponentMockingRule<CachedLESSColorThemeConverter> mocker =
            new MockitoComponentMockingRule<>(CachedLESSColorThemeConverter.class);

    private IntegratedLESSCompiler lessCompiler;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(IntegratedLESSCompiler.class);
    }

    @Test
    public void compute() throws Exception
    {
        StringWriter string = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("/bigStyle.css"), string);
        when(lessCompiler.compile(any(LESSResourceReference.class), eq(false), eq(false), eq("skin"), eq(false))).
            thenReturn(string.toString());

        Map<String, String> results = mocker.getComponentUnderTest().compute(
            new LESSSkinFileResourceReference("file"), false, false, true, "skin");
        assertEquals("#e8e8e8", results.get("borderColor"));
    }
}
