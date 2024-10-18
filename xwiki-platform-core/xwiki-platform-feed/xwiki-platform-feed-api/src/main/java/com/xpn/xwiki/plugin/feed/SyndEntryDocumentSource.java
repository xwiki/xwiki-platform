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
package com.xpn.xwiki.plugin.feed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.output.NullWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.tidy.Tidy;
import org.xwiki.xml.XMLUtils;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndCategoryImpl;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.feed.synd.SyndPersonImpl;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.TidyMessageLogger;

/**
 * Concrete strategy for computing the field values of a feed entry from any {@link XWikiDocument} instance.
 */
public class SyndEntryDocumentSource implements SyndEntrySource
{
    /**
     * Utility class for selecting a property from a XWiki object.
     */
    public static class PropertySelector
    {
        /**
         * The name of a XWiki class.
         */
        private String className;

        /**
         * The index of an object within the document that this selector is applied to.
         */
        private int objectIndex;

        /**
         * The name of a property available for {@link #className}.
         */
        private String propertyName;

        /**
         * Creates a new instance from a string representation.
         * 
         * @param strRep a string like "ClassName_ObjectIndex_PropertyName", where class name and object index are
         *            optional
         */
        public PropertySelector(String strRep)
        {
            int indexStartPos = strRep.indexOf('_');
            if (indexStartPos < 0) {
                // class name and object index are not specified
                this.className = null;
                this.objectIndex = 0;
                this.propertyName = strRep;
            } else {
                int propStartPos = strRep.indexOf("_", indexStartPos + 1);
                if (propStartPos < 0) {
                    // object index is not specified
                    this.className = strRep.substring(0, indexStartPos);
                    this.objectIndex = 0;
                    this.propertyName = strRep.substring(indexStartPos + 1);
                } else {
                    // all three have been specified
                    this.className = strRep.substring(0, indexStartPos);
                    this.objectIndex = Integer.parseInt(strRep.substring(indexStartPos + 1, propStartPos));
                    this.propertyName = strRep.substring(propStartPos + 1);
                }
            }
        }

        /**
         * @return the name of a XWiki class
         */
        public String getClassName()
        {
            return this.className;
        }

        /**
         * @return the index of an object within the document that this selector is applied to
         */
        public int getObjectIndex()
        {
            return this.objectIndex;
        }

        /**
         * @return the name of a property available for {@link #className}
         */
        public String getPropertyName()
        {
            return this.propertyName;
        }
    }

    protected static final Logger LOGGER = LoggerFactory.getLogger(SyndEntryDocumentSource.class);

    protected static final TidyMessageLogger TIDY_LOGGER = new TidyMessageLogger(LOGGER);

    public static final String CONTENT_TYPE = "ContentType";

    public static final String CONTENT_LENGTH = "ContentLength";

    public static final Properties TIDY_FEED_CONFIG;

    public static final Properties TIDY_XML_CONFIG;

    public static final Properties TIDY_HTML_CONFIG;

    public static final String FIELD_URI = "uri";

    public static final String FIELD_LINK = "link";

    public static final String FIELD_TITLE = "title";

    public static final String FIELD_DESCRIPTION = "description";

    public static final String FIELD_CATEGORIES = "categories";

    public static final String FIELD_PUBLISHED_DATE = "publishedDate";

    public static final String FIELD_UPDATED_DATE = "updatedDate";

    public static final String FIELD_AUTHOR = "author";

    public static final String FIELD_CONTRIBUTORS = "contributors";

    public static final Map<String, Object> DEFAULT_PARAMS;

    static {
        // general configuration
        TIDY_FEED_CONFIG = new Properties();
        TIDY_FEED_CONFIG.setProperty("force-output", "yes");
        TIDY_FEED_CONFIG.setProperty("indent-attributes", "no");
        TIDY_FEED_CONFIG.setProperty("indent", "no");
        TIDY_FEED_CONFIG.setProperty("quiet", "yes");
        TIDY_FEED_CONFIG.setProperty("trim-empty-elements", "yes");

        // XML specific configuration
        TIDY_XML_CONFIG = new Properties(TIDY_FEED_CONFIG);
        TIDY_XML_CONFIG.setProperty("input-xml", "yes");
        TIDY_XML_CONFIG.setProperty("output-xml", "yes");
        TIDY_XML_CONFIG.setProperty("add-xml-pi", "no");
        TIDY_XML_CONFIG.setProperty("input-encoding", "UTF8");

        // HTML specific configuration
        TIDY_HTML_CONFIG = new Properties(TIDY_FEED_CONFIG);
        TIDY_HTML_CONFIG.setProperty("output-xhtml", "yes");
        TIDY_HTML_CONFIG.setProperty("show-body-only", "yes");
        TIDY_HTML_CONFIG.setProperty("drop-empty-paras", "yes");
        TIDY_HTML_CONFIG.setProperty("enclose-text", "yes");
        TIDY_HTML_CONFIG.setProperty("logical-emphasis", "yes");
        TIDY_HTML_CONFIG.setProperty("input-encoding", "UTF8");

        // default parameters for all instances of this class
        DEFAULT_PARAMS = new HashMap<String, Object>();
        DEFAULT_PARAMS.put(CONTENT_TYPE, "text/html");
        DEFAULT_PARAMS.put(CONTENT_LENGTH, -1); // no limit by default
    }

    /**
     * Strategy instance parameters. Each concrete strategy can define its own (paramName, paramValue) pairs. These
     * parameters are overwritten by those used when calling
     * {@link SyndEntrySource#source(SyndEntry, Object, Map, XWikiContext)} method
     */
    private Map<String, Object> params;

    public SyndEntryDocumentSource()
    {
        this(new HashMap<String, Object>());
    }

    /**
     * Creates a new instance. The given parameters overwrite {@link #DEFAULT_PARAMS}.
     * 
     * @param params parameters only for this instance
     */
    public SyndEntryDocumentSource(Map<String, Object> params)
    {
        setParams(params);
    }

    /**
     * @return instance parameters
     */
    public Map<String, Object> getParams()
    {
        return this.params;
    }

    /**
     * Sets instance parameters. Instance parameters overwrite {@link #DEFAULT_PARAMS}
     * 
     * @param params instance parameters
     */
    public void setParams(Map<String, Object> params)
    {
        this.params = joinParams(params, getDefaultParams());
    }

    /**
     * Strategy class parameters
     */
    protected Map<String, Object> getDefaultParams()
    {
        return DEFAULT_PARAMS;
    }

    @Override
    public void source(SyndEntry entry, Object obj, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        // cast source
        Document doc = castDocument(obj, context);

        // test access rights
        if (!doc.hasAccessLevel("view")) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in view mode!");
        }

        // prepare parameters (overwrite instance parameters)
        Map<String, Object> trueParams = joinParams(params, getParams());

        sourceDocument(entry, doc, trueParams, context);
    }

    /**
     * Overwrites the current values of the given feed entry with new ones computed from the specified source document.
     * 
     * @param entry the feed entry whose fields are going to be overwritten
     * @param doc the source for the new values to be set on the fields of the feed entry
     * @param params parameters to adjust the computation. Each concrete strategy may define its own (key, value) pairs
     * @param context the XWiki context
     * @throws XWikiException
     */
    public void sourceDocument(SyndEntry entry, Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        entry.setUri(getURI(doc, params, context));
        entry.setLink(getLink(doc, params, context));
        entry.setTitle(getTitle(doc, params, context));
        entry.setDescription(getDescription(doc, params, context));
        entry.setCategories(getCategories(doc, params, context));
        entry.setPublishedDate(getPublishedDate(doc, params, context));
        entry.setUpdatedDate(getUpdateDate(doc, params, context));
        entry.setAuthor(getAuthor(doc, params, context));
        entry.setContributors(getContributors(doc, params, context).stream()
            .map(name -> {
                SyndPerson person = new SyndPersonImpl();
                person.setName(name);
                return person;
            })
            .toList());
    }

    protected String getDefaultURI(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        return doc.getExternalURL("view", "language=" + doc.getRealLanguage());
    }

    protected String getURI(Document doc, Map<String, Object> params, XWikiContext context) throws XWikiException
    {
        String mapping = (String) params.get(FIELD_URI);
        if (mapping == null) {
            return getDefaultURI(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseString(mapping, doc, context);
        } else {
            return getStringValue(mapping, doc, context);
        }
    }

    protected String getDefaultLink(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        return getDefaultURI(doc, params, context);
    }

    protected String getLink(Document doc, Map<String, Object> params, XWikiContext context) throws XWikiException
    {
        String mapping = (String) params.get(FIELD_LINK);
        if (mapping == null) {
            return getDefaultLink(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseString(mapping, doc, context);
        } else {
            return getStringValue(mapping, doc, context);
        }
    }

    protected String getDefaultTitle(Document doc, Map<String, Object> params, XWikiContext context)
    {
        return doc.getDisplayTitle();
    }

    protected String getTitle(Document doc, Map<String, Object> params, XWikiContext context) throws XWikiException
    {
        String mapping = (String) params.get(FIELD_TITLE);
        if (mapping == null) {
            return getDefaultTitle(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseString(mapping, doc, context);
        } else {
            return getStringValue(mapping, doc, context);
        }
    }

    protected String getDefaultDescription(Document doc, Map<String, Object> params, XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        String author = xwiki.getUserName(doc.getAuthor(), null, false, context);
        // the description format should be taken from a resource bundle, and thus localized
        String descFormat = "Version %1$s edited by %2$s on %3$s";
        return String.format(descFormat, new Object[] {doc.getVersion(), author, doc.getDate()});
    }

    protected SyndContent getDescription(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        String description;
        String mapping = (String) params.get(FIELD_DESCRIPTION);
        if (mapping == null) {
            description = getDefaultDescription(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            description = parseString(mapping, doc, context);
        } else {
            description = doc.getRenderedContent(getStringValue(mapping, doc, context), doc.getSyntaxId());
        }
        String contentType = (String) params.get(CONTENT_TYPE);
        int contentLength = ((Number) params.get(CONTENT_LENGTH)).intValue();
        if (contentLength >= 0) {
            if ("text/plain".equals(contentType)) {
                description = getPlainPreview(description, contentLength);
            } else if ("text/html".equals(contentType)) {
                description = getHTMLPreview(description, contentLength);
            } else if ("text/xml".equals(contentType)) {
                description = getXMLPreview(description, contentLength);
            }
        }
        return getSyndContent(contentType, description);
    }

    protected List<SyndCategory> getDefaultCategories(Document doc, Map<String, Object> params, XWikiContext context)
    {
        return Collections.emptyList();
    }

    protected List<SyndCategory> getCategories(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        String mapping = (String) params.get(FIELD_CATEGORIES);
        if (mapping == null) {
            return getDefaultCategories(doc, params, context);
        }

        List<Object> categories;
        if (isVelocityCode(mapping)) {
            categories = parseList(mapping, doc, context);
        } else {
            categories = getListValue(mapping, doc, context);
        }

        List<SyndCategory> result = new ArrayList<SyndCategory>();
        for (Object category : categories) {
            if (category instanceof SyndCategory) {
                result.add((SyndCategory) category);
            } else if (category != null) {
                SyndCategory scat = new SyndCategoryImpl();
                scat.setName(category.toString());
                result.add(scat);
            }
        }
        return result;
    }

    protected Date getDefaultPublishedDate(Document doc, Map<String, Object> params, XWikiContext context)
    {
        return doc.getDate();
    }

    protected Date getPublishedDate(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        String mapping = (String) params.get(FIELD_PUBLISHED_DATE);
        if (mapping == null) {
            return getDefaultPublishedDate(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseDate(mapping, doc, context);
        } else {
            return getDateValue(mapping, doc, context);
        }
    }

    protected Date getDefaultUpdateDate(Document doc, Map<String, Object> params, XWikiContext context)
    {
        return doc.getDate();
    }

    protected Date getUpdateDate(Document doc, Map<String, Object> params, XWikiContext context) throws XWikiException
    {
        String mapping = (String) params.get(FIELD_UPDATED_DATE);
        if (mapping == null) {
            return getDefaultUpdateDate(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseDate(mapping, doc, context);
        } else {
            return getDateValue(mapping, doc, context);
        }
    }

    protected String getDefaultAuthor(Document doc, Map<String, Object> params, XWikiContext context)
    {
        return context.getWiki().getUserName(doc.getCreator(), null, false, context);
    }

    protected String getAuthor(Document doc, Map<String, Object> params, XWikiContext context) throws XWikiException
    {
        String mapping = (String) params.get(FIELD_AUTHOR);
        if (mapping == null) {
            return getDefaultAuthor(doc, params, context);
        } else if (isVelocityCode(mapping)) {
            return parseString(mapping, doc, context);
        } else {
            return getStringValue(mapping, doc, context);
        }
    }

    protected List<String> getDefaultContributors(Document doc, Map<String, Object> params, XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        List<String> contributors = new ArrayList<String>();
        contributors.add(xwiki.getUserName(doc.getAuthor(), null, false, context));
        return contributors;
    }

    protected List<String> getContributors(Document doc, Map<String, Object> params, XWikiContext context)
        throws XWikiException
    {
        String mapping = (String) params.get(FIELD_CONTRIBUTORS);
        if (mapping == null) {
            return getDefaultContributors(doc, params, context);
        }

        List<Object> rawContributors;
        if (isVelocityCode(mapping)) {
            rawContributors = parseList(mapping, doc, context);
        } else {
            rawContributors = getListValue(mapping, doc, context);
        }

        List<String> contributors = new ArrayList<String>();
        for (Object rawContributor : rawContributors) {
            if (rawContributor instanceof String) {
                contributors.add((String) rawContributor);
            } else {
                contributors.add(rawContributor.toString());
            }
        }

        return contributors;
    }

    /**
     * Distinguishes between mapping to a property and mapping to a velocity code.
     * 
     * @param mapping
     * @return true if the given string is a mapping to a velocity code
     */
    protected boolean isVelocityCode(String mapping)
    {
        return mapping.charAt(0) == '{' && mapping.charAt(mapping.length() - 1) == '}';
    }

    protected String parseString(String mapping, Document doc, XWikiContext context) throws XWikiException
    {
        if (isVelocityCode(mapping)) {
            return doc.getRenderedContent(mapping.substring(1, mapping.length() - 1), doc.getSyntax().toIdString());
        } else {
            return mapping;
        }
    }

    /**
     * Converts the given velocity string to a {@link Date} instance. The velocity code must be evaluated to a long
     * representing a time stamp.
     */
    protected Date parseDate(String mapping, Document doc, XWikiContext context) throws NumberFormatException,
        XWikiException
    {
        if (isVelocityCode(mapping)) {
            return new Date(Long.parseLong(parseString(mapping, doc, context).trim()));
        } else {
            return null;
        }
    }

    /**
     * Converts the given velocity code to a {@link List} instance. The velocity code must be evaluated to a string with
     * the following format: "[item1,item2,...,itemN]".
     */
    protected List<Object> parseList(String mapping, Document doc, XWikiContext context) throws XWikiException
    {
        if (!isVelocityCode(mapping)) {
            return null;
        }
        String strRep = parseString(mapping, doc, context).trim();
        if (strRep.charAt(0) != '[' || strRep.charAt(strRep.length() - 1) != ']') {
            return null;
        }
        String[] array = strRep.substring(1, strRep.length() - 1).split(",");
        if (array.length > 0) {
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves the value of a string property.
     */
    protected String getStringValue(String mapping, Document doc, XWikiContext context) throws XWikiException
    {
        PropertySelector ps = new PropertySelector(mapping);
        if (ps.getClassName() == null) {
            return doc.display(ps.getPropertyName());
        } else {
            XWikiDocument xdoc = context.getWiki().getDocument(doc.getFullName(), context);
            return xdoc.getObject(ps.getClassName(), ps.getObjectIndex()).getStringValue(ps.getPropertyName());
        }
    }

    /**
     * Retrieves the value of a date property.
     */
    protected Date getDateValue(String mapping, Document doc, XWikiContext context) throws XWikiException
    {
        XWikiDocument xdoc = context.getWiki().getDocument(doc.getFullName(), context);
        PropertySelector ps = new PropertySelector(mapping);
        if (ps.getClassName() == null) {
            return xdoc.getFirstObject(ps.getPropertyName(), context).getDateValue(ps.getPropertyName());
        } else {
            return xdoc.getObject(ps.getClassName(), ps.getObjectIndex()).getDateValue(ps.getPropertyName());
        }
    }

    /**
     * Retrieves the value of a list property.
     */
    protected List<Object> getListValue(String mapping, Document doc, XWikiContext context) throws XWikiException
    {
        XWikiDocument xdoc = context.getWiki().getDocument(doc.getFullName(), context);
        PropertySelector ps = new PropertySelector(mapping);
        if (ps.getClassName() == null) {
            return xdoc.getFirstObject(ps.getPropertyName(), context).getListValue(ps.getPropertyName());
        } else {
            return xdoc.getObject(ps.getClassName(), ps.getObjectIndex()).getListValue(ps.getPropertyName());
        }
    }

    /**
     * @return base + (extra - base)
     */
    protected Map<String, Object> joinParams(Map<String, Object> base, Map<String, Object> extra)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(base);

        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            if (params.get(entry.getKey()) == null) {
                params.put(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    /**
     * Cleans up the given XML fragment using the specified configuration.
     * 
     * @param xmlFragment the XML fragment to be cleaned up
     * @param config the configuration properties to use
     * @return the DOM tree associated with the cleaned up XML fragment
     */
    public static org.w3c.dom.Document tidy(String xmlFragment, Properties config)
    {
        Tidy tidy = new Tidy();
        tidy.setConfigurationFromProps(config);
        // We capture the logs and redirect them to the XWiki logging subsystem. Since we do this we don't want
        // JTidy warnings and errors to be sent to stderr/stdout
        tidy.setMessageListener(TIDY_LOGGER);
        tidy.setErrout(new PrintWriter(new NullWriter()));
        // Even if we add a message listener we still have to redirect the output. Otherwise all the messages will be
        // written to the standard output (besides being logged by TIDY_LOGGER).
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(xmlFragment.getBytes(Charset.forName(config.getProperty("input-encoding"))));
        return tidy.parseDOM(in, out);
    }

    /**
     * Computes the sum of lengths of all the text nodes within the given DOM sub-tree
     * 
     * @param node the root of the DOM sub-tree containing the text
     * @return the sum of lengths of text nodes within the given DOM sub-tree
     */
    public static int innerTextLength(Node node)
    {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                return node.getNodeValue().length();
            case Node.ELEMENT_NODE:
                int length = 0;
                Node child = node.getFirstChild();
                while (child != null) {
                    length += innerTextLength(child);
                    child = child.getNextSibling();
                }
                return length;
            case Node.DOCUMENT_NODE:
                return innerTextLength(((org.w3c.dom.Document) node).getDocumentElement());
            default:
                return 0;
        }
    }

    /**
     * Extracts the first characters of the given XML fragment, up to the given length limit, adding only characters in
     * XML text nodes. The XML fragment is cleaned up before extracting the prefix to be sure that the result is
     * well-formed.
     * 
     * @param xmlFragment the full XML text
     * @param previewLength the maximum number of characters allowed in the preview, considering only the XML text nodes
     * @return a prefix of the given XML fragment summing at most <code>previewLength</code> characters in its text
     *         nodes
     */
    public static String getXMLPreview(String xmlFragment, int previewLength)
    {
        try {
            return XMLUtils.extractXML(tidy(xmlFragment, TIDY_XML_CONFIG), 0, previewLength);
        } catch (RuntimeException e) {
            return getPlainPreview(xmlFragment, previewLength);
        }
    }

    /**
     * Extracts the first characters of the given HTML fragment, up to the given length limit, adding only characters in
     * HTML text nodes. The HTML fragment is cleaned up before extracting the prefix to be sure the result is
     * well-formed.
     * 
     * @param htmlFragment the full HTML text
     * @param previewLength the maximum number of characters allowed in the preview, considering only the HTML text
     *            nodes
     * @return a prefix of the given HTML fragment summing at most <code>previewLength</code> characters in its text
     *         nodes
     */
    public static String getHTMLPreview(String htmlFragment, int previewLength)
    {
        try {
            org.w3c.dom.Document html = tidy(htmlFragment, TIDY_HTML_CONFIG);
            Node body = html.getElementsByTagName("body").item(0);
            return XMLUtils.extractXML(body.getFirstChild(), 0, previewLength);
        } catch (RuntimeException e) {
            return getPlainPreview(htmlFragment, previewLength);
        }
    }

    /**
     * Extracts the first characters of the given text, up to the last space within the given length limit.
     * 
     * @param plainText the full text
     * @param previewLength the maximum number of characters allowed in the preview
     * @return a prefix of the <code>plainText</code> having at most <code>previewLength</code> characters
     */
    public static String getPlainPreview(String plainText, int previewLength)
    {
        if (plainText.length() <= previewLength) {
            return plainText;
        }
        // We remove the leading and trailing spaces from the given text to avoid interfering
        // with the last space within the length limit
        plainText = plainText.trim();
        if (plainText.length() <= previewLength) {
            return plainText;
        }
        int spaceIndex = plainText.lastIndexOf(" ", previewLength);
        if (spaceIndex < 0) {
            spaceIndex = previewLength;
        }
        plainText = plainText.substring(0, spaceIndex);
        return plainText;
    }

    /**
     * Creates a new {@link SyndContent} instance for the given content type and value.
     * 
     * @param type content type
     * @param value the content
     * @return a new {@link SyndContent} instance
     */
    protected SyndContent getSyndContent(String type, String value)
    {
        SyndContent content = new SyndContentImpl();
        content.setType(type);
        content.setValue(value);
        return content;
    }

    /**
     * Casts the given object to a {@link Document} instance. The given object must be either a {@link Document}
     * instance already, a {@link XWikiDocument} instance or the full name of the document.
     * 
     * @param obj object to be casted
     * @param context the XWiki context
     * @return the document associated with the given object
     * @throws XWikiException if the given object is neither a {@link Document} instance, a {@link XWikiDocument}
     *             instance nor the full name of the document
     */
    protected Document castDocument(Object obj, XWikiContext context) throws XWikiException
    {
        if (obj instanceof Document) {
            return (Document) obj;
        } else if (obj instanceof XWikiDocument) {
            return ((XWikiDocument) obj).newDocument(context);
        } else if (obj instanceof String) {
            return context.getWiki().getDocument((String) obj, context).newDocument(context);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, "");
        }
    }
}
