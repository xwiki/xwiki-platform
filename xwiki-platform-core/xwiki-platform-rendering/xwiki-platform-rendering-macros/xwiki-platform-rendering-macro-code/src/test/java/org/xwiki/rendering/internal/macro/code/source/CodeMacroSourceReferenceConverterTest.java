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
package org.xwiki.rendering.internal.macro.code.source;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CodeMacroSourceReferenceConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(ContextComponentManagerProvider.class)
class CodeMacroSourceReferenceConverterTest
{
    @InjectMockComponents
    private CodeMacroSourceReferenceConverter converter;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    public static class MyType
    {
        @Override
        public String toString()
        {
            return "MyType";
        }
    }

    @Test
    void convertToType()
    {
        assertNull(this.converter.convert(CodeMacroSourceReference.class, null));
    }

    @Test
    void convertFromString()
    {
        assertEquals(new CodeMacroSourceReference("string", "string"),
            this.converter.convert(CodeMacroSourceReference.class, "string"));
        assertEquals(new CodeMacroSourceReference("string", ""),
            this.converter.convert(CodeMacroSourceReference.class, ""));
        assertEquals(new CodeMacroSourceReference("string", "reference"),
            this.converter.convert(CodeMacroSourceReference.class, "string:reference"));
    }

    @Test
    void convertFromReader()
    {
        assertEquals(new CodeMacroSourceReference("string", "reference"),
            this.converter.convert(CodeMacroSourceReference.class, new StringReader("reference")));
    }

    @Test
    void convertFromBytes()
    {
        assertEquals(new CodeMacroSourceReference("string", "reference"),
            this.converter.convert(CodeMacroSourceReference.class, "reference".getBytes()));
    }

    @Test
    void convertFromInputStream()
    {
        assertEquals(new CodeMacroSourceReference("string", "reference"),
            this.converter.convert(CodeMacroSourceReference.class, new ByteArrayInputStream("reference".getBytes())));
    }

    @Test
    void convertFromURL() throws MalformedURLException
    {
        assertEquals(new CodeMacroSourceReference("url", "http://url"),
            this.converter.convert(CodeMacroSourceReference.class, new URL("http://url")));
    }

    @Test
    void convertFromFile()
    {
        assertEquals(new CodeMacroSourceReference("file", "/path"),
            this.converter.convert(CodeMacroSourceReference.class, new File("/path")));
    }

    @Test
    void convertFromUnknownType() throws Exception
    {
        assertEquals(new CodeMacroSourceReference("string", "MyType"),
            this.converter.convert(CodeMacroSourceReference.class, new MyType()));
    }

    @Test
    void convertFromCustomType() throws Exception
    {
        org.xwiki.rendering.macro.code.source.CodeMacroSourceReferenceConverter myconverter =
            this.componentManager.registerMockComponent(TypeUtils.parameterize(
                org.xwiki.rendering.macro.code.source.CodeMacroSourceReferenceConverter.class, MyType.class));

        MyType myvalue = new MyType();
        CodeMacroSourceReference myReference = new CodeMacroSourceReference("mytype", "value2");

        when(myconverter.convert(myvalue)).thenReturn(myReference);

        assertEquals(myReference, this.converter.convert(CodeMacroSourceReference.class, myvalue));
    }
}
