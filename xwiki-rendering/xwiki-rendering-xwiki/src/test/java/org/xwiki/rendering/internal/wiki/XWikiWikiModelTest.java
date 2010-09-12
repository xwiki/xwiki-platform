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
package org.xwiki.rendering.internal.wiki;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link XWikiWikiModel}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiWikiModelTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private XWikiWikiModel wikiModel;

    @Test
    public void testGetDocumentEditURLWhenNoQueryStringSpecified() throws Exception
    {
        final EntityReferenceSerializer< ? > entityReferenceSerializer =
            getComponentManager().lookup(EntityReferenceSerializer.class);
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getCurrentDocumentReference();
                will(returnValue(documentReference));
                oneOf(entityReferenceSerializer).serialize(documentReference);
                will(returnValue("wiki:Space.Page\u20AC"));

                // The test is here: we verify that getURL is called with the query string already encoded since
                // getURL() doesn't encode it.
                oneOf(documentAccessBridge).getURL("Space.Page\u20AC", "create", "parent=wiki%3ASpace.Page%E2%82%AC",
                    "anchor");
            }
        });

        wikiModel.getDocumentEditURL("Space.Page\u20AC", "anchor", null);
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height image parameters are specified.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothWidthAndHeightAttributesAreSpecified() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("width", "100px");
        parameters.put("height", "50");
        Assert.assertEquals("?width=100&height=50", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height image parameters are specified
     * but including them in the image URL is forbidden from the rendering configuration.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenIncludingImageDimensionsIsForbidden() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(false));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("width", "101px");
        parameters.put("height", "55px");
        Assert.assertEquals("", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height CSS properties are specified.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothWidthAndHeightCSSPropertiesAreSpecified() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("style", "border: 1px; height: 30px; margin-top: 2em; width: 70px");
        Assert.assertEquals("?width=70&height=30", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when only the width image parameter is specified.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenOnlyWidthAttributeIsSpecified() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("width", "150");
        parameters.put("height", "30%");
        Assert.assertEquals("?width=150", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when only the height CSS property is specified.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenOnlyHeightCSSPropertyIsSpecified() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("style", "width: 5cm; height: 80px");
        Assert.assertEquals("?height=80", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height are unspecified and image size is
     * limited in the configuration.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothWidthAndHeightAreUnspecifiedAndImageSizeIsLimited() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
                oneOf(configuration).getImageWidthLimit();
                will(returnValue(200));
                oneOf(configuration).getImageHeightLimit();
                will(returnValue(170));
            }
        });
        Map<String, String> parameters = Collections.emptyMap();
        Assert.assertEquals("?width=200&height=170&keepAspectRatio=true", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height are unspecified and only the
     * image width is limited in the configuration.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothWidthAndHeightAreUnspecifiedAndOnlyImageWidthIsLimited() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
                oneOf(configuration).getImageWidthLimit();
                will(returnValue(25));
                oneOf(configuration).getImageHeightLimit();
                will(returnValue(-1));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("width", "45%");
        parameters.put("style", "height:10em");
        Assert.assertEquals("?width=25", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the width and the height are unspecified and the image
     * size is not limited in the configuration.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothWidthAndHeightAreUnspecifiedAndImageSizeIsNotLimited() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
                oneOf(configuration).getImageWidthLimit();
                will(returnValue(-1));
                oneOf(configuration).getImageHeightLimit();
                will(returnValue(-1));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("style", "bad CSS declaration");
        Assert.assertEquals("", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when the attachment URL has a fragment identifier.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenAttachmentURLHasFragmentIdentifier() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue("test#fragment"));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("width", "23");
        Assert.assertEquals("test?width=23#fragment", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when the attachment URL has a query string and a fragment
     * identifier.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenAttachmentURLHasQueryStringAndFragmentIdentifier() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue("test?param=value#fragment"));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("height", "17");
        Assert.assertEquals("test?param=value&height=17#fragment", wikiModel.getImageURL("", "", parameters));
    }

    /**
     * Tests that the proper image URL is generated when both the style and the dimension parameters are specified.
     * 
     * @throws Exception if an exception occurs while running the test
     */
    @Test
    public void testGetImageURLWhenBothStyleAndDimensionParametersAreSpecified() throws Exception
    {
        final DocumentAccessBridge documentAccessBridge = getComponentManager().lookup(DocumentAccessBridge.class);
        final RenderingConfiguration configuration = getComponentManager().lookup(RenderingConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getAttachmentURL("", "");
                will(returnValue(""));
                oneOf(configuration).isIncludeImageDimensionsInImageURL();
                will(returnValue(true));
            }
        });
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("height", "46");
        parameters.put("width", "101px");
        parameters.put("style", "width: 20%; height:75px");
        // Note that the style parameter take precedence over the dimension parameters and the width is actually 20% but
        // we can't use it for resizing the image on the server side so it's omitted from the query string.
        Assert.assertEquals("?height=75", wikiModel.getImageURL("", "", parameters));
    }
}
