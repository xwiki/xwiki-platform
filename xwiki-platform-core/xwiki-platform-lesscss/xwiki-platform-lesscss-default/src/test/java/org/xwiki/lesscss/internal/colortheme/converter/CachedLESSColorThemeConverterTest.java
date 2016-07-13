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
package org.xwiki.lesscss.internal.colortheme.converter;

import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.core.DefaultLessCompiler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.colortheme.converter.CachedLESSColorThemeConverter}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
public class CachedLESSColorThemeConverterTest
{
    @Rule
    public MockitoComponentMockingRule<CachedLESSColorThemeConverter> mocker =
            new MockitoComponentMockingRule<>(CachedLESSColorThemeConverter.class);

    private LESSCompiler lessCompiler;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
    }

    @Test
    public void compute() throws Exception
    {
        StringWriter lessSource = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("/styleWithColorTheme.less"), lessSource);
        
        // To have a better test, we use Less4j to generate the CSS that contains the color theme mapping
        LessCompiler less4jCompiler = new DefaultLessCompiler();
        LessCompiler.Configuration options = new LessCompiler.Configuration();
        options.setCompressing(true);
        LessCompiler.CompilationResult lessResult = less4jCompiler.compile(lessSource.toString(), options);        
        when(lessCompiler.compile(any(LESSResourceReference.class), eq(false), eq(false), eq("skin"), eq(false))).
            thenReturn(lessResult.getCss());

        // So now we can test the converter on the less4j output
        Map<String, String> results = mocker.getComponentUnderTest().compute(
            new LESSSkinFileResourceReference("file", null, null), false, false, true, "skin");
        assertEquals("#E8E8E8", results.get("borderColor"));
        assertEquals("#3e444c", results.get("highlightColor"));
    }
}
