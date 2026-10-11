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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.validator.ValidationError.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: testRpd2s5FramesetDoctype - JSoup does not handle the doctype.
// TODO: testRpd2s5FramesetTag - JSoup does not handle the <frameset> tag
// TODO: testRpd2s5FrameTag - JSoup does not handle <frame> tags
// TODO: testRpd3s13BulletList - difficult to do with JSoup
// TODO: testRpd3s13DashList - difficult to do with JSoup
// TODO: testRpd3s13NumberedList - difficult to do with JSoup
class HTML5DutchWebGuidelinesValidatorTest
{
    private HTML5DutchWebGuidelinesValidator validator;

    @BeforeEach
    void beforeEach()
    {
        this.validator = new HTML5DutchWebGuidelinesValidator();
    }

    private void setValidatorDocument(InputStream document)
    {
        this.validator.setDocument(document);
    }

    private void setValidatorDocument(String content)
    {
        this.validator.setDocument(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        if (this.validator.getHTML5Document() != null) {
            this.validator.clear();
        }
    }

    private String getErrors(HTML5DutchWebGuidelinesValidator validator)
    {
        List<String> errors = new ArrayList<>(validator.getErrors().size());
        for (ValidationError error : validator.getErrors()) {
            errors.add(error.toString());
        }

        return StringUtils.join(errors, '\n');
    }

    private boolean isValid(HTML5DutchWebGuidelinesValidator validator)
    {
        boolean isValid = true;

        for (ValidationError error : validator.getErrors()) {
            if (error.getType() != Type.WARNING) {
                isValid = false;
                break;
            }
        }

        return isValid;
    }

    // All

    @Test
    void testValid()
    {
        setValidatorDocument(getClass().getResourceAsStream("/html5-valid.html"));
        this.validator.validate();

        for (ValidationError error : this.validator.getErrors()) {
            System.err.println(error);
        }

        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testInvalid()
    {
        setValidatorDocument(getClass().getResourceAsStream("/html5-invalid.html"));
        this.validator.validate();

        List<ValidationError> errors = this.validator.getErrors();

        assertEquals(8, errors.size());
    }

    // RPD 1s3
    @ParameterizedTest
    @CsvSource({
        "<a href='test'>test</a>",
        "<form><fieldset><input type='submit' /></fieldset></form>",
        "<form><fieldset><input type='image' alt='submit' /></fieldset></form>"
    })
    void testRpd1s3(String document)
    {
        setValidatorDocument(document);
        this.validator.validateRpd1s3();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd1s3LinkJavascript()
    {
        setValidatorDocument("<a href='javascript:'>test</a>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd1s3LinkEventHandlers()
    {
        setValidatorDocument("<a href='' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<a href='#' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<a href='test' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd1s3FormValidSubmitButton()
    {
        setValidatorDocument("<form><fieldset><button type='submit'>Go</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<form><fieldset><button>Go</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd1s3FormInvalidImageInput()
    {
        setValidatorDocument("<form><fieldset><input type='image' alt='' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd1s3FormNoSubmit()
    {
        setValidatorDocument("<form></form>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<form><fieldset><input type='text' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<form><fieldset><button type='reset'>Reset</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 2s3

    @Test
    void testRpd2s3NoDoctype()
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd2s3();
        // There is no doctype in HTML5
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 2s4

    @Test
    void testRpd2s4NoDoctype()
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd2s4();
        // Not valid in HTML5
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 2s5

    @Test
    void testRpd2s5ValidDoctype()
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        this.validator.validateRpd2s5();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s1

    @Test
    void testRpd3s1BoldMarkup()
    {
        setValidatorDocument("<p><b></b></p>");
        this.validator.validateRpd3s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd3s1ItalicMarkup()
    {
        setValidatorDocument("<p><i></i></p>");
        this.validator.validateRpd3s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s2

    @Test
    void testRpd3s2NoHeading()
    {
        setValidatorDocument("<body></body>");
        this.validator.validateRpd3s2();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s3

    @Test
    void testRpd3s3HeadingsValid()
    {
        setValidatorDocument("<body><h1></h1><h2></h2><h2></h2><h3></h3></body>");
        this.validator.validateRpd3s3();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd3s3HeadingsMissingLevel()
    {
        setValidatorDocument("<body><h1></h1><h3></h3></body>");
        this.validator.validateRpd3s3();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s4

    @Test
    void testRpd3s4ValidParagraphs()
    {
        setValidatorDocument("<body><p>content<br/>content<br/>content<br/></p>"
            + "<p>content<br/>content<br/>content<br/></p><p>content<br/>content<br/>content<br/></p></body>");
        this.validator.validateRpd3s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd3s4MissingParagraph()
    {
        // Consecutive line breaks.
        setValidatorDocument("<body><p>content<br/><br/>content</p></body>");
        this.validator.validateRpd3s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        // Consecutive line breaks separated by white spaces.
        setValidatorDocument("<body><p>content<br/>   <br/>content</p></body>");
        this.validator.validateRpd3s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s5

    @Test
    void testRpd3s5InvalidMarkup()
    {
        setValidatorDocument("<body><p><b>bold</b></p></body>");
        this.validator.validateRpd3s5();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><p><i>italic</i></p></body>");
        this.validator.validateRpd3s5();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 3s9

    @Test
    void testRpd3s9Sub()
    {
        setValidatorDocument("<body><p><sub>sub</sub></p></body>");
        this.validator.validateRpd3s9();
        assertEquals("WARNING: The use of <sub> is not recommended.", getErrors(this.validator));
        assertTrue(isValid(this.validator));
    }

    @Test
    void testRpd3s9Sup()
    {
        setValidatorDocument("<body><p><sup>sup</sup></p></body>");
        this.validator.validateRpd3s9();
        assertEquals("WARNING: The use of <sup> is not recommended.", getErrors(this.validator));
        assertTrue(isValid(this.validator));
    }

    // RPD 3s11

    @Test
    void testRpd3s11Quotation()
    {
        setValidatorDocument("<body><p><q>quotation</q></p></body>");
        this.validator.validateRpd3s11();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    // RPD 6s1

    @Test
    void testRpd6s1Doctypes()
    {
        setValidatorDocument("<!DOCTYPE html><html></html>");
        this.validator.validateRpd6s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        this.validator.validateRpd6s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd7s1ValidAlts()
    {
        setValidatorDocument("<body><img alt='' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><area alt='' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><input alt='' type='image' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd7s1MissingAlts()
    {
        setValidatorDocument("<body><img /></body>");
        this.validator.validateRpd7s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><area /></body>");
        this.validator.validateRpd7s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><input type='image' /></body>");
        this.validator.validateRpd7s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd7s4ImagesInLinks()
    {
        setValidatorDocument("<body><a><img alt=''/></a></body>");
        this.validator.validateRpd7s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a><img alt=''/>text</a></body>");
        this.validator.validateRpd7s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a><img alt='text' /></a></body>");
        this.validator.validateRpd7s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a><span>text</span></a></body>");
        this.validator.validateRpd7s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a><img/><span>text</span></a></body>");
        this.validator.validateRpd7s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a><span/></a></body>");
        this.validator.validateRpd7s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd7s5ImageMaps()
    {
        setValidatorDocument("<body><img alt='' usemap='#map' /></body>");
        this.validator.validateRpd7s5();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='' /></map></body>");
        this.validator.validateRpd7s5();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='text' /></map></body>");
        this.validator.validateRpd7s5();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd8s1Links()
    {
        setValidatorDocument("<body><a>to get the resource, click here</a></body>");
        this.validator.validateRpd8s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a>resource</a></body>");
        this.validator.validateRpd8s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd8s11Accesskeys()
    {
        setValidatorDocument("<body><a accesskey='a'></a></body>");
        this.validator.validateRpd8s11();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a accesskey='8'></a></body>");
        this.validator.validateRpd8s11();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd8s14Links()
    {
        setValidatorDocument("<body><a target='any'></a></body>");
        this.validator.validateRpd8s14();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a onclick='window.open'></a></body>");
        this.validator.validateRpd8s14();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd8s16Links()
    {
        setValidatorDocument("<body><a href='mailto:text@text.com?subject=foobar'>text@text.com</a></body>");
        this.validator.validateRpd8s16();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a href='mailto:text@text.com'>mail</a></body>");
        this.validator.validateRpd8s16();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd8s17Links()
    {
        setValidatorDocument("<body><a href='mailto:text@text.com?subject=foobar'>text@text.com</a></body>");
        this.validator.validateRpd8s17();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><a href='mailto:text@text.com'>text@text.com</a></body>");
        this.validator.validateRpd8s17();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd9s1StyleAttr()
    {
        setValidatorDocument("<body><div style='test'></div></body>");
        this.validator.validateRpd9s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd9s1StyleTag()
    {
        setValidatorDocument("<body><style></style></body>");
        this.validator.validateRpd9s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd11s2Table()
    {
        setValidatorDocument("<body><table></table></body>");
        this.validator.validateRpd11s2();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><table><th/></table></body>");
        this.validator.validateRpd11s2();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd11s4Table()
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        this.validator.validateRpd11s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        this.validator.validateRpd11s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        this.validator.validateRpd11s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd11s5Table()
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        this.validator.validateRpd11s5();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        this.validator.validateRpd11s5();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        this.validator.validateRpd11s5();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd12s1Iframe()
    {
        setValidatorDocument("<body><iframe/></body>");
        this.validator.validateRpd12s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><iframe title='an youtube video iframe'/></body>");
        this.validator.validateRpd12s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><iframe role='an youtube video iframe'/></body>");
        this.validator.validateRpd12s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><iframe title='an youtube video iframe' role='video tutorial'/></body>");
        this.validator.validateRpd12s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd13s1Label()
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><form><input name='test' id='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><form><label></label><input name='test' id='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument(
            "<body><form><label for='test' /><input name='test' id='test' /><input type='button'/></form></body>");
        this.validator.validateRpd13s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><form><label for='test' /><input name='test' id='test' /><input type='button'/>"
            + "<input type='hidden' /><input type='image' /><input type='reset' /><input type='submit' /></form></body>");
        this.validator.validateRpd13s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd13s4Submits()
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument(
            "<body><form><fieldset><input name='test' /><input type='submit' /></fieldset></form></body>");
        this.validator.validateRpd13s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument(
            "<body><form><fieldset><input name='test' /><button type='submit' /></fieldset></form></body>");
        this.validator.validateRpd13s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><form><fieldset><input name='test' /><button/></fieldset></form></body>");
        this.validator.validateRpd13s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd13s18Reset()
    {
        setValidatorDocument("<body><form><input type='reset' /></form></body>");
        this.validator.validateRpd13s18();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd15s6Language()
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd15s6();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<html lang='en'></html>");
        this.validator.validateRpd15s6();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd16s1Charset()
    {
        setValidatorDocument("<html><head></head></html>");
        this.validator.validateRpd16s1();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument(
            "<html><head><meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s1();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd16s2Charset()
    {
        setValidatorDocument("<html><head></head></html>");
        this.validator.validateRpd16s2();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument("<html><head>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s2();
        assertFalse(isValid(this.validator), getErrors(this.validator));
    }

    @Test
    void testRpd16s4CharsetPosition()
    {
        setValidatorDocument("<html><head><meta/>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s4();
        assertFalse(isValid(this.validator), getErrors(this.validator));

        setValidatorDocument(
            "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /><meta/></head></html>");
        this.validator.validateRpd16s4();
        assertTrue(isValid(this.validator), getErrors(this.validator));
    }
}
