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
 *
 */
package org.xwiki.xml.internal.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.JDomSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagTransformation;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Default implementation for {@link org.xwiki.xml.html.HTMLCleaner} using the <a href="HTML Cleaner
 * framework>http://htmlcleaner.sourceforge.net/</a>.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class DefaultHTMLCleaner implements HTMLCleaner, Initializable
{
    /**
     * List of default cleaning filters to call when cleaning code with HTML Cleaner. This is for cases when there are
     * no <a href="http://htmlcleaner.sourceforge.net/parameters.php">properties</a> defined in HTML Cleaner.
     */
    private List<CleaningFilter> filters;

    /**
     * The HTML Cleaner instance we used for cleaning HTML.
     */
    private HtmlCleaner cleaner;

    /**
     * The HTML Cleaner properties we use for each cleaning.
     */
    private CleanerProperties cleanerProperties;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.filters = new ArrayList<CleaningFilter>();
        this.filters.add(new ListCleaningFilter());
        // TODO: See http://jira.xwiki.org/jira/browse/XWIKI-2668
        // Commenting for now to prevent WikiModel XHTML parser from going to the internet to get the DTD
        // Need to specify a local DTD instead (somehow).
        // this.filters.add(new DocTypeCleaningFilter());

        // Initialize Cleaner objects once.
        this.cleaner = new HtmlCleaner();
        this.cleaner.setTransformations(getCleaningTransformations());
        this.cleanerProperties = this.cleaner.getProperties();
        this.cleanerProperties.setOmitUnknownTags(true);
        this.cleanerProperties.setPruneTags("script,style");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.xml.html.HTMLCleaner#clean(String)
     */
    public org.w3c.dom.Document clean(String originalHtmlContent)
    {
        org.w3c.dom.Document result;

        TagNode cleanedNode;
        try {
            cleanedNode = this.cleaner.clean(originalHtmlContent);
        } catch (IOException e) {
            // This shouldn't happen since we're not doing any IO... I consider this a flaw in the
            // design of HTML Cleaner.
            throw new RuntimeException("Unhandled error when cleaning HTML [" + originalHtmlContent + "]", e);
        }

        Document document = new JDomSerializer(this.cleanerProperties, false).createJDom(cleanedNode);

        // Perform other cleaning operation this time using the W3C Document interface.
        for (CleaningFilter filter : this.filters) {
            filter.filter(document);
        }

        try {
            DOMOutputter outputter = new DOMOutputter();
            result = outputter.output(document);
        } catch (JDOMException e) {
            throw new RuntimeException("Failed to convert JDOM Document to W3C Document for content ["
                + originalHtmlContent + "]", e);
        }

        return result;
    }

    /**
     * @return the cleaning transformations to perform on tags, in addition to the base transformations done by
     *         HTML Cleaner
     */
    private CleanerTransformations getCleaningTransformations()
    {
        CleanerTransformations transformations = new CleanerTransformations();

        TagTransformation tt = new TagTransformation(HTMLConstants.B, HTMLConstants.STRONG, false);
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.I, HTMLConstants.EM, false);
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.U, HTMLConstants.INS, false);
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.S, HTMLConstants.DEL, false);
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.STRIKE, HTMLConstants.DEL, false);
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.CENTER, HTMLConstants.P, false);
        tt.addAttributeTransformation(HTMLConstants.STYLE_ATTRIBUTE, "text-align:center");
        transformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.FONT, HTMLConstants.SPAN, false);
        tt.addAttributeTransformation(HTMLConstants.STYLE_ATTRIBUTE,
            "color:${color};font-family=${face};font-size=${size}pt;");
        transformations.addTransformation(tt);

        return transformations;
    }
}
