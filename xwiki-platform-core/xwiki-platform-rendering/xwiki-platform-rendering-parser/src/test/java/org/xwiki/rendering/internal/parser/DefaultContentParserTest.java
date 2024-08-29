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
package org.xwiki.rendering.internal.parser;

import java.io.Reader;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Unit tests for {@link DefaultContentParser}.
 *
 * @version $Id$
 * @since 6.0M2
 */
@ComponentTest
@ComponentList(ContextComponentManagerProvider.class)
class DefaultContentParserTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final String SOURCE = "wiki:space.page";

    @InjectMockComponents
    private DefaultContentParser defaultContentParser;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    private Parser plain10parser;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.plain10parser = componentManager.registerMockComponent(Parser.class, PLAIN_1_0.toIdString());
        when(this.plain10parser.parse(argThat(CoreMatchers.any(Reader.class)))).thenReturn(new XDOM(emptyList()));
        when(this.serializer.serialize(DOCUMENT_REFERENCE)).thenReturn(SOURCE);
    }

    @Test
    void parseHasNoMetadataSource() throws Exception
    {
        XDOM xdom = this.defaultContentParser.parse("", PLAIN_1_0);

        assertThat(xdom.getMetaData().getMetaData(MetaData.SOURCE), nullValue());
    }

    @Test
    void parseIsAddingMetadataSource() throws Exception
    {
        XDOM xdom = this.defaultContentParser.parse("", PLAIN_1_0, DOCUMENT_REFERENCE);

        assertThat(xdom.getMetaData().getMetaData(MetaData.SOURCE), equalTo(SOURCE));
    }

    @Test
    void parseWhenNoParser()
    {
        MissingParserException missingParserException = assertThrows(MissingParserException.class,
            () -> this.defaultContentParser.parse("", XWIKI_2_1, DOCUMENT_REFERENCE));
        assertEquals(ComponentLookupException.class, missingParserException.getCause().getClass());
        assertEquals("Failed to find a parser for syntax [XWiki 2.1]", missingParserException.getMessage());
    }

    @Test
    void parseWhenNoParserFail() throws Exception
    {
        when(this.plain10parser.parse(any())).thenThrow(StackOverflowError.class);

        ParseException parseErrorException =
            assertThrows(ParseException.class, () -> this.defaultContentParser.parse("content", PLAIN_1_0));

        assertEquals(StackOverflowError.class, parseErrorException.getCause().getClass());
        assertEquals("Failed to parse with syntax [plain/1.0].", parseErrorException.getMessage());
    }

    @Test
    void parseWhenNoParserFailWithSource() throws Exception
    {
        when(this.plain10parser.parse(any())).thenThrow(StackOverflowError.class);

        ParseException parseErrorException =
            assertThrows(ParseException.class, () -> this.defaultContentParser.parse("content", PLAIN_1_0, null));

        assertEquals(StackOverflowError.class, parseErrorException.getCause().getClass());
        assertEquals("Failed to parse with syntax [plain/1.0].", parseErrorException.getMessage());
    }

    @Test
    void parseWhenNullSource() throws Exception
    {
        XDOM xdom = this.defaultContentParser.parse(null, Syntax.PLAIN_1_0);
        assertEquals(0, xdom.getChildren().size());
    }
}
