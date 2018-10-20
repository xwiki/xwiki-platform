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
package org.xwiki.validator;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.xpath.XPathConstants;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.framework.AbstractDOMValidator;
import org.xwiki.validator.framework.NodeListIterable;

/**
 * Validator allowing to validate (X)HTML content against Dutch Web Guidelines.
 * <p>
 *"There are internationally recognized agreements for creating web sites, known as 125 quality requirements standards
 * warrants a significantly better website. The Netherlands government has assembled these international standards in a
 * quality model called the Web Guidelines. This quality model comprises 125 quality requirements for the benefit of
 * better websites."
 * </p>
 * 
 * @version $Id$
 */
public class DutchWebGuidelinesValidator extends AbstractDOMValidator
{
    /**
     * Utility selector.
     */
    private static final String CONTENT_TYPE_META_SELECTOR = "//meta[@http-equiv='Content-Type']";

    /**
     * String used to identify the charset in the content-type meta.
     */
    private static final String CONTENT_CHARSET_FRAGMENT = "charset=";

    /**
     * Character used to mark the beginning of the query string in a URL.
     */
    private static final String QUERY_STRING_SEPARATOR = "?";

    /**
     * Message resources.
     */
    private ResourceBundle messages = ResourceBundle.getBundle("DutchWebGuidelines");

    /**
     * Constructor.
     */
    public DutchWebGuidelinesValidator()
    {
        super(false);
    }

    @Override
    public String getName()
    {
        return "Dutch Web Guidelines";
    }

    /**
     * Add an error to the list of errors using our message resources.
     * 
     * @param errorType type of the error
     * @param line line where the error occurred
     * @param column where the error occurred
     * @param key key to retrieve the value from in the message properties
     */
    @Override
    protected void addError(Type errorType, int line, int column, String key)
    {
        super.addError(errorType, line, column, this.messages.getString(key));
    }

    @Override
    protected void validate(Document document)
    {
        // RPD 1
        validateRpd1s1();
        validateRpd1s2();
        validateRpd1s3();

        // RPD 2
        validateRpd2s1();
        validateRpd2s2();
        validateRpd2s3();
        validateRpd2s4();
        validateRpd2s5();
        validateRpd2s6();
        validateRpd2s7();
        validateRpd2s8();
        validateRpd2s9();

        // RPD 3
        validateRpd3s1();
        validateRpd3s2();
        validateRpd3s3();
        validateRpd3s4();
        validateRpd3s5();
        validateRpd3s6();
        validateRpd3s7();
        validateRpd3s8();
        validateRpd3s9();
        validateRpd3s10();
        validateRpd3s11();
        validateRpd3s12();
        validateRpd3s13();
        validateRpd3s14();
        validateRpd3s15();

        // RPD 4
        validateRpd4s1();
        validateRpd4s2();
        validateRpd4s3();
        validateRpd4s4();
        validateRpd4s5();
        validateRpd4s6();
        validateRpd4s7();

        // RPD 5
        validateRpd5s1();

        // RPD 6
        validateRpd6s1();
        validateRpd6s2();

        // RPD 7
        validateRpd7s1();
        validateRpd7s2();
        validateRpd7s3();
        validateRpd7s4();
        validateRpd7s5();
        validateRpd7s6();
        validateRpd7s7();

        // RPD 8
        validateRpd8s1();
        validateRpd8s2();
        validateRpd8s3();
        validateRpd8s4();
        validateRpd8s5();
        validateRpd8s6();
        validateRpd8s7();
        validateRpd8s8();
        validateRpd8s9();
        validateRpd8s10();
        validateRpd8s11();
        validateRpd8s12();
        validateRpd8s13();
        validateRpd8s14();
        validateRpd8s15();
        validateRpd8s16();
        validateRpd8s17();
        validateRpd8s18();
        validateRpd8s19();
        validateRpd8s20();
        validateRpd8s21();
        validateRpd8s22();
        validateRpd8s23();

        // RPD 9
        validateRpd9s1();
        validateRpd9s2();

        // RPD 10
        validateRpd10s1();
        validateRpd10s2();
        validateRpd10s3();

        // RPD 11
        validateRpd11s1();
        validateRpd11s2();
        validateRpd11s3();
        validateRpd11s4();
        validateRpd11s5();
        validateRpd11s6();
        validateRpd11s7();
        validateRpd11s8();
        validateRpd11s9();
        validateRpd11s10();

        // RPD 12
        validateRpd12s1();

        // RPD 13
        validateRpd13s1();
        validateRpd13s2();
        validateRpd13s3();
        validateRpd13s4();
        validateRpd13s5();
        validateRpd13s6();
        validateRpd13s7();
        validateRpd13s8();
        validateRpd13s9();
        validateRpd13s10();
        validateRpd13s11();
        validateRpd13s12();
        validateRpd13s13();
        validateRpd13s14();
        validateRpd13s15();
        validateRpd13s16();
        validateRpd13s17();
        validateRpd13s18();

        // RPD 14
        validateRpd14s1();

        // RPD 15
        validateRpd15s1();
        validateRpd15s2();
        validateRpd15s3();
        validateRpd15s4();
        validateRpd15s5();
        validateRpd15s6();
        validateRpd15s7();

        // RPD 16
        validateRpd16s1();
        validateRpd16s2();
        validateRpd16s3();
        validateRpd16s4();

        // RPD 18
        validateRpd18s1();
        validateRpd18s2();

        // RPD 22
        validateRpd22s1();
        validateRpd22s2();
        validateRpd22s3();
        validateRpd22s4();
        validateRpd22s5();
        validateRpd22s6();
        validateRpd22s7();
        validateRpd22s8();
        validateRpd22s9();
        validateRpd22s10();
    }

    /**
     * Keep structure and design separate as much as possible: use HTML or XHTML for the structure of the site and CSS
     * for its design.
     */
    public void validateRpd1s1()
    {
        // HTML Validation errors are checked by XHTMLValidator.
    }

    /**
     * Build websites according to the "layered construction" principle.
     */
    public void validateRpd1s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not make the function of the website dependent on optional technology, such as CSS and client-side script:
     * optional technology should complement the information on the site and its use, and should not interfere with
     * access to it if this technology is not supported.
     */
    public void validateRpd1s3()
    {
        // Links validation.
        NodeListIterable linkElements = getElements("a");

        // Links must not use javascript in href.
        for (String hrefValue : getAttributeValues(linkElements, "href")) {
            assertFalse(Type.ERROR, "rpd1s3.javascript", hrefValue.startsWith("javascript:"));
        }

        // Links must not use the attributes listed below.
        List<String> forbiddenAttributes =
            Arrays.asList(ATTR_BLUR, ATTR_CHANGE, ATTR_CLICK, ATTR_FOCUS, ATTR_LOAD, ATTR_MOUSEOVER, ATTR_SELECT,
                ATTR_SELECT, ATTR_UNLOAD);
        for (Node linkElement : linkElements) {
            if (!ListUtils.intersection(getAttributeNames(linkElement), forbiddenAttributes).isEmpty()) {
                assertFalse(Type.ERROR, "rpd1s3.inlineEventHandlers", getAttributeValue(linkElement, ATTR_HREF).equals(
                    "")
                    || getAttributeValue(linkElement, ATTR_HREF).equals("#"));
            }
        }

        // Form validation
        NodeListIterable formElements = getElements("form");

        for (Node formElement : formElements) {
            // Look for either a submit input or an image input with the 'alt' attribute specified.
            // See http://www.w3.org/TR/WCAG10-HTML-TECHS/#forms-graphical-buttons
            String inputSubmitOrImage = "//input[@type = 'submit' or (@type = 'image' and @alt != '')]";
            // The default value of the type attribute of a button element is 'submit'.
            // See http://www.w3.org/TR/xhtml1/dtds.html#dtdentry_xhtml1-strict.dtd_button
            String buttonSubmit = "//button[not(@type) or @type = 'submit']";
            assertTrue(Type.ERROR, "rpd1s3.formSubmit",
                (Boolean) evaluate(formElement, inputSubmitOrImage + " | " + buttonSubmit, XPathConstants.BOOLEAN));
        }
    }

    /**
     * Use HTML 4.01 or XHTML 1.0 according to the W3C specifications for the markup of websites.
     */
    public void validateRpd2s1()
    {
        // HTML Validation errors are checked by XHTMLValidator.
    }

    /**
     * Do not use any markup which is referred to as deprecated (outmoded) in the W3C specifications.
     */
    public void validateRpd2s2()
    {
        // HTML Validation errors are checked by XHTMLValidator.
    }

    /**
     * When modifying an existing website: only use the Transitional version of HTML 4.01 or XHTML 1.0 if it is not
     * possible or desirable to use the Strict version.
     */
    public void validateRpd2s3()
    {
        // This guideline cannot be automatically tested, however we check that a DOCTYPE has been specified.
        assertFalse(Type.ERROR, "rpd2s3.noDoctype", this.document.getDoctype() == null);
    }

    /**
     * When building a new website: only use the Strict version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s4()
    {
        // This guideline cannot be automatically tested, however we check that a DOCTYPE has been specified.
        assertFalse(Type.ERROR, "rpd2s4.noDoctype", this.document.getDoctype() == null);
    }

    /**
     * Do not use frames on websites. Therefore, also do not use the Frameset version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s5()
    {
        // Usage of frameset doctype is forbidden
        if (this.document.getDoctype() != null) {
            assertFalse(Type.ERROR, "rpd2s5.framesetDoctype", StringUtils.containsIgnoreCase(this.document.getDoctype()
                .getPublicId(), "frameset"));
        }

        // Usage of frameset is forbidden
        assertFalse(Type.ERROR, "rpd2s5.frameset", containsElement(ELEM_FRAMESET));

        // Usage of frames is forbidden
        assertFalse(Type.ERROR, "rpd2s5.frame", containsElement(ELEM_FRAME));
    }

    /**
     * Use CSS Level-2.1 according to the W3C specification for designing websites.
     */
    public void validateRpd2s6()
    {
        // CSS Validation errors are checked by CSSValidator.
    }

    /**
     * If client-side script is used, use ECMAScript according to the specification.
     */
    public void validateRpd2s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * If elements in the HTML hierarchy are to be manipulated, use the W3C DOM according to the specification.
     */
    public void validateRpd2s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Build a website according to the Web Content Accessibility Guidelines (WCAG 1.0) of the W3C.
     */
    public void validateRpd2s9()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Write both grammatically correct and descriptive markup.
     */
    public void validateRpd3s1()
    {
        // <b> and <i> are not allowed.
        assertFalse(Type.ERROR, "rpd3s1.boldMarkup", containsElement("b"));
        assertFalse(Type.ERROR, "rpd3s1.italicMarkup", containsElement("i"));
    }

    /**
     * Use markup for headings that express the hierarchy of information on the page.
     */
    public void validateRpd3s2()
    {
        NodeListIterable h1s = getElements(ELEM_H1);

        // A page must contain at least a h1.
        assertTrue(Type.ERROR, "rpd3s2.noheading", h1s.getNodeList().getLength() > 0);

        // It is recommended to use only one h1 per page.
        if (h1s.getNodeList().getLength() > 1) {
            addError(Type.WARNING, -1, -1, "rpd3s2.multipleh1");
        }
    }

    /**
     * Do not skip any levels in the hierarchy of headings in the markup.
     */
    public void validateRpd3s3()
    {
        List<String> headings = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6");

        int previousLevel = 1;
        for (Node element : getElements(headings)) {
            int currentLevel = Integer.parseInt(element.getNodeName().substring(1));

            // Verify that we haven't jumped from h1 to h3.
            assertTrue(Type.ERROR, "rpd3s3.headings", currentLevel <= previousLevel + 1);
            previousLevel = currentLevel;
        }
    }

    /**
     * Use the p (paragraph) element to indicate paragraphs. Do not use the br (linebreak) element to separate
     * paragraphs.
     */
    public void validateRpd3s4()
    {
        for (Node br : getElements(ELEM_BR)) {

            Node currentNode = br.getNextSibling();

            while (currentNode != null && currentNode.getNodeType() == Node.TEXT_NODE
                && StringUtils.isBlank(currentNode.getTextContent())) {
                // Ignore white spaces between <br/>.
                currentNode = currentNode.getNextSibling();
            }

            if (currentNode != null) {
                assertFalse(Type.ERROR, "rpd3s4.linebreaks", currentNode.getNodeName().equals(ELEM_BR));
            }
        }
    }

    /**
     * Use the em (emphasis) and strong elements to indicate emphasis.
     */
    public void validateRpd3s5()
    {
        // <b> and <i> are not allowed.
        String key = "rpd3s5.invalidMarkup";
        assertFalse(Type.ERROR, key, containsElement(ELEM_BOLD));
        assertFalse(Type.ERROR, key, containsElement(ELEM_ITALIC));
    }

    /**
     * Use the abbr (abbreviation) element for an abbreviation if confusion could arise concerning its meaning, if the
     * abbreviation plays a very important role in the text or if the abbreviation is not listed in the Dutch
     * dictionary.
     */
    public void validateRpd3s6()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use the dfn (definition) element to indicate terms that are defined elsewhere in a definition list.
     */
    public void validateRpd3s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use the ins (insertion) and del (deletion) elements to indicate regular changes in the content of a page.
     */
    public void validateRpd3s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Avoid using the sup (superscript) and sub (subscript) element if possible.
     */
    public void validateRpd3s9()
    {
        // <sub> and <sup> are not recommended.
        assertFalse(Type.WARNING, "rpd3s9.sub", containsElement("sub"));
        assertFalse(Type.WARNING, "rpd3s9.sup", containsElement("sup"));
    }

    /**
     * Use the cite element for references to people and titles.
     */
    public void validateRpd3s10()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Avoid using the q (quotation) element.
     */
    public void validateRpd3s11()
    {
        // <q> is not allowed.
        assertFalse(Type.ERROR, "rpd3s11.quotation", containsElement("q"));
    }

    /**
     * Use the blockquote element to indicate (long) quotations.
     */
    public void validateRpd3s12()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use ol (ordered list) and ul (unordered list) elements to indicate lists.
     */
    public void validateRpd3s13()
    {
        for (Node br : getElements(ELEM_BR)) {
            Node previousNode = null;
            String regex = "^\\s*(\\*|-|[0-9]\\.).*";

            for (Node currentNode : new NodeListIterable(br.getParentNode().getChildNodes())) {
                Node nextNode = currentNode.getNextSibling();

                if (previousNode != null && nextNode != null) {
                    boolean currentNodeMatches = currentNode.getNodeName().equals(ELEM_BR);
                    boolean previousNodeMatches =
                        previousNode.getNodeType() == Node.TEXT_NODE && previousNode.getTextContent().matches(regex);
                    boolean nextNodeMatches =
                        nextNode.getNodeType() == Node.TEXT_NODE && nextNode.getTextContent().matches(regex);

                    assertFalse(Type.ERROR, "rpd3s13.lists", previousNodeMatches && currentNodeMatches
                        && nextNodeMatches);
                }

                previousNode = currentNode;
            }
        }
    }

    /**
     * Use the dl (definition list), the dt (definition term) and dd (definition data) elements to indicate lists with
     * definitions.
     */
    public void validateRpd3s14()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Give meaningful names to id and class attributes.
     */
    public void validateRpd3s15()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Create unique, unchanging URLs.
     */
    public void validateRpd4s1()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Dynamically generated URLs should continue to refer to the same content if content is changed or added.
     */
    public void validateRpd4s2()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Avoid using sessions in URLs.
     */
    public void validateRpd4s3()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Provide redirection to the new location if information is moved.
     */
    public void validateRpd4s4()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Automatic redirection should be carried by the server if possible.
     */
    public void validateRpd4s5()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Use friendly URLs that are readable and recognizable.
     */
    public void validateRpd4s6()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * Set up a readable, expandable directory structure.
     */
    public void validateRpd4s7()
    {
        // URL guidelines can't be automatically tested, this validator allow to validate content only.
    }

    /**
     * In the event that important information is provided through a closed standard, the same information should also
     * be provided through an open standard.
     */
    public void validateRpd5s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Each HTML or XHTML document must begin with a valid doctype declaration.
     */
    public void validateRpd6s1()
    {
        assertTrue(Type.ERROR, "rpd6s1.doctype", StringUtils.containsIgnoreCase(this.document.getDoctype()
            .getPublicId(), ELEM_HTML));
    }

    /**
     * Put the content of the page in the HTML source code in order of importance.
     */
    public void validateRpd6s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * The alt (alternative) attribute should be used on every img (image) and area element and should be provided with
     * an effective alternative text.
     */
    public void validateRpd7s1()
    {
        // alt attributes are mandatory in <img>
        for (Node img : getElements(ELEM_IMG)) {
            assertTrue(Type.ERROR, "rpd7s1.img", hasAttribute(img, ATTR_ALT));
        }

        // alt attributes are mandatory in <area>
        for (Node area : getElements(ELEM_AREA)) {
            assertTrue(Type.ERROR, "rpd7s1.area", hasAttribute(area, ATTR_ALT));
        }

        // alt attributes are mandatory in <input type="image">
        for (Node input : getElements(ELEM_INPUT)) {
            if (getAttributeValue(input, ATTR_TYPE).equals(IMAGE)) {
                assertTrue(Type.ERROR, "rpd7s1.input", hasAttribute(input, ATTR_ALT));
            }
        }
    }

    /**
     * Do not use an alt attribute to display tooltips.
     */
    public void validateRpd7s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not use d-links on websites. Use of the longdesc (long description) attribute is preferred if the text
     * alternative on the alt attribute is inadequate for understanding the information in the image.
     */
    public void validateRpd7s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Images placed in a link should have a non-empty text alternative to enable visitors who do not see the image to
     * follow the link.
     */
    public void validateRpd7s4()
    {
        assertEmpty(Type.ERROR, "rpd7s4.links",
            "//a[normalize-space(.) = '']//img[not(@alt) or normalize-space(@alt) = '']");
    }

    /**
     * When using image maps, indicate an effective text alternative for both the img element and each area element by
     * means of the alt attribute.
     */
    public void validateRpd7s5()
    {
        // Non-empty alt attributes are mandatory in <img usemap=''>
        for (Node img : getElements(ELEM_IMG)) {
            if (hasAttribute(img, "usemap")) {
                assertFalse(Type.ERROR, "rpd7s5.img", StringUtils.isEmpty(getAttributeValue(img, ATTR_ALT)));
            }
        }

        // Non-empty alt attributes are mandatory in <area>
        for (Node area : getElements(ELEM_AREA)) {
            assertFalse(Type.ERROR, "rpd7s5.area", StringUtils.isEmpty(getAttributeValue(area, ATTR_ALT)));
        }
    }

    /**
     * Decorative images should be inserted via CSS as much as possible. Informative images should be inserted via HTML.
     */
    public void validateRpd7s6()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Applying CSS Image Replacement techniques to essential information is not recommended.
     */
    public void validateRpd7s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not describe the mechanism behind following a link.
     */
    public void validateRpd8s1()
    {
        List<String> forbiddenLinkTexts = Arrays.asList(messages.getString("rpd8s1.forbiddenLinkTexts").split(","));

        for (Node link : getElements(ELEM_LINK)) {
            for (Node linkChild : new NodeListIterable(link.getChildNodes())) {
                if (linkChild.getNodeType() == Node.TEXT_NODE) {
                    for (String forbiddenLinkText : forbiddenLinkTexts) {
                        assertFalse(Type.ERROR, "rpd8s1.link", StringUtils.containsIgnoreCase(linkChild
                            .getTextContent(), forbiddenLinkText));
                    }
                }
            }
        }
    }

    /**
     * Write clear, descriptive text for links.
     */
    public void validateRpd8s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use the minimum amount of text needed to understand where the link leads.
     */
    public void validateRpd8s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Provide sufficient information on the destination of a link to prevent unpleasant surprises for the visitor.
     */
    public void validateRpd8s4()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When using client-side script in combination with a link: make the script functionality an expansion of the basic
     * functionality of the link.
     */
    public void validateRpd8s5()
    {
        // Already checked by RPD 1s3
    }

    /**
     * When using client-side script in combination with a link: if the link does not lead to anything, do not confront
     * the visitor without support for client-side script with a non-working link.
     */
    public void validateRpd8s6()
    {
        // Already checked by RPD 1s3
    }

    /**
     * When using client-side script in combination with a link: if necessary, use client-side script as an expansion of
     * server-side functions.
     */
    public void validateRpd8s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Links must be easy to distinguish from other text.
     */
    public void validateRpd8s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Provide a logical order for the links on the page. Use the tabindex attribute to deviate from the standard tab
     * order for links if this order is inadequate for correct use of the page by keyboard users.
     */
    public void validateRpd8s9()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not make it impossible to tab to links. Do not remove the focus rectangle surrounding a link or the
     * possibility of focusing on a link.
     */
    public void validateRpd8s10()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Avoid using the Access key attribute. If the decision is nevertheless made to apply this attribute, only use it
     * on links that remain unchanged throughout the site (e.g. main navigation) and limit the shortcut key combinations
     * to numbers.
     */
    public void validateRpd8s11()
    {
        for (Node link : getElements(ELEM_LINK)) {
            if (hasAttribute(link, ATTR_ACCESSKEY)) {
                assertTrue(Type.ERROR, "rpd8s11.accesskey", StringUtils.isNumeric(getAttributeValue(link,
                    ATTR_ACCESSKEY)));
            }
        }
    }

    /**
     * Give blind visitors additional options to skip long lists of links.
     */
    public void validateRpd8s12()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * At the top of pages with many topics, provide a page index with links to navigate to the different topics.
     */
    public void validateRpd8s13()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Links on websites should not automatically open new windows without warning.
     */
    public void validateRpd8s14()
    {
        for (Node link : getElements(ELEM_LINK)) {
            // target attribute is forbidden
            assertFalse(Type.ERROR, "rpd8s14.target", hasAttribute(link, "target"));
            if (hasAttribute(link, ATTR_CLICK)) {
                // Usage of window.open is forbidden
                assertFalse(Type.ERROR, "rpd8s14.window", getAttributeValue(link, ATTR_CLICK).contains("window.open"));
            }
        }
    }

    /**
     * Do not open any new windows automatically, unless the location of the link contains useful information that may
     * be necessary during an important uninterruptible process.
     */
    public void validateRpd8s15()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Links to e-mail addresses: the e-mail address to which the message is addressed must be visible in the link text.
     */
    public void validateRpd8s16()
    {
        for (Node link : getElements(ELEM_LINK)) {
            String href = getAttributeValue(link, ATTR_HREF);
            if (href != null && href.startsWith(MAILTO)) {
                String email = StringUtils.substringAfter(href, MAILTO);
                if (email.contains(QUERY_STRING_SEPARATOR)) {
                    email = StringUtils.substringBefore(email, QUERY_STRING_SEPARATOR);
                }
                assertTrue(Type.ERROR, "rpd8s16.email", link.getTextContent().contains(email));
            }
        }
    }

    /**
     * Links to e-mail addresses: the URL in the href attribute of a link to an e-mail address may only contain the
     * mailto protocol and an e-mail address.
     */
    public void validateRpd8s17()
    {
        for (Node link : getElements(ELEM_LINK)) {
            String href = getAttributeValue(link, ATTR_HREF);
            if (href != null && href.startsWith(MAILTO)) {
                String email = StringUtils.substringAfter(href, MAILTO);
                assertTrue(Type.ERROR, "rpd8s17.email", email
                    .matches("^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}$"));
            }
        }
    }

    /**
     * Do not apply any technical measures to the website to hide an e-mail address from spam robots.
     */
    public void validateRpd8s18()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Be extremely cautious when publishing e-mail addresses of visitors to the website. Inform the visitor of which
     * information will be published on the site, or do not publish the visitor's e-mail address.
     */
    public void validateRpd8s19()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When presenting downloadable files, inform the visitor how to download and then use them.
     */
    public void validateRpd8s20()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Serve files with the correct MIME type.
     */
    public void validateRpd8s21()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not automatically open links to downloadable files in a new window.
     */
    public void validateRpd8s22()
    {
        // Duplicate of 8s14
    }

    /**
     * Do not intentionally serve downloadable files with an unknown or incorrect MIME type to force the browser to do
     * something.
     */
    public void validateRpd8s23()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * CSS should be placed in linked files and not mixed with the HTML source code.
     */
    public void validateRpd9s1()
    {
        String exprString = "//@style";
        assertFalse(Type.ERROR, "rpd9s1.attr", ((Boolean) evaluate(getElement(ELEM_BODY), exprString,
            XPathConstants.BOOLEAN)));
        assertFalse(Type.ERROR, "rpd9s1.tag",
            getChildren(getElement(ELEM_BODY), "style").getNodeList().getLength() > 0);
    }

    /**
     * Pages should remain usable if a web browser does not support CSS.
     */
    public void validateRpd9s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Make sure that the meaning of communicative elements is not expressed only through colour.
     */
    public void validateRpd10s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Be consistent with colour use when indicating meaning.
     */
    public void validateRpd10s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Make sure there is sufficient brightness contrast between the text and the background colour.
     */
    public void validateRpd10s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use tables to display relational information and do not use them for layout.
     */
    public void validateRpd11s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use the th (table header) to describe a column or row in a table with relational information.
     */
    public void validateRpd11s2()
    {
        for (Node table : getElements(ELEM_TABLE)) {
            assertTrue(Type.ERROR, "rpd11s2.th", getChildrenTagNames(table).contains(ELEM_TH));
        }
    }

    /**
     * Group rows with only th (table header) cells with the thead (table head) element. Group the rest of the table
     * with the tbody (table body) element.
     */
    public void validateRpd11s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * @param table Table to analyze
     * @return true if the table contains th with ids and td
     */
    private boolean hasTableHeadersAndIds(Node table)
    {
        boolean hasHeadersAndIds = false;

        String exprString = "//td[@headers]";
        hasHeadersAndIds = (Boolean) evaluate(table, exprString, XPathConstants.BOOLEAN);
        exprString = "//th[@id]";
        hasHeadersAndIds = hasHeadersAndIds && (Boolean) evaluate(table, exprString, XPathConstants.BOOLEAN);

        return hasHeadersAndIds;
    }

    /**
     * Use the scope attribute to associate table labels (th cells) with columns or rows.
     */
    public void validateRpd11s4()
    {
        for (Node table : getElements(ELEM_TABLE)) {
            boolean hasHeadersAndIds = hasTableHeadersAndIds(table);

            if (!hasHeadersAndIds) {
                for (Node th : getChildren(table, ELEM_TH)) {
                    assertTrue(Type.ERROR, "rpd11s4.scope", hasAttribute(th, ATTR_SCOPE));
                }
            }
        }
    }

    /**
     * Use the headers and id attributes to associate table labels (th cells) with individual cells in complex tables.
     */
    public void validateRpd11s5()
    {
        for (Node table : getElements(ELEM_TABLE)) {

            boolean hasScope = false;

            for (Node th : getChildren(table, ELEM_TH)) {
                if (hasAttribute(th, ATTR_SCOPE)) {
                    hasScope = true;
                }
            }

            if (!hasScope) {
                assertTrue(Type.ERROR, "rpd11s5.headers", hasTableHeadersAndIds(table));
            }
        }
    }

    /**
     * Provide abbreviations for table labels (th cells) by means of the abbr (abbreviation) attribute if the content of
     * the table label is so long that repetition in a speech browser could cause irritation.
     */
    public void validateRpd11s6()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use the caption element or heading markup to provide a heading above a table.
     */
    public void validateRpd11s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When modifying an existing website: use CSS for the presentation and layout of web pages, and avoid using tables
     * for layout.
     */
    public void validateRpd11s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When using tables for layout: do not use more than one table and use CSS for the design of this table as much as
     * possible.
     */
    public void validateRpd11s9()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When using tables for layout: do not apply any accessibility markup.
     */
    public void validateRpd11s10()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not use frames on websites. This applies to regular frames in framesets as well as iframes.
     */
    public void validateRpd12s1()
    {
        // Usage of iframes is forbidden
        // frameset and frame tags are checked in RPD2.5.
        assertFalse(Type.ERROR, "rpd12s1.iframe", containsElement(ELEM_IFRAME));
    }

    /**
     * Use the label element to explicitly associate text with an input field in a form.
     */
    public void validateRpd13s1()
    {
        String message = "rpd13s1.label";
        // type = text|password|checkbox|radio|submit|reset|file|hidden|image|button
        List<String> inputWithoutLabels = Arrays.asList(SUBMIT, RESET, IMAGE, BUTTON, HIDDEN);

        for (Node input : getElements(ELEM_INPUT)) {
            // Some inputs doesn't need a label.
            if (!inputWithoutLabels.contains(getAttributeValue(input, ATTR_TYPE))) {

                // Labelled inputs must have an ID.
                String id = getAttributeValue(input, ATTR_ID);
                assertFalse(Type.ERROR, message, id == null);

                if (id != null) {
                    // Looking for the label associated to the input.
                    String exprString = "//label[@for='" + id + "']";
                    assertTrue(Type.ERROR, message, (Boolean) evaluate(this.document, exprString,
                        XPathConstants.BOOLEAN));
                }
            }
        }
    }

    /**
     * Use the tabindex attribute to deviate from the standard tab order on form fields if this order is inadequate for
     * correct use of the form by keyboard users.
     */
    public void validateRpd13s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Apply grouping of input fields by means of the fieldset element.
     */
    public void validateRpd13s3()
    {
        // Display a warning if a form without fieldset is present.
        for (Node form : getElements(ELEM_FORM)) {
            if (!getChildrenTagNames(form).contains(ELEM_FIELDSET)) {
                addError(Type.WARNING, -1, -1, "rpd13s3.fieldset");
            }
        }
    }

    /**
     * Avoid automatic redirection during interaction with forms.
     */
    public void validateRpd13s4()
    {
        for (Node form : getElements(ELEM_FORM)) {
            boolean hasSubmit = false;
            boolean hasDynamicSelect = false;

            String exprString = "//input[@type='submit']";
            hasSubmit = (Boolean) evaluate(form, exprString, XPathConstants.BOOLEAN);
            exprString = "//input[@type='image']";
            hasSubmit = hasSubmit || (Boolean) evaluate(this.document, exprString, XPathConstants.BOOLEAN);
            assertTrue(Type.ERROR, "rpd13s4.submit", hasSubmit);

            exprString = "//select[@onchange]";
            hasDynamicSelect = (Boolean) evaluate(form, exprString, XPathConstants.BOOLEAN);

            if (hasDynamicSelect) {
                addError(Type.WARNING, -1, -1, "rpd13s4.select");
            }
        }
    }

    /**
     * Do not use client-side script or forms as the only way of accessing information on the site.
     */
    public void validateRpd13s5()
    {
        for (Node form : getElements(ELEM_FORM)) {
            // Display a warning if the form has a "onsubmit" event handler.
            if (hasAttribute(form, ATTR_SUBMIT)) {
                addError(Type.WARNING, -1, -1, "rpd13s5.onsubmit");
            }

            // Display a warning if the form has a "onchange" event handler.
            if (hasAttribute(form, ATTR_CHANGE)) {
                addError(Type.WARNING, -1, -1, "rpd13s5.onchange");
            }
        }
    }

    /**
     * Do not confront a visitor with a non-working form if optional technologies "such as CSS or client-side script"
     * are not supported by the browser.
     */
    public void validateRpd13s6()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use CSS sparingly for input fields and form buttons.
     */
    public void validateRpd13s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * If a visitor has to provide personal data, let him know what will be done with this data, e.g. in the form of a
     * privacy statement.
     */
    public void validateRpd13s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not ask a visitor to provide more information by means of a form than necessary for the purpose of the form.
     * Keep forms as short as possible and limit the mandatory completion of form fields.
     */
    public void validateRpd13s9()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Indicate which fields are mandatory and which are optional.
     */
    public void validateRpd13s10()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Provide alternate contact options, such as address details, telephone number or e-mail addresses, if available.
     */
    public void validateRpd13s11()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Let the visitor know what will be done with the form when it is sent.
     */
    public void validateRpd13s12()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Give the visitor the option of saving his reply.
     */
    public void validateRpd13s13()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Once the visitor has completed and sent the form, send him confirmation that his message has been received by the
     * recipient (autoreply).
     */
    public void validateRpd13s14()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Before displaying complex forms, give the visitor an impression of the size of the form.
     */
    public void validateRpd13s15()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * List documents which the visitor might need while completing the form beforehand.
     */
    public void validateRpd13s16()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Provide forms with instructions for the visitor if necessary, particularly for the applicable input fields.
     */
    public void validateRpd13s17()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not add any reset buttons to forms.
     */
    public void validateRpd13s18()
    {
        String exprString = "//input[@type='reset']";
        assertFalse(Type.ERROR, "rpd13s18.reset", (Boolean) evaluate(this.document, exprString,
            XPathConstants.BOOLEAN));
    }

    /**
     * Do not use client-side script for essential functionality on web pages, unless any lack of support for these
     * scripts is sufficiently compensated by HTML alternatives and/or server-side script.
     */
    public void validateRpd14s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * The visitor should have the option of choosing between languages on every page of the site.
     */
    public void validateRpd15s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Links for language choice should have a clear and consistent place in the navigation of the site.
     */
    public void validateRpd15s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use fully written out (textual) links to the language versions.
     */
    public void validateRpd15s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Write links to language versions in their corresponding languages.
     */
    public void validateRpd15s4()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Do not use associations with nationalities for language choice.
     */
    public void validateRpd15s5()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Specify the base language of a page in the markup.
     */
    public void validateRpd15s6()
    {
        Node html = getElement(ELEM_HTML);

        // Check for lang attribute in th html node.
        assertTrue(Type.ERROR, "rpd15s6.lang", html != null && hasAttribute(html, "lang"));
    }

    /**
     * Indicate language variations in the content of pages in the markup.
     */
    public void validateRpd15s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Specify the character set for web pages.
     */
    public void validateRpd16s1()
    {
        NodeListIterable metas =
            new NodeListIterable((NodeList) evaluate(this.document, CONTENT_TYPE_META_SELECTOR,
                XPathConstants.NODESET));

        assertTrue(Type.ERROR, "rpd16s1.nometa", metas.getNodeList().getLength() > 0);

        for (Node meta : metas) {
            assertTrue(Type.ERROR, "rpd16s1.charset", StringUtils.containsIgnoreCase(getAttributeValue(meta,
                ATTR_CONTENT), CONTENT_CHARSET_FRAGMENT));
        }
    }

    /**
     * Specify the UTF-8 character set.
     */
    public void validateRpd16s2()
    {
        NodeListIterable metas =
            new NodeListIterable((NodeList) evaluate(this.document, CONTENT_TYPE_META_SELECTOR,
                XPathConstants.NODESET));

        assertTrue(Type.ERROR, "rpd16s2.nometa", metas.getNodeList().getLength() > 0);

        for (Node meta : metas) {
            String content = getAttributeValue(meta, ATTR_CONTENT);
            assertTrue(Type.ERROR, "rpd16s2.notutf8", StringUtils.containsIgnoreCase(content, "charset=utf-8"));
            assertTrue(Type.ERROR, "rpd16s2.differs", StringUtils.containsIgnoreCase(content, CONTENT_CHARSET_FRAGMENT
                + this.document.getXmlEncoding()));
        }
    }

    /**
     * Also specify the character set by means of HTTP headers, if possible.
     */
    public void validateRpd16s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use (at least) the meta element to specify the character set and place this element as high as possible in the
     * head section of the markup.
     */
    public void validateRpd16s4()
    {
        Node meta = getElement(ELEM_META);

        assertTrue(Type.ERROR, "rpd16s4.position", hasAttribute(meta, ATTR_CONTENT)
            && StringUtils.containsIgnoreCase(getAttributeValue(meta, ATTR_CONTENT), CONTENT_CHARSET_FRAGMENT));
    }

    /**
     * Use a unique, descriptive title for each page.
     */
    public void validateRpd18s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Write short, concise text, in which the main message is mentioned at the top of the page.
     */
    public void validateRpd18s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use language that the visitor understands: limit the use of jargon, difficult terms and abbreviations.
     */
    public void validateRpd22s1()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Give visitors an <em>escape route</em>: possibilities to continue if they get stuck. Escape routes include useful
     * links, being able to use the back button, a search function, and being able to correct input errors immediately.
     */
    public void validateRpd22s2()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Don't make visitors guess: provide information on how they can correct errors they have made. Take into account
     * the most common errors.
     */
    public void validateRpd22s3()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Make modified error pages "for errors such as dead links (404 Not Found)" where the visitor is given options for
     * continuing within the site.
     */
    public void validateRpd22s4()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * In the event of an error message as a result of sending a form, give the visitor the option of correcting the
     * error in the form immediately and don't make him be dependent on the use of the back button.
     */
    public void validateRpd22s5()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * When implementing a search engine on the website: use "smart" search technology that takes into account spelling
     * errors, similar search terms, terms in singular or plural form, etc.
     */
    public void validateRpd22s6()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Provide a well-organised list of the most relevant search results. If too many search results are provided, it
     * takes visitors too long to find the desired information. Give visitors the option of entering search criteria, or
     * sorting the search results.
     */
    public void validateRpd22s7()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Give visitors the option of reporting errors on the site.
     */
    public void validateRpd22s8()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Use colours, icons and textual explanations to draw the visitor's attention to an error message and explain the
     * problem.
     */
    public void validateRpd22s9()
    {
        // This guideline cannot be automatically tested.
    }

    /**
     * Give visitors the option of finding information in alternate ways. For example, by providing a sitemap, search
     * functions, or by means of a request by e-mail, letter or telephone.
     */
    public void validateRpd22s10()
    {
        // This guideline cannot be automatically tested.
    }
}
