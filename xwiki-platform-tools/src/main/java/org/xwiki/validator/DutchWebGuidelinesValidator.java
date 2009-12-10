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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.framework.AbstractDOMValidator;
import org.xwiki.validator.framework.NodeListIterable;

/**
 * Validator allowing to validate (X)HTML content against Dutch Web Guidelines.
 * <p>"There are internationally recognized agreements for creating web sites, known as 125 quality requirements
 * standards warrants a significantly better website. The Netherlands government has assembled these international
 * standards in a quality model called the Web Guidelines. This quality model comprises 125 quality requirements for the
 * benefit of better websites."
 * </p>
 * 
 * @version $Id$
 */
public class DutchWebGuidelinesValidator extends AbstractDOMValidator
{
    /**
     * Message resources.
     */
    private ResourceBundle messages = ResourceBundle.getBundle("DutchWebGuidelines");

    /**
     * Constructor.
     */
    public DutchWebGuidelinesValidator()
    {
        super();
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
        super.addError(errorType, line, column, messages.getString(key));
    }

    /**
     * Run the validator on the given {@link Document}.
     * 
     * @return results of the validation
     * @throws
     */
    public List<ValidationError> validate()
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

        return getErrors();
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
     * Build websites according to the ‘layered construction’ principle.
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
            assertTrue(Type.ERROR, "rpd1s3.inlineEventHandlers", ListUtils.intersection(getAttributeNames(linkElement),
                forbiddenAttributes).isEmpty());
        }

        // Form validation
        NodeListIterable formElements = getElements("form");

        for (Node formElement : formElements) {
            boolean validForm = false;

            if (hasChildElement(formElement, "submit")) {
                // Form contains a <submit> element.
                validForm = true;
            }

            for (Node input : getChildren(formElement, ELEM_INPUT)) {
                if (hasAttribute(input, ATTR_TYPE)) {
                    if (getAttributeValue(input, ATTR_TYPE).equals(SUBMIT)) {
                        // Form contains an <input type="submit" /> element.
                        validForm = true;
                    } else if (getAttributeValue(input, ATTR_TYPE).equals("image")) {
                        if (hasAttribute(input, ATTR_ALT) && !StringUtils.isEmpty(getAttributeValue(input, ATTR_ALT))) {
                            // Form contains an <input type="image" alt="action" /> element.
                            // See http://www.w3.org/TR/WCAG10-HTML-TECHS/#forms-graphical-buttons
                            validForm = true;
                        }
                    }
                }
            }

            assertTrue(Type.ERROR, "rpd1s3.formSubmit", validForm);
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
        assertFalse(Type.ERROR, "rpd2s3.noDoctype", document.getDoctype() == null);
    }

    /**
     * When building a new website: only use the Strict version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s4()
    {
        // This guideline cannot be automatically tested, however we check that a DOCTYPE has been specified.
        assertFalse(Type.ERROR, "rpd2s4.noDoctype", document.getDoctype() == null);
    }

    /**
     * Do not use frames on websites. Therefore, also do not use the Frameset version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s5()
    {
        // Usage of frameset doctype is forbidden
        if (document.getDoctype() != null) {
            assertFalse(Type.ERROR, "rpd2s5.framesetDoctype", StringUtils.containsIgnoreCase(document.getDoctype()
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
        // <sub> and <sup> are not allowed.
        assertFalse(Type.ERROR, "rpd3s9.sub", containsElement("sub"));
        assertFalse(Type.ERROR, "rpd3s9.sup", containsElement("sup"));
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
        assertTrue(Type.ERROR, "rpd6s1.doctype", StringUtils.containsIgnoreCase(document.getDoctype().getPublicId(),
            "html"));
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
        for (Node link : getElements(ELEM_LINK)) {

            // Look for images in the link.
            boolean hasNonEmptyAlt = false;
            for (Node child : getChildren(link, ELEM_IMG)) {
                if (!StringUtils.isEmpty(getAttributeValue(child, ATTR_ALT))) {
                    hasNonEmptyAlt = true;
                }
            }

            // Look for text in the link.
            boolean hasText = false;
            for (Node linkChild : new NodeListIterable(link.getChildNodes())) {
                if (linkChild.getNodeType() == Node.TEXT_NODE) {
                    hasText = true;
                }
            }

            // Images in links must have a not empty alt attribute if there's no text in the link.
            assertTrue(Type.ERROR, "rpd7s4.links", hasNonEmptyAlt || hasText);
        }
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
}
