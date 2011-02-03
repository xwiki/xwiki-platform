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

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagTransformation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLConstants;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * Default implementation for {@link org.xwiki.xml.html.HTMLCleaner} using the <a href="HTML Cleaner
 * framework>http://htmlcleaner.sourceforge.net/</a>.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class DefaultHTMLCleaner implements HTMLCleaner, Initializable
{
    /**
     * The qualified name to be used when generating an html {@link DocumentType}.
     */
    private static final String QUALIFIED_NAME_HTML = "html";

    /**
     * {@link HTMLFilter} for filtering html lists.
     */
    @Requirement("list")
    private HTMLFilter listFilter;

    /**
     * {@link HTMLFilter} for filtering HTML font elements.
     */
    @Requirement("font")
    private HTMLFilter fontFilter;

    /**
     * {@link HTMLFilter} for wrapping invalid body elements with paragraphs.
     */
    @Requirement("body")
    private HTMLFilter bodyFilter;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // The clean method below is thread safe. However it seems that DOMOutputter.output() is not fully thread safe
        // since it causes the following exception on the first time it's called from different threads:
        //  Caused by: org.jdom.JDOMException: Reflection failed while creating new JAXP document:
        //  duplicate class definition: org/apache/xerces/jaxp/DocumentBuilderFactoryImpl
        //  at org.jdom.adapters.JAXPDOMAdapter.createDocument(JAXPDOMAdapter.java:191)
        //  at org.jdom.adapters.AbstractDOMAdapter.createDocument(AbstractDOMAdapter.java:133)
        //  at org.jdom.output.DOMOutputter.createDOMDocument(DOMOutputter.java:208)
        //  at org.jdom.output.DOMOutputter.output(DOMOutputter.java:127)
        // Since this only happens once, we call it first here at initialization time (since there's no thread
        // contention at that time). Note: This email thread seems to say it's thread safe but that's not what we see
        // here: http:osdir.com/ml/text.xml.xforms.chiba.devel/2006-09/msg00025.html
        clean(new StringReader(""));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.xml.html.HTMLCleaner#clean(java.io.Reader)
     */
    public Document clean(Reader originalHtmlContent)
    {
        return clean(originalHtmlContent, getDefaultConfiguration());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.xml.html.HTMLCleaner#clean(Reader, HTMLCleanerConfiguration)
     * @since 1.8.1
     */
    public Document clean(Reader originalHtmlContent, HTMLCleanerConfiguration configuration)
    {
        Document result = null;

        // HtmlCleaner is not threadsafe. Thus we need to recreate an instance at each run since otherwise we would need
        // to synchronize this clean() method which would slow down the whole system by queuing up cleaning requests.
        // See http://sourceforge.net/tracker/index.php?func=detail&aid=2139927&group_id=183053&atid=903699
        CleanerProperties cleanerProperties = getDefaultCleanerProperties(configuration);
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);

        cleaner.setTransformations(getDefaultCleanerTransformations());
        TagNode cleanedNode;
        try {
            cleanedNode = cleaner.clean(originalHtmlContent);
        } catch (Exception e) {
            // This shouldn't happen since we're not doing any IO... I consider this a flaw in the design of HTML
            // Cleaner.
            throw new RuntimeException("Unhandled error when cleaning HTML", e);
        }

        // Workaround HTML XML declaration bug.
        fixCleanedNodeBug(cleanedNode);

        // Serialize the cleanedNode TagNode into a w3c dom. Ideally following code should be enough.
        // But SF's HTML Cleaner seems to omit the DocType declaration while serializing.
        // See https://sourceforge.net/tracker/index.php?func=detail&aid=2062318&group_id=183053&atid=903696
        //      cleanedNode.setDocType(new DoctypeToken("html", "PUBLIC", "-//W3C//DTD XHTML 1.0 Strict//EN",
        //          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"));
        //      try {
        //          result = new DomSerializer(cleanerProperties, false).createDOM(cleanedNode);
        //      } catch(ParserConfigurationException ex) { }
        // As a workaround, we must serialize the cleanedNode into a temporary w3c document, create a new w3c document
        // with proper DocType declaration and move the root node from the temporary document to the new one.
        try {
            // Since there's a bug in SF's HTML Cleaner in that it doesn't recognize CDATA blocks we need to turn off
            // character escaping (hence the false value passed) and do the escaping in XMLUtils.toString(). Note that
            // this can cause problem for code not serializing the W3C DOM to a String since it won't have the
            // characters escaped.
            // See https://sourceforge.net/tracker/index.php?func=detail&aid=2691888&group_id=183053&atid=903696
            Document tempDoc = new XWikiDOMSerializer(cleanerProperties, false).createDOM(cleanedNode);
            DOMImplementation domImpl =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            DocumentType docType = domImpl.createDocumentType(QUALIFIED_NAME_HTML, "-//W3C//DTD XHTML 1.0 Strict//EN",
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
            result = domImpl.createDocument(null, QUALIFIED_NAME_HTML, docType);
            result.replaceChild(result.adoptNode(tempDoc.getDocumentElement()), result.getDocumentElement());
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Error while serializing TagNode into w3c dom.", ex);
        }

        // Finally apply filters.
        for (HTMLFilter filter : configuration.getFilters()) {
            filter.filter(result, configuration.getParameters());
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HTMLCleaner#getDefaultConfiguration()
     * @since 1.8.1
     */
    public HTMLCleanerConfiguration getDefaultConfiguration()
    {
        HTMLCleanerConfiguration configuration = new DefaultHTMLCleanerConfiguration();
        configuration.setFilters(Arrays.asList(this.bodyFilter, this.listFilter, this.fontFilter));
        return configuration;
    }

    /**
     * @param configuration the configuration to use for the cleaning
     * @return the default {@link CleanerProperties} to be used for cleaning.
     */
    private CleanerProperties getDefaultCleanerProperties(HTMLCleanerConfiguration configuration)
    {
        CleanerProperties defaultProperties = new CleanerProperties();
        defaultProperties.setOmitUnknownTags(true);
        defaultProperties.setNamespacesAware(true);

        // HTML Cleaner uses the compact notation by default but we don't want that since:
        // - it's more work and not required since not compact notation is valid XHTML
        // - expanded elements can also be rendered fine in browsers that only support HTML.
        defaultProperties.setUseEmptyElementTags(false);

        // Wrap script and style content in CDATA blocks
        defaultProperties.setUseCdataForScriptAndStyle(true);

        // Handle the NAMESPACE_AWARE configuration property
        String param = configuration.getParameters().get(HTMLCleanerConfiguration.NAMESPACES_AWARE);
        boolean namespacesAware = (param != null) ? Boolean.parseBoolean(param) : defaultProperties.isNamespacesAware();
        defaultProperties.setNamespacesAware(namespacesAware);

        return defaultProperties;
    }

    /**
     * @return the default cleaning transformations to perform on tags, in addition to the base transformations done by
     *         HTML Cleaner
     */
    private CleanerTransformations getDefaultCleanerTransformations()
    {
        CleanerTransformations defaultTransformations = new CleanerTransformations();

        TagTransformation tt = new TagTransformation(HTMLConstants.TAG_B, HTMLConstants.TAG_STRONG, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_I, HTMLConstants.TAG_EM, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_U, HTMLConstants.TAG_INS, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_S, HTMLConstants.TAG_DEL, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_STRIKE, HTMLConstants.TAG_DEL, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.TAG_CENTER, HTMLConstants.TAG_P, false);
        tt.addAttributeTransformation(HTMLConstants.ATTRIBUTE_STYLE, "text-align:center");
        defaultTransformations.addTransformation(tt);

        return defaultTransformations;
    }

    /**
     * There's a known limitation (bug?) in HTML Cleaner where if there's a XML declaration specified it'll be copied as
     * the first element of the body. Thus remove it if it's there. See
     * https://sourceforge.net/forum/message.php?msg_id=4657800 and
     * https://sourceforge.net/tracker/index.php?func=detail&aid=2688635&group_id=183053&atid=903696
     * 
     * @param cleanedNode the cleaned node (ie after the HTML cleaning)
     */
    private void fixCleanedNodeBug(TagNode cleanedNode)
    {
        TagNode body = cleanedNode.getElementsByName("body", false)[0];
        if (body.getChildren().size() > 0) {
            Object firstBodyChild = body.getChildren().get(0);
            if (firstBodyChild instanceof ContentToken
                && ((ContentToken) firstBodyChild).getContent().startsWith("<?xml")) {
                body.removeChild(firstBodyChild);
            }
        }
    }
}
