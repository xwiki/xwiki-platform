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
package org.xwiki.officeimporter.internal.cleaner;

import java.io.StringReader;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

/**
 * Test case for cleaning HTML images in {@link OfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class ImageOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * The key used to store the target document string reference in the cleaning parameters map.
     */
    private static final String TARGET_DOCUMENT_KEY = "targetDocument";

    /**
     * The default target document string reference.
     */
    private static final String TARGET_DOCUMENT_VALUE = "Import.Test";

    /**
     * Default target document reference.
     */
    private static final DocumentReference TARGET_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Import", "Test");

    /**
     * Default attachment reference to be used in tests.
     */
    private static final AttachmentReference DEFAULT_ATTACHMENT_REFERENCE =
        new AttachmentReference("foo.png", TARGET_DOCUMENT_REFERENCE);

    /**
     * Default attachment URL to be used in tests.
     */
    private static final String DEFAULT_ATTACHMENT_URL = "/bridge/foo.png";

    /**
     * {@code <img/>} links should be wrapped in xwiki specific html elements so that they are recognized by the XHTML
     * parser.
     */
    @org.junit.Test
    public void testImageWrapping()
    {
        String html = header + "<img src=\"foo.png\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(TARGET_DOCUMENT_KEY, TARGET_DOCUMENT_VALUE));

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentReferenceResolver).resolve(TARGET_DOCUMENT_VALUE);
                will(returnValue(TARGET_DOCUMENT_REFERENCE));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(DEFAULT_ATTACHMENT_REFERENCE, false);
                will(returnValue(DEFAULT_ATTACHMENT_URL));
            }
        });

        Document doc = officeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startComment = image.getPreviousSibling();
        Node stopComment = image.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        Assert.assertEquals("startimage:false|-|attach|-|foo.png", startComment.getNodeValue());
        Assert.assertEquals(DEFAULT_ATTACHMENT_URL, image.getAttribute("src"));
        Assert.assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        Assert.assertEquals("stopimage", stopComment.getNodeValue());
    }

    /**
     * Sometimes images are used inside links. In such cases, both the html link and the image need to be wrapped
     * properly.
     */
    @org.junit.Test
    public void testCompoundImageLinkWrapping()
    {
        String html = header + "<a href=\"http://www.xwiki.org\"><img src=\"foo.png\"/></a>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(TARGET_DOCUMENT_KEY, TARGET_DOCUMENT_VALUE));

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentReferenceResolver).resolve(TARGET_DOCUMENT_VALUE);
                will(returnValue(TARGET_DOCUMENT_REFERENCE));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(DEFAULT_ATTACHMENT_REFERENCE, false);
                will(returnValue(DEFAULT_ATTACHMENT_URL));
            }
        });

        Document doc = officeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startImageComment = image.getPreviousSibling();
        Node stopImageComment = image.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startImageComment.getNodeType());
        Assert.assertEquals("startimage:false|-|attach|-|foo.png", startImageComment.getNodeValue());
        Assert.assertEquals(DEFAULT_ATTACHMENT_URL, image.getAttribute("src"));
        Assert.assertEquals(Node.COMMENT_NODE, stopImageComment.getNodeType());
        Assert.assertEquals("stopimage", stopImageComment.getNodeValue());
        Element link = (Element) image.getParentNode();
        Assert.assertEquals("a", link.getNodeName());
        Assert.assertEquals("http://www.xwiki.org", link.getAttribute("href"));
    }

    /**
     * OpenOffice 3.2 server generates relative paths for embedded images. These relative paths should be cleaned and
     * the image name extracted.
     */
    @org.junit.Test
    public void testRelativePathCleaning()
    {
        String html = header + "<img src=\"../../some/path/foo.png\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(TARGET_DOCUMENT_KEY, TARGET_DOCUMENT_VALUE));

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentReferenceResolver).resolve(TARGET_DOCUMENT_VALUE);
                will(returnValue(TARGET_DOCUMENT_REFERENCE));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(DEFAULT_ATTACHMENT_REFERENCE, false);
                will(returnValue(DEFAULT_ATTACHMENT_URL));
            }
        });

        Document doc = officeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Assert.assertEquals(DEFAULT_ATTACHMENT_URL, image.getAttribute("src"));
    }

    /**
     * Added comments for images should be escaped properly.
     */
    @org.junit.Test
    public void testImageCommentEscaping()
    {
        String html = header + "<img src=\"-foo--bar.png-\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(TARGET_DOCUMENT_KEY, TARGET_DOCUMENT_VALUE));

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentReferenceResolver).resolve(TARGET_DOCUMENT_VALUE);
                will(returnValue(TARGET_DOCUMENT_REFERENCE));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(
                    new AttachmentReference("-foo--bar.png-", TARGET_DOCUMENT_REFERENCE), false);
                will(returnValue(DEFAULT_ATTACHMENT_URL));
            }
        });

        Document doc = officeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Node startComment = nodes.item(0).getPreviousSibling();
        Assert.assertEquals("startimage:false|-|attach|-|-foo-\\-bar.png-\\", startComment.getNodeValue());
    }
}
