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
package org.xwiki.rendering.internal.macro.rss;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.properties.internal.DefaultBeanManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RssMacro}.
 * 
 * @version $Id$
 * @since 1.9
 */
@ComponentTest
@ComponentList({
    DefaultBeanManager.class
})
public class RssMacroTest
{
    @InjectMockComponents
    private RssMacro macro;

    @MockComponent
    private Execution execution;

    @MockComponent
    private ExecutionContext context;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainTextParser;

    @MockComponent
    private MacroContentParser contentParser;

    @BeforeEach
    public void setup()
    {
        when(execution.getContext()).thenReturn(context);
    }

    /**
     * Tests whether the macro throws the appropriate exception 
     * in cases where the required 'feed' parameter is missing.
     */
    @Test
    public void testRequiredParameterMissing() throws Exception
    {
        Throwable exception = assertThrows(MacroExecutionException.class, () -> {
            this.macro.execute(new RssMacroParameters(), null, null);
        });
        assertEquals("The required 'feed' parameter is missing", exception.getMessage());
    }

    /**
     * Tests the macro's behavior when the server hosting the feeds doesn't respond.
     */
    @Test
    public void testInvalidDocument() throws Exception
    {
        // Use a Mock SyndFeedInput to control what it returns for the test.
        Mockery mockery = new Mockery();
        final RomeFeedFactory mockFactory = mockery.mock(RomeFeedFactory.class);
        final RssMacroParameters parameters = new RssMacroParameters();
        MacroExecutionException expectedException = new MacroExecutionException("Error");
        mockery.checking(new Expectations() {{
            oneOf(mockFactory).createFeed(with(same(parameters))); will(throwException(expectedException));
        }});
        this.macro.setFeedFactory(mockFactory);

        // Dummy URL since a feed URL is mandatory
        parameters.setFeed("http://www.xwiki.org");

        Throwable exception = assertThrows(MacroExecutionException.class, () -> {
            this.macro.execute(parameters, null, null);
        });
        assertSame(expectedException, exception);
    }

    @Test
    public void execute() throws Exception
    {
        RssMacroParameters rssMacroParameters = new RssMacroParameters();
        rssMacroParameters.setContent(true);
        rssMacroParameters.setCount(4);
        rssMacroParameters.setImage(true);
        rssMacroParameters.setWidth("100");
        rssMacroParameters.setEncoding("UTF-8");
        rssMacroParameters.setDecoration(true);
        rssMacroParameters.setFeed("http://www.xwiki.org");

        MacroTransformationContext macroTransformationContext = mock(MacroTransformationContext.class);
        Mockery mockery = new Mockery();
        final RomeFeedFactory mockFactory = mockery.mock(RomeFeedFactory.class);
        File feedFile = new File("src/test/resources/feed1.xml");

        SyndFeed syndFeed = new SyndFeedInput().build(new XmlReader(new FileInputStream(feedFile), true,
            rssMacroParameters.getEncoding()));

        mockery.checking(new Expectations() {{
            oneOf(mockFactory).createFeed(with(same(rssMacroParameters))); will(returnValue(syndFeed));
        }});
        this.macro.setFeedFactory(mockFactory);
        when(context.getProperty("RssMacro.feed")).thenReturn(syndFeed);

        List<Block> expectedBlockList = Collections.singletonList(
            new GroupBlock(Collections.singletonList(new WordBlock("foo")))
        );
        when(plainTextParser.parse(any())).thenReturn(new XDOM(expectedBlockList));
        when(contentParser.parse("Some content", macroTransformationContext, false, false))
            .thenReturn(new XDOM(expectedBlockList));

        List<Block> obtainedContent = this.macro.execute(rssMacroParameters, "Some content", macroTransformationContext);
        assertEquals(1, obtainedContent.size());

        GroupBlock groupBlock = (GroupBlock) obtainedContent.get(0);
        // checking main parameters
        assertEquals("box rssfeed", groupBlock.getParameter("class"));
        assertEquals("width:100", groupBlock.getParameter("style"));

        // start checking children
        List<Block> children = groupBlock.getChildren();
        assertEquals(12, children.size());

        // checking created image
        assertTrue(children.get(0) instanceof ImageBlock);
        ImageBlock imageBlock = (ImageBlock) children.get(0);
        assertEquals("http://www.w3schools.com/images/logo.gif", imageBlock.getReference().getReference());

        assertTrue(children.get(1) instanceof NewLineBlock);

        // checking first paragraph and first group presenting channel description
        assertTrue(children.get(2) instanceof ParagraphBlock);
        ParagraphBlock paragraphBlock = (ParagraphBlock) children.get(2);
        assertEquals("rsschanneltitle", paragraphBlock.getParameter("class"));

        assertEquals(2, paragraphBlock.getChildren().size());
        assertTrue(paragraphBlock.getChildren().get(0) instanceof LinkBlock);
        LinkBlock firstLink = (LinkBlock) paragraphBlock.getChildren().get(0);
        assertEquals("http://liftoff.msfc.nasa.gov/", firstLink.getReference().getReference());
        assertEquals(ResourceType.URL, firstLink.getReference().getType());
        assertEquals(1, firstLink.getChildren().size());
        assertEquals(new WordBlock("foo"), firstLink.getChildren().get(0));

        assertTrue(paragraphBlock.getChildren().get(1) instanceof LinkBlock);
        LinkBlock secondLink = (LinkBlock) paragraphBlock.getChildren().get(1);
        assertEquals("http://liftoff.msfc.nasa.gov/", secondLink.getReference().getReference());
        assertEquals(ResourceType.URL, secondLink.getReference().getType());
        assertEquals(1, secondLink.getChildren().size());
        assertTrue(secondLink.getChildren().get(0) instanceof ImageBlock);

        assertEquals(expectedBlockList.get(0), children.get(3));

        // checking second paragraph presenting first item link
        assertTrue(children.get(4) instanceof ParagraphBlock);
        paragraphBlock = (ParagraphBlock) children.get(4);
        assertEquals("rssitemtitle", paragraphBlock.getParameter("class"));
        assertEquals(1, paragraphBlock.getChildren().size());
        assertTrue(paragraphBlock.getChildren().get(0) instanceof LinkBlock);
        firstLink = (LinkBlock) paragraphBlock.getChildren().get(0);
        assertEquals("http://liftoff.msfc.nasa.gov/news/2003/news-starcity.asp",
            firstLink.getReference().getReference());
        assertEquals(ResourceType.URL, firstLink.getReference().getType());
        assertEquals(1, firstLink.getChildren().size());
        assertEquals(new WordBlock("foo"), firstLink.getChildren().get(0));

        // checking second group block presenting first item description
        assertTrue(children.get(5) instanceof GroupBlock);
        groupBlock = (GroupBlock) children.get(5);
        assertEquals("rssitemdescription", groupBlock.getParameter("class"));
        assertEquals(1, groupBlock.getChildren().size());
        assertEquals(
            new RawBlock("How do Americans get ready to work with Russians aboard the International Space Station?",
            Syntax.XHTML_1_0), groupBlock.getChildren().get(0));
    }
}
