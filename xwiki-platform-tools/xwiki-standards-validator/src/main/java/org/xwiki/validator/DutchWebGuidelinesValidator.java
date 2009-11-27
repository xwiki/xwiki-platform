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
        validateRpd1s1();
        validateRpd1s2();
        validateRpd1s3();
        validateRpd2s1();
        validateRpd2s2();
        validateRpd2s3();
        validateRpd2s4();
        validateRpd2s5();
        validateRpd2s6();
        validateRpd2s7();
        validateRpd2s8();
        validateRpd2s9();
        validateRpd3s1();

        return getErrors();
    }

    /**
     * Keep structure and design separate as much as possible: use HTML or XHTML for the structure of the site and CSS
     * for its design.
     */
    public void validateRpd1s1()
    {
        // Validating 1s1 means running XHTML validity tests.
        // TODO : use XHTML validator
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
            assertTrue(Type.ERROR, "rpd1s3.formSubmit", hasChildElement(formElement, "submit"));
        }
    }

    /**
     * Use HTML 4.01 or XHTML 1.0 according to the W3C specifications for the markup of websites.
     */
    public void validateRpd2s1()
    {
        // HTML Validation errors are checked by Rpd1s1
    }

    /**
     * Do not use any markup which is referred to as deprecated (outmoded) in the W3C specifications.
     */
    public void validateRpd2s2()
    {
        // HTML Validation errors are checked by Rpd1s1
    }

    /**
     * When modifying an existing website: only use the Transitional version of HTML 4.01 or XHTML 1.0 if it is not
     * possible or desirable to use the Strict version.
     */
    public void validateRpd2s3()
    {
        // Check doctype XHTML strict or HTML 4.01 strict
    }

    /**
     * When building a new website: only use the Strict version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s4()
    {
        // Check doctype XHTML strict or HTML 4.01 strict
    }

    /**
     * Do not use frames on websites. Therefore, also do not use the Frameset version of HTML 4.01 or XHTML 1.0.
     */
    public void validateRpd2s5()
    {
        // Usage of frameset doctype is forbidden
        if (document.getDoctype() != null) {
            assertFalse(Type.ERROR, "rpd2s5.framesetDoctype", 
                StringUtils.containsIgnoreCase(document.getDoctype().getPublicId(), "frameset"));
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
        // TODO: use CSS validator
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
        List<String> headings = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6");

        int previousLevel = 1;
        for (Node element : getElements(headings)) {
            int currentLevel = Integer.parseInt(element.getNodeName().substring(1));

            // Verify that we haven't jumped from h1 to h3.
            assertTrue(Type.ERROR, "rpd3s1.headings", currentLevel <= previousLevel + 1);
            previousLevel = currentLevel;
        }

        // <b> and <i> are not allowed.
        assertFalse(Type.ERROR, "rpd3s1.boldMarkup", containsElement("b"));
        assertFalse(Type.ERROR, "rpd3s1.italicMarkup", containsElement("i"));
    }
}
