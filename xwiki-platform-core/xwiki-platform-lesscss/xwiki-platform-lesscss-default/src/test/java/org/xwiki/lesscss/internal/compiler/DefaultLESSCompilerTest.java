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
package org.xwiki.lesscss.internal.compiler;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link DefaultLESSCompiler}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
public class DefaultLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSCompiler.class);

    @Test
    public void compile() throws Exception
    {
        // Get an example
        StringWriter source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style.less").getFile()), source);

        // Compile
        String result = mocker.getComponentUnderTest().compile(source.toString());

        // Get the expected result
        StringWriter expectedResult = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style.css").getFile()), expectedResult);

        // Compare
        assertEquals(expectedResult.toString(), result);
    }

    @Test
    public void compileWithImports() throws Exception
    {
        // Get an example
        StringWriter source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/styleWithImports.less").getFile()), source);

        // Compile
        Path[] paths = { Paths.get(getClass().getResource("/").getPath())};
        String result = mocker.getComponentUnderTest().compile(source.toString(), paths);

        // Get the expected result
        StringWriter expectedResult = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/styleWithImports.css").getFile()),
                expectedResult);

        // Compare
        assertEquals(expectedResult.toString(), result);
    }
}
