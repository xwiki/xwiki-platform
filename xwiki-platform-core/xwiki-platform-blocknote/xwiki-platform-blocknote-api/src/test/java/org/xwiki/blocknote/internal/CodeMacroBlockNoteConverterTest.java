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
package org.xwiki.blocknote.internal;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CodeMacroBlockNoteConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class CodeMacroBlockNoteConverterTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @InjectMockComponents
    private CodeMacroBlockNoteConverter converter;

    private static ObjectNode json(String json) throws JsonProcessingException
    {
        return (ObjectNode) OBJECT_MAPPER.readTree(json.replace('\'', '"'));
    }

    @Test
    void getBlockType()
    {
        assertEquals("codeBlock", this.converter.getBlockType());
    }

    @Test
    void toBlock() throws Exception
    {
        assertEquals(Optional.of(json("""
            {'type': 'codeBlock', 'props': {'language': 'java', 'xwikiParameters': {'layout': 'linenumbers'}},
                'content': 'int a = 1;\\nint b = 2;'}""")), this.converter.toBlock(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code',
                'parameters': {'layout': 'linenumbers', 'language': 'java'}, 'content': 'int a = 1;\\nint b = 2;'},
                'output': []}}""")));
    }

    @Test
    void toBlockWithDefaultLanguageAndWithoutContent() throws Exception
    {
        // The default language is kept as a parameter because the code block doesn't save it.
        assertEquals(Optional.of(json("""
            {'type': 'codeBlock', 'props': {'xwikiParameters': {'Language': 'text'}}, 'content': ''}""")),
            this.converter.toBlock(json("""
                {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code', 'parameters': {'Language': 'text'}},
                    'output': []}}""")));
    }

    @Test
    void toBlockWithoutParameters() throws Exception
    {
        assertEquals(Optional.of(json("{'type': 'codeBlock', 'props': {}, 'content': 'test'}")),
            this.converter.toBlock(json("""
                {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code', 'parameters': {}, 'content': 'test'},
                    'output': []}}""")));
    }

    @Test
    void toBlockWithSource() throws Exception
    {
        assertEquals(Optional.empty(), this.converter.toBlock(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code',
                'parameters': {'Source': 'attachment:test.java'}}, 'output': []}}""")));
    }

    @Test
    void toBlockWithInlineMacro() throws Exception
    {
        assertEquals(Optional.empty(), this.converter.toBlock(json("""
            {'type': 'xwikiInlineMacro', 'props': {'call': {'name': 'code', 'parameters': {}, 'content': 'test'},
                'output': []}}""")));
    }

    @Test
    void toBlockWithEditableContent() throws Exception
    {
        assertEquals(Optional.empty(), this.converter.toBlock(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code', 'parameters': {},
                'content': [{'type': 'paragraph', 'content': 'test'}]}, 'output': []}}""")));
    }

    @Test
    void toMacro() throws Exception
    {
        // The language selected in the code block overwrites the language parameter.
        assertEquals(Optional.of(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code',
                'parameters': {'layout': 'linenumbers', 'language': 'java'},
                'content': 'int a = 1;\\nint b = 2;'}}}""")), this.converter.toMacro(json("""
                {'type': 'codeBlock', 'props': {'language': 'java',
                    'xwikiParameters': {'Language': 'text', 'layout': 'linenumbers'}},
                    'content': [{'type': 'text', 'text': 'int a = 1;', 'styles': {}}, '\\nint b = 2;']}""")));
    }

    @Test
    void toMacroWithDefaultLanguage() throws Exception
    {
        assertEquals(Optional.of(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code', 'parameters': {}, 'content': 'test'}}}""")),
            this.converter.toMacro(json("{'type': 'codeBlock', 'props': {'language': 'text'}, 'content': 'test'}")));

        assertEquals(Optional.of(json("""
            {'type': 'xwikiMacroBlock', 'props': {'call': {'name': 'code', 'parameters': {'language': 'text'},
                'content': ''}}}""")), this.converter.toMacro(json("""
                {'type': 'codeBlock', 'props': {'xwikiParameters': {'language': 'text'}}}""")));
    }

    @Test
    void toMacroWithVerbatim() throws Exception
    {
        assertEquals(Optional.empty(), this.converter.toMacro(json("""
            {'type': 'codeBlock', 'props': {'xwikiVerbatim': true}, 'content': 'test'}""")));
    }
}
