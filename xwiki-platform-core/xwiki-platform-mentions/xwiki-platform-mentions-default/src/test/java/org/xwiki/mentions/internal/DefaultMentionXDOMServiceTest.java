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
package org.xwiki.mentions.internal;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.MARKDOWN_1_1;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;
import static org.xwiki.test.LogLevel.WARN;

/**
 * Test of {@link DefaultMentionXDOMService}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
class DefaultMentionXDOMServiceTest
{
    @InjectMockComponents
    private DefaultMentionXDOMService xdomService;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(WARN);

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @Mock
    private ComponentManager componentManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private DocumentReference documentReferenceA;

    private DocumentReference documentReferenceB;

    private DocumentReference documentReferenceC;

    @BeforeEach
    void setup()
    {
        this.documentReferenceA = new DocumentReference("xwiki", "A", "A");
        this.documentReferenceB = new DocumentReference("ywiki", "B", "B");
        this.documentReferenceC = new DocumentReference("xwiki", "C", "C");
        when(this.documentReferenceResolver.resolve("A")).thenReturn(this.documentReferenceA);
        when(this.documentReferenceResolver.resolve("B")).thenReturn(this.documentReferenceB);
        when(this.documentReferenceResolver.resolve("C")).thenReturn(this.documentReferenceC);
        when(this.contextComponentManager.get()).thenReturn(this.componentManager);
    }

    @Test
    void listMentionMacros()
    {
        List<MacroBlock> actual = this.xdomService.listMentionMacros(new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new GroupBlock(singletonList(
                new MacroBlock("mention", new HashMap<>(), true)
            ))
        )))));
        assertEquals(1, actual.size());
        Assertions.assertEquals(new MacroBlock("mention", new HashMap<>(), true), actual.get(0));
    }

    @Test
    void countByIdentifierEmpty()
    {
        Map<DocumentReference, List<String>> actual = this.xdomService.countByIdentifier(emptyList());
        assertTrue(actual.isEmpty());
    }

    @Test
    void countByIdentifierOne()
    {
        Map<DocumentReference, List<String>> actual = this.xdomService.countByIdentifier(singletonList(
            initMentionMacro("A", "A1")
        ));
        HashMap<DocumentReference, List<String>> expected = new HashMap<>();
        expected.put(this.documentReferenceA, Collections.singletonList("A1"));
        assertEquals(expected, actual);
    }

    @Test
    void countByIdentifierTwo()
    {
        Map<DocumentReference, List<String>> actual = this.xdomService.countByIdentifier(asList(
            initMentionMacro("A", "A1"),
            initMentionMacro("A", "A2")
        ));
        HashMap<DocumentReference, List<String>> expected = new HashMap<>();
        expected.put(this.documentReferenceA, Arrays.asList("A1", "A2"));
        assertEquals(expected, actual);
    }

    @Test
    void countByIdentifierThree()
    {
        Map<DocumentReference, List<String>> actual = this.xdomService.countByIdentifier(asList(
            initMentionMacro("A", "A1"),
            initMentionMacro("B", "B1"),
            initMentionMacro("A", "A2")
        ));
        HashMap<DocumentReference, List<String>> expected = new HashMap<>();
        expected.put(this.documentReferenceB, Collections.singletonList("B1"));
        expected.put(this.documentReferenceA, Arrays.asList("A1", "A2"));
        assertEquals(expected, actual);
    }

    @Test
    void countByIdentifierWithEmptyValues()
    {
        Map<DocumentReference, List<String>> actual = this.xdomService.countByIdentifier(asList(
            initMentionMacro("A", null),
            initMentionMacro("A", "A1"),
            initMentionMacro("A", ""),
            initMentionMacro("B", "B1"),
            initMentionMacro("A", "A2"),
            initMentionMacro("C", "")
        ));
        HashMap<DocumentReference, List<String>> expected = new HashMap<>();
        expected.put(this.documentReferenceB, Collections.singletonList("B1"));
        expected.put(this.documentReferenceA, Arrays.asList(null, "A1", "", "A2"));
        expected.put(this.documentReferenceC, Collections.singletonList(""));
        assertEquals(expected, actual);
    }

    @Test
    void parse() throws Exception
    {

        Parser parser = mock(Parser.class);
        when(this.componentManager.getInstance(Parser.class, MARKDOWN_1_1.toIdString())).thenReturn(
            parser);

        XDOM xdom = new XDOM(emptyList());
        when(parser.parse(ArgumentMatchers.any(Reader.class))).thenReturn(xdom);

        Optional<XDOM> actual = this.xdomService.parse("ABC", MARKDOWN_1_1);

        assertEquals(Optional.of(xdom), actual);
    }

    @Test
    void parseError() throws Exception
    {
        Parser parser = mock(Parser.class);
        when(this.componentManager.getInstance(Parser.class, XWIKI_2_1.toIdString())).thenReturn(
            parser);
        when(parser.parse(ArgumentMatchers.any(Reader.class))).thenThrow(new ParseException(""));

        Optional<XDOM> actual = this.xdomService.parse("ABC", XWIKI_2_1);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to parse the payload [ABC]. Cause [ParseException: ].", this.logCapture.getMessage(0));

        assertEquals(Optional.empty(), actual);
    }

    private MacroBlock initMentionMacro(String reference, String anchor)
    {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("reference", reference);
        parameters.put("anchor", anchor);
        return new MacroBlock("mention", parameters, false);
    }
}