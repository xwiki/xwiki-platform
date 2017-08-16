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

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.parser.DefaultContentParser}.
 *
 * @version $Id$
 * @since 6.0M2
 */
@ComponentList(ContextComponentManagerProvider.class)
public class DefaultContentParserTest
{
    @Rule
    public final MockitoComponentMockingRule<ContentParser> mocker =
        new MockitoComponentMockingRule<>(DefaultContentParser.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");
    private static final String SOURCE = "wiki:space.page";

    @Before
    public void configure() throws Exception
    {
        Parser parser = mocker.registerMockComponent(Parser.class, Syntax.PLAIN_1_0.toIdString());
        when(parser.parse(argThat(any(Reader.class)))).thenReturn(new XDOM(Collections.<Block>emptyList()));

        EntityReferenceSerializer<String> serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(DOCUMENT_REFERENCE)).thenReturn(SOURCE);
    }

    @Test
    public void parseHasNoMetadataSource() throws Exception
    {
        XDOM xdom = mocker.getComponentUnderTest().parse("", Syntax.PLAIN_1_0);

        assertThat(xdom.getMetaData().getMetaData(MetaData.SOURCE), nullValue());
    }

    @Test
    public void parseIsAddingMetadataSource() throws Exception
    {
        XDOM xdom = mocker.getComponentUnderTest().parse("", Syntax.PLAIN_1_0, DOCUMENT_REFERENCE);

        assertThat(xdom.getMetaData().getMetaData(MetaData.SOURCE), equalTo(SOURCE));
    }

    @Test
    public void parseWhenNoParser() throws Exception
    {
        thrown.expect(MissingParserException.class);
        thrown.expectMessage("Failed to find a parser for syntax [XWiki 2.1]");
        thrown.expectCause(any(ComponentLookupException.class));
        mocker.getComponentUnderTest().parse("", Syntax.XWIKI_2_1, DOCUMENT_REFERENCE);
    }

    @Test
    public void parseWhenNullSource() throws Exception
    {
        XDOM xdom = mocker.getComponentUnderTest().parse(null, Syntax.PLAIN_1_0);
        assertEquals(0, xdom.getChildren().size());
    }
}
