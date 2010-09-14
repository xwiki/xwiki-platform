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
package org.xwiki.gwt.wysiwyg.client.plugin.image.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONSerializer;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageMetaDataExtractor;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link InsertImageExecutable}.
 * 
 * @version $Id$
 */
public class InsertImageExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private InsertImageExecutable executable;

    /**
     * The URL of a blank image that can be used in tests that require a real image.
     */
    private String blankImageURL;

    /**
     * The object used to extract the image meta data.
     */
    private final ImageMetaDataExtractor metaDataExtractor = new ImageMetaDataExtractor();

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        executable = new InsertImageExecutable(rta);
        blankImageURL = GWT.getModuleBaseURL() + "clear.cache.gif";
    }

    /**
     * Unit test for {@link InsertImageExecutable#isExecuted()}.
     */
    public void testIsExecuted()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIsExecuted();
            }
        });
    }

    /**
     * Unit test for {@link InsertImageExecutable#isExecuted()}.
     */
    private void doTestIsExecuted()
    {
        rta.setHTML("<p>before<img/>after</p>");

        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild());
        select(range);

        assertEquals("before", rta.getDocument().getSelection().toString());
        assertFalse(executable.isExecuted());

        range.selectNode(getBody().getFirstChild().getChild(1));
        select(range);

        assertTrue(executable.isExecuted());

        range.setStart(getBody().getFirstChild().getFirstChild(), 2);
        range.setEnd(getBody().getFirstChild().getLastChild(), 3);
        select(range);

        assertEquals("foreaft", rta.getDocument().getSelection().toString());
        assertFalse(executable.isExecuted());
    }

    /**
     * Unit test for {@link InsertImageExecutable#getParameter()}.
     */
    public void testGetParameter()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestGetParameter();
            }
        });
    }

    /**
     * Unit test for {@link InsertImageExecutable#getParameter()}.
     */
    private void doTestGetParameter()
    {
        rta.setHTML("<p>abc<img src=\"http://www.xwiki.org/missing.png\" "
            + "alt=\"A missing image.\" width=\"70\" height=\"35%\" "
            + "metadata=\"<!--startimage:Space.Page@missing.png-->" + Element.INNER_HTML_PLACEHOLDER
            + "<!--stopimage-->\" style=\"float: right; border:1px solid black;\"/>xyz</p>");

        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild());
        select(range);

        assertEquals("abc", rta.getDocument().getSelection().toString());
        assertNull(executable.getParameter());

        range.selectNode(getBody().getFirstChild().getChild(1));
        select(range);

        assertEquals("{reference:\"Space.Page@missing.png\",url:\"http://www.xwiki.org/missing.png\",width:\"70\","
            + "height:\"35%\",alttext:\"A missing image.\",alignment:\"RIGHT\"}", executable.getParameter());

        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getLastChild(), 0);
        select(range);

        assertEquals("bc", rta.getDocument().getSelection().toString());
        assertNull(executable.getParameter());
    }

    /**
     * Unit test for {@link InsertImageExecutable#execute(String)} when there's no image selected.
     */
    public void testInsertImage()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestInsertImage();
            }
        });
    }

    /**
     * Unit test for {@link InsertImageExecutable#execute(String)} when there's no image selected.
     */
    private void doTestInsertImage()
    {
        rta.setHTML("<p>123</p>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getFirstChild(), 2);
        select(range);

        String imageJSON =
            "{reference:\"Main.Test@missing.png\",url:\"http://www.xwiki.org/missing.png?width=70\","
                + "width:\"70\",height:\"35%\",alttext:\"A missing image.\",alignment:\"CENTER\"}";
        rta.getDocument().addInnerHTMLListener(metaDataExtractor);
        assertTrue(executable.execute(imageJSON));
        rta.getDocument().removeInnerHTMLListener(metaDataExtractor);
        assertEquals(imageJSON, executable.getParameter());
    }

    /**
     * Unit test for {@link InsertImageExecutable#execute(String)} when an image is selected.
     */
    public void testEditImage()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEditImage();
            }
        });
    }

    /**
     * Unit test for {@link InsertImageExecutable#execute(String)} when an image is selected.
     */
    private void doTestEditImage()
    {
        rta.setHTML("<p><img id=\"test\" class=\"photo\" " + "style=\"vertical-align:bottom; margin-top: 12px;\" "
            + "src=\"http://www.xwiki.org/photo.png\" title=\"A nice photo.\" width=\"100\" height=\"40\"/></p>");

        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getFirstChild());
        select(range);

        ImageConfig imageConfig = new ImageConfigJSONParser().parse(executable.getParameter());
        imageConfig.setReference("Blog.Trip@photo.png");
        imageConfig.setWidth("100pt");
        imageConfig.setAlignment(ImageAlignment.MIDDLE);

        rta.getDocument().addInnerHTMLListener(metaDataExtractor);
        assertTrue(executable.execute(new ImageConfigJSONSerializer().serialize(imageConfig)));
        rta.getDocument().removeInnerHTMLListener(metaDataExtractor);

        ImageElement image = (ImageElement) getBody().getFirstChild().getFirstChild();
        assertEquals("test", image.getId());
        assertEquals("photo", image.getClassName());
        assertEquals("middle", image.getStyle().getVerticalAlign());
        assertEquals("12px", image.getStyle().getMarginTop());
        assertEquals("A nice photo.", image.getTitle());
        assertFalse(image.hasAttribute(Style.WIDTH));
        assertEquals(imageConfig.getWidth(), image.getStyle().getWidth());
        assertEquals("40", image.getAttribute(Style.HEIGHT));
        assertEquals("<!--startimage:" + imageConfig.getReference() + "-->" + Element.INNER_HTML_PLACEHOLDER
            + "<!--stopimage-->", image.getAttribute(Element.META_DATA_ATTR));
    }

    /**
     * Tests that image dimensions are included in the image URL when they are specified.
     */
    public void testIfImageDimensionsAreIncludedInImageURL()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIfImageDimensionsAreIncludedInImageURL();
            }
        });
    }

    /**
     * Tests that image dimensions are included in the image URL when they are specified.
     */
    private void doTestIfImageDimensionsAreIncludedInImageURL()
    {
        rta.setHTML("<p>321</p>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getFirstChild(), 2);
        select(range);

        String imageURL = blankImageURL + "?width=23&height=17&keepAspectRatio=true";
        String imageJSON = "{reference:'Main.Test@logo.png',url:'" + imageURL + "',width:'150px',height:'100'}";
        rta.getDocument().addInnerHTMLListener(metaDataExtractor);
        assertTrue(executable.execute(imageJSON));
        rta.getDocument().removeInnerHTMLListener(metaDataExtractor);
        assertEquals(blankImageURL + "?width=150&height=100", executable.getSelectedElement().getSrc());
    }

    /**
     * Tests that image width is limited to rich text area width when it is not specified.
     */
    public void testImageWidthIsLimitedToRichTextAreaWidth()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestImageWidthIsLimitedToRichTextAreaWidth();
            }
        });
    }

    /**
     * Tests that image width is limited to rich text area width when it is not specified.
     */
    private void doTestImageWidthIsLimitedToRichTextAreaWidth()
    {
        rta.setHTML("<p>xyz</p>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getFirstChild(), 2);
        select(range);

        String imageJSON = "{reference:'Main.Test@logo.jpg',url:'" + blankImageURL + "'}";
        rta.getDocument().addInnerHTMLListener(metaDataExtractor);
        assertTrue(executable.execute(imageJSON));
        rta.getDocument().removeInnerHTMLListener(metaDataExtractor);
        assertEquals(blankImageURL + "?width=" + (rta.getDocument().getClientWidth() - 22), executable
            .getSelectedElement().getSrc());
    }

    /**
     * Tests that images with relative dimensions are properly resized.
     */
    public void testResizeImageWithRelativeDimensions()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestResizeImageWithRelativeDimensions();
            }
        });
    }

    /**
     * Tests that images with relative dimensions are properly resized.
     */
    private void doTestResizeImageWithRelativeDimensions()
    {
        rta.setHTML("<p>#</p>");

        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild());
        select(range);

        rta.getDocument().addInnerHTMLListener(metaDataExtractor);
        assertTrue(executable.execute("{reference:'x',url:'" + blankImageURL + "',width:'20%'}"));
        int computedWidth = executable.getSelectedElement().getWidth();
        // Double the image width.
        assertTrue(executable.execute(executable.getParameter().replace("20%", "40%")));
        rta.getDocument().removeInnerHTMLListener(metaDataExtractor);
        assertEquals(2 * computedWidth, executable.getSelectedElement().getWidth());
    }
}
