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
package org.xwiki.officeimporter.internal.filter;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.xml.html.filter.HTMLFilter;

import com.github.ooxi.jdatauri.DataUri;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ImageFilter}.
 * 
 * @version $Id$
 */
public class ImageFilterTest extends AbstractHTMLFilterTest
{
    @Rule
    public MockitoComponentMockingRule<HTMLFilter> mocker =
        new MockitoComponentMockingRule<HTMLFilter>(ImageFilter.class);

    private DocumentAccessBridge dab;

    private ResourceReferenceSerializer xhtmlMarkerSerializer;

    private DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path.To"), "Page");

    @Override
    public void configure() throws Exception
    {
        super.configure();

        this.filter = this.mocker.getComponentUnderTest();
        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        this.xhtmlMarkerSerializer = this.mocker.getInstance(ResourceReferenceSerializer.class, "xhtmlmarker");

        DocumentReferenceResolver<String> currentMixedResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        when(currentMixedResolver.resolve("Path.To.Page")).thenReturn(this.documentReference);
    }

    @Test
    public void filterRemovesAlignAttribute()
    {
        filterAndAssertOutput("<img align=\"center\"/>", "<img/>");
    }

    @Test
    public void filterAddsImageMarkers()
    {
        AttachmentReference attachmentReference = new AttachmentReference("-foo--bar.png-", this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/foo.png");

        ResourceReference resourceReference = new ResourceReference("-foo--bar.png-", ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);
        when(this.xhtmlMarkerSerializer.serialize(resourceReference)).thenReturn("false|-|attach|-|-foo--bar.png-");

        filterAndAssertOutput("<img src=\"../../some/path/-foo--b%61r.png-\"/>",
            Collections.singletonMap("targetDocument", "Path.To.Page"),
            "<!--startimage:false|-|attach|-|-foo-\\-bar.png-\\--><img src=\"/path/to/foo.png\"/><!--stopimage-->");
    }

    @Test
    public void filterIgnoresAbsoluteURLs()
    {
        filterAndAssertOutput("<img src=\"http://server/path/to/image.png\"/>",
            Collections.singletonMap("targetDocument", "Path.To.Page"),
            "<img src=\"http://server/path/to/image.png\"/>");
        filterAndAssertOutput("<img src=\"file://path/to/image.png\"/>",
            Collections.singletonMap("targetDocument", "Path.To.Page"), "<img src=\"file://path/to/image.png\"/>");
    }

    @Test
    public void filterCollectsEmbeddedImages()
    {
        AttachmentReference attachmentReference = new AttachmentReference("foo.png", this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/foo.png");

        ResourceReference resourceReference = new ResourceReference("foo.png", ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);
        when(this.xhtmlMarkerSerializer.serialize(resourceReference)).thenReturn("false|-|attach|-|foo.png");

        String fileName =
            DataUri.parse("data:image/jpeg;base64,GgoAAAAN==", Charset.forName("UTF-8")).hashCode() + ".jpg";
        attachmentReference = new AttachmentReference(fileName, this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/" + fileName);

        resourceReference = new ResourceReference(fileName, ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);
        when(this.xhtmlMarkerSerializer.serialize(resourceReference)).thenReturn("false|-|attach|-|" + fileName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("targetDocument", "Path.To.Page");
        parameters.put("attachEmbeddedImages", "true");

        Document document = filterAndAssertOutput(
            "<img src=\"data:image/png;fileName=foo.png;base64,iVBORw0K==\"/>"
                + "<img src=\"data:image/jpeg;base64,GgoAAAAN==\"/>",
            parameters,
            "<!--startimage:false|-|attach|-|foo.png--><img src=\"/path/to/foo.png\"/><!--stopimage-->"
                + "<!--startimage:false|-|attach|-|" + fileName + "--><img src=\"/path/to/" + fileName
                + "\"/><!--stopimage-->");

        @SuppressWarnings("unchecked")
        Map<String, byte[]> embeddedImages = (Map<String, byte[]>) document.getUserData("embeddedImages");
        assertEquals(new HashSet<String>(Arrays.asList("foo.png", fileName)), embeddedImages.keySet());
    }
}
