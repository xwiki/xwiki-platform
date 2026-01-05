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
package org.xwiki.export.pdf.test.po;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

/**
 * Utility class to verify the content of a PDF document.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PDFDocument implements AutoCloseable
{
    private final PDDocument document;

    private final PDFImageExtractor imageExtractor;

    /**
     * Fetches and parses a PDF document from a given URL.
     * 
     * @param url where to fetch the PDF document from
     * @throws IOException if fetching and parsing the PDF document fails
     */
    public PDFDocument(URL url) throws IOException
    {
        this(url, null, null);
    }

    /**
     * Fetches and parses a PDF document from a given URL.
     * 
     * @param url where to fetch the PDF document from
     * @param userName the user name used to access the PDF document
     * @param password the password used to access the PDF document
     * @throws IOException if fetching and parsing the PDF document fails
     * @since 14.10
     */
    public PDFDocument(URL url, String userName, String password) throws IOException
    {
        URLConnection connection = url.openConnection();
        if (!StringUtils.isEmpty(userName)) {
            String auth = userName + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode((auth.getBytes(StandardCharsets.UTF_8)));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            connection.setRequestProperty("Authorization", authHeaderValue);
        }
        this.document = Loader.loadPDF(IOUtils.toByteArray(connection));
        this.imageExtractor = new PDFImageExtractor();
    }

    @Override
    public void close() throws Exception
    {
        this.document.close();
    }

    /**
     * @return the number of pages
     */
    public int getNumberOfPages()
    {
        return this.document.getNumberOfPages();
    }

    /**
     * @param pageNumber the page number
     * @return the text from the specified page
     * @throws IOException if we fail to extract the page text
     */
    public String getTextFromPage(int pageNumber) throws IOException
    {
        // The text stripper is using 1-based index.
        int realPageNumber = pageNumber + 1;
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(realPageNumber);
        textStripper.setEndPage(realPageNumber);
        return textStripper.getText(this.document);
    }

    /**
     * @return the entire text from this PDF document
     * @throws IOException if we fail to extract the text
     */
    public String getText() throws IOException
    {
        return new PDFTextStripper().getText(this.document);
    }

    /**
     * @return a mapping between link labels and link targets
     * @throws IOException if we fail to extract the links from this PDF document
     */
    public Map<String, String> getLinks() throws IOException
    {
        Map<String, String> links = new LinkedHashMap<>();
        for (int i = 0; i < this.document.getNumberOfPages(); i++) {
            links.putAll(getLinksFromPage(i));
        }
        return links;
    }

    /**
     * @param pageNumber the page number
     * @return a mapping between link labels and link targets
     * @throws IOException if we fail to extract the links from the specified page
     */
    public Map<String, String> getLinksFromPage(int pageNumber) throws IOException
    {
        return getLinksFromPage(pageNumber, true);
    }

    /**
     * @param pageNumber the page number
     * @param merge whether to merge consecutive link annotations with the same target
     * @return a mapping between link labels and link targets
     * @throws IOException if we fail to extract the links from the specified page
     */
    public Map<String, String> getLinksFromPage(int pageNumber, boolean merge) throws IOException
    {
        return getLinksFromPage(this.document.getPage(pageNumber), merge);
    }

    /**
     * Code adapted from
     * https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/PrintURLs.java
     */
    private Map<String, String> getLinksFromPage(PDPage page, boolean merge) throws IOException
    {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        List<PDAnnotation> annotations = page.getAnnotations();
        // First setup the text extraction regions.
        for (int j = 0; j < annotations.size(); j++) {
            PDAnnotation annotation = annotations.get(j);
            if (annotation instanceof PDAnnotationLink) {
                PDRectangle rect = annotation.getRectangle();
                // Need to reposition link rectangle to match text space.
                float x = rect.getLowerLeftX();
                float y = rect.getUpperRightY();
                float width = rect.getWidth();
                float height = rect.getHeight();
                int rotation = page.getRotation();
                if (rotation == 0) {
                    PDRectangle pageSize = page.getMediaBox();
                    // Area stripper uses java coordinates, not PDF coordinates.
                    y = pageSize.getHeight() - y;
                }

                Rectangle2D.Float awtRect = new Rectangle2D.Float(x, y, width, height);
                stripper.addRegion(String.valueOf(j), awtRect);
            }
        }

        stripper.extractRegions(page);

        Map<String, String> links = new LinkedHashMap<>();
        // Starting with Chrome 124 we sometimes get multiple link annotations for the same source HTML link. Basically,
        // the link label is split into multiple parts, each part being annotated as a link to the same target. We
        // overcome this strange behavior by merging consecutive link annotations with the same target.
        StringBuilder linkLabel = new StringBuilder();
        String previousLinkTarget = null;
        for (int j = 0; j < annotations.size(); j++) {
            PDAnnotation annotation = annotations.get(j);
            if (annotation instanceof PDAnnotationLink) {
                PDAnnotationLink link = (PDAnnotationLink) annotation;
                String linkTarget = getLinkTarget(link);
                if (!merge || (previousLinkTarget != null && !previousLinkTarget.equals(linkTarget))) {
                    // Commit the current link group and start a new link group.
                    links.put(linkLabel.toString(), previousLinkTarget);
                    linkLabel.setLength(0);
                }

                linkLabel.append(stripper.getTextForRegion(String.valueOf(j)).trim());
                previousLinkTarget = linkTarget;
            } else if (previousLinkTarget != null) {
                // Commit the current link group and start a new link group.
                links.put(linkLabel.toString(), previousLinkTarget);
                linkLabel.setLength(0);
                previousLinkTarget = null;
            }
        }

        if (previousLinkTarget != null) {
            // Commit the last link group.
            links.put(linkLabel.toString(), previousLinkTarget);
        }

        return links;
    }

    private String getLinkTarget(PDAnnotationLink link) throws IOException
    {
        PDAction action = link.getAction();
        PDDestination destination = link.getDestination();
        if (action instanceof PDActionGoTo) {
            PDActionGoTo anchor = (PDActionGoTo) action;
            return getDestinationText(anchor.getDestination());
        } else if (action instanceof PDActionURI) {
            PDActionURI uri = (PDActionURI) action;
            return uri.getURI();
        } else if (destination != null) {
            return getDestinationText(destination);
        } else {
            return null;
        }
    }

    private String getDestinationText(PDDestination destination) throws IOException
    {
        if (destination instanceof PDPageXYZDestination) {
            return getDestinationText((PDPageXYZDestination) destination);
        } else if (destination instanceof PDPageDestination) {
            return "Page " + ((PDPageDestination) destination).getPageNumber();
        } else if (destination instanceof PDNamedDestination) {
            return ((PDNamedDestination) destination).getNamedDestination();
        }
        return destination.toString();
    }

    private String getDestinationText(PDPageXYZDestination destination) throws IOException
    {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.addRegion("destination", getRectangleBelowDestination(destination));
        stripper.extractRegions(destination.getPage());
        return stripper.getTextForRegion("destination").trim();
    }

    private Rectangle2D getRectangleBelowDestination(PDPageXYZDestination destination)
    {
        PDPage page = destination.getPage();
        PDRectangle pageSize = page.getMediaBox();
        float x = destination.getLeft();
        float y = pageSize.getHeight() - destination.getTop();
        float width = pageSize.getWidth();
        float height = destination.getTop();
        return new Rectangle2D.Float(x, y, width, height);
    }

    /**
     * @return the images from this PDF document
     * @throws IOException if we fail to extract the images
     */
    public List<PDFImage> getImages() throws IOException
    {
        List<PDFImage> images = new ArrayList<>();
        for (PDPage page : this.document.getPages()) {
            images.addAll(getImagesFromPage(page));
        }
        return images;
    }

    /**
     * @param pageNumber the page number
     * @return the images from the specified page
     * @throws IOException if we fail to extract the images
     */
    public List<PDFImage> getImagesFromPage(int pageNumber) throws IOException
    {
        return getImagesFromPage(this.document.getPage(pageNumber));
    }

    /**
     * Each node from the outline is printed on a new lines, with indentation representing the outline hierarchy.
     *
     * @return the outline from this PDF document as text
     * @since 18.0.0RC1
     * @since 17.10.1
     */
    public String getOutlineText()
    {
        PDDocumentOutline root = this.document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem item = root.getFirstChild();
        StringBuilder output = new StringBuilder();
        while (item != null) {
            printOutlineItem(item, output, 0);
            item = item.getNextSibling();
        }
        return output.toString();
    }

    private void printOutlineItem(PDOutlineItem item, StringBuilder output, int level)
    {
        output.append(StringUtils.repeat("  ", level));
        output.append(item.getTitle()).append("\n");
        PDOutlineItem child = item.getFirstChild();
        while (child != null) {
            printOutlineItem(child, output, level + 1);
            child = child.getNextSibling();
        }
    }

    private List<PDFImage> getImagesFromPage(PDPage page) throws IOException
    {
        return this.imageExtractor.getImages(page);
    }
}
