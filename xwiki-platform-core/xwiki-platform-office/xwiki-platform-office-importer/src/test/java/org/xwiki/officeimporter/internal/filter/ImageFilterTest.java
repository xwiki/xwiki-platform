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

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.internal.renderer.xhtml.XHTMLMarkerResourceReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.ooxi.jdatauri.DataUri;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImageFilter}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ XHTMLMarkerResourceReferenceSerializer.class })
class ImageFilterTest extends AbstractHTMLFilterTest
{
    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedResolver;

    private final DocumentReference documentReference = new DocumentReference("wiki", List.of("Path.To"), "Page");

    @BeforeEach
    @Override
    public void configure() throws Exception
    {
        super.configure();
        when(this.currentMixedResolver.resolve("Path.To.Page")).thenReturn(this.documentReference);
    }

    @Test
    void filterRemovesAlignAttribute()
    {
        assertFilteredOutput("<img align=\"center\"/>", Map.of(), "<img/>");
    }

    @Test
    void filterAddsImageMarkers()
    {
        AttachmentReference attachmentReference = new AttachmentReference("-foo--bar.png-", this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/foo.png");

        ResourceReference resourceReference = new ResourceReference("-foo--bar.png-", ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);

        assertFilteredOutput("<img src=\"../../some/path/-foo--b%61r.png-\"/>",
            Map.of("targetDocument", "Path.To.Page"),
            "<!--startimage:false|-|attach|-|-foo-\\-bar.png-\\--><img src=\"/path/to/foo.png\"/><!--stopimage-->");
    }

    @Test
    void filterAddsImageMarkersSpecialCharacters()
    {
        String imageNameUrl = "foo&amp;+_b%61r@.png";
        String imageName = "foo&+_bar\\@.png";
        String encodedImageName = URLEncoder.encode(imageName, UTF_8);
        AttachmentReference attachmentReference = new AttachmentReference(imageName, this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/" + encodedImageName);

        ResourceReference resourceReference = new ResourceReference(imageName, ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);
        String imageNameEscaped = "foo&+_bar\\\\@.png";

        assertFilteredOutput(String.format("<img src=\"../../some/path/%s\"/>", imageNameUrl),
            Map.of("targetDocument", "Path.To.Page"),
            String.format("<!--startimage:false|-|attach|-|%s--><img src=\"/path/to/%s\"/><!--stopimage-->",
                imageNameEscaped, encodedImageName));
    }

    @Test
    void filterIgnoresAbsoluteURLs()
    {
        assertFilteredOutput("<img src=\"http://server/path/to/image.png\"/>",
            Map.of("targetDocument", "Path.To.Page"),
            "<img src=\"http://server/path/to/image.png\"/>");
        assertFilteredOutput("<img src=\"file://path/to/image.png\"/>",
            Map.of("targetDocument", "Path.To.Page"), "<img src=\"file://path/to/image.png\"/>");
    }

    @Test
    void filterCollectsEmbeddedImages()
    {
        AttachmentReference attachmentReference = new AttachmentReference("foo.png", this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/foo.png");

        ResourceReference resourceReference = new ResourceReference("foo.png", ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);

        String fileName =
            DataUri.parse("data:image/jpeg;base64,GgoAAAAN==", UTF_8).hashCode() + ".jpg";
        attachmentReference = new AttachmentReference(fileName, this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/" + fileName);

        resourceReference = new ResourceReference(fileName, ResourceType.ATTACHMENT);
        resourceReference.setTyped(false);

        Map<String, String> parameters = Map.of(
            "targetDocument", "Path.To.Page",
            "attachEmbeddedImages", "true"
        );

        Document document = assertFilteredOutput(
            "<img src=\"data:image/png;fileName=foo.png;base64,iVBORw0K==\"/>"
                + "<img src=\"data:image/jpeg;base64,GgoAAAAN==\"/>",
            parameters,
            "<!--startimage:false|-|attach|-|foo.png--><img src=\"/path/to/foo.png\"/><!--stopimage-->"
                + "<!--startimage:false|-|attach|-|" + fileName + "--><img src=\"/path/to/" + fileName
                + "\"/><!--stopimage-->");

        @SuppressWarnings("unchecked")
        Map<String, byte[]> embeddedImages = (Map<String, byte[]>) document.getUserData("embeddedImages");
        assertEquals(Set.of("foo.png", fileName), embeddedImages.keySet());
    }

    @Test
    void rewriteImagePrefix()
    {
        Map<String, String> configuration = Map.of(
            "targetDocument", "Path.To.Page",
            "replaceImagePrefix", "output_",
            "replacementImagePrefix", "re@placement_"
        );

        String imageName = "re\\@placement_image.png";
        String encodedImageName = URLEncoder.encode(imageName.replace("\\@", "@"), UTF_8);
        AttachmentReference attachmentReference = new AttachmentReference(imageName, this.documentReference);
        when(this.dab.getAttachmentURL(attachmentReference, false)).thenReturn("/path/to/" + encodedImageName);

        assertFilteredOutput("<img src=\"output_image.png\" />", configuration,
            "<!--startimage:false|-|attach|-|re\\\\@placement_image.png-->"
                + "<img src=\"/path/to/re%40placement_image.png\"/><!--stopimage-->");

        verify(this.dab).getAttachmentURL(attachmentReference, false);
    }
}
