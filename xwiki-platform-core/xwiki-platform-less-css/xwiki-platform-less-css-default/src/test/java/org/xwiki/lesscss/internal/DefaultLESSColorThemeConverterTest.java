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
package org.xwiki.lesscss.internal;

import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.LESSSkinFileCompiler;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link org.xwiki.lesscss.internal.DefaultLESSColorThemeConverter}.
 *
 * @since 6.1M2
 * @version $Id$
 */
public class DefaultLESSColorThemeConverterTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSColorThemeConverter> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSColorThemeConverter.class);

    private LESSSkinFileCompiler lessSkinFileCompiler;

    @Before
    public void setUp() throws Exception
    {
        lessSkinFileCompiler = mocker.getInstance(LESSSkinFileCompiler.class);
    }

    @Test
    public void getColorThemeFromCSS() throws Exception
    {
        StringWriter string = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("/bigStyle.css"), string);

        Map<String, String> results = mocker.getComponentUnderTest().getColorThemeFromCSS(string.toString());
        assertEquals("#e8e8e8", results.get("borderColor"));
    }
}
