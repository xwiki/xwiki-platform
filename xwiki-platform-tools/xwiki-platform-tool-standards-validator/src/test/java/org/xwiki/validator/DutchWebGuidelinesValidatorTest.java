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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.validator.ValidationError.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DutchWebGuidelinesValidatorTest
{
    private DutchWebGuidelinesValidator validator;

    @Before
    public void setUp() throws Exception
    {
        this.validator = new DutchWebGuidelinesValidator();
    }

    private void setValidatorDocument(InputStream document) throws Exception
    {
        this.validator.setDocument(document);
    }

    private void setValidatorDocument(String content) throws Exception
    {
        this.validator.setDocument(new ByteArrayInputStream(content.getBytes("UTF-8")));
        if (this.validator.getDocument() != null) {
            this.validator.clear();
        }
    }

    private String getErrors(DutchWebGuidelinesValidator validator)
    {
        List<String> errors = new ArrayList<>(validator.getErrors().size());
        for (ValidationError error : validator.getErrors()) {
            errors.add(error.toString());
        }

        return StringUtils.join(errors, '\n');
    }

    private boolean isValid(DutchWebGuidelinesValidator validator)
    {
        boolean isValid = true;

        for (ValidationError error : validator.getErrors()) {
            if (error.getType() != Type.WARNING) {
                isValid = false;
            }
        }

        return isValid;
    }

    // All

    @Test
    public void testValidate() throws Exception
    {
        setValidatorDocument(getClass().getResourceAsStream("/dwg-valid.html"));
        this.validator.validate();

        for (ValidationError error : this.validator.getErrors()) {
            System.err.println(error);
        }

        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 1s3

    @Test
    public void testRpd1s3LinkValid() throws Exception
    {
        setValidatorDocument("<a href='test'>test</a>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3LinkJavascript() throws Exception
    {
        setValidatorDocument("<a href='javascript:'>test</a>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3LinkEventHandlers() throws Exception
    {
        setValidatorDocument("<a href='' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<a href='#' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<a href='test' onclick=''></a>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3FormValidInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='submit' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3FormValidSubmitButton() throws Exception
    {
        setValidatorDocument("<form><fieldset><button type='submit'>Go</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<form><fieldset><button>Go</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3FormInvalidImageInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='image' alt='' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3FormValidImageInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='image' alt='submit' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd1s3FormNoSubmit() throws Exception
    {
        setValidatorDocument("<form></form>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<form><fieldset><input type='text' /></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<form><fieldset><button type='reset'>Reset</button></fieldset></form>");
        this.validator.validateRpd1s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 2s3

    @Test
    public void testRpd2s3NoDoctype() throws Exception
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd2s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 2s4

    @Test
    public void testRpd2s4NoDoctype() throws Exception
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd2s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 2s5

    @Test
    public void testRpd2s5ValidDoctype() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        this.validator.validateRpd2s5();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd2s5FramesetDoctype() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Frameset//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd'><html></html>");
        this.validator.validateRpd2s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd2s5FramesetTag() throws Exception
    {
        setValidatorDocument("<frameset></frameset>");
        this.validator.validateRpd2s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd2s5FrameTag() throws Exception
    {
        setValidatorDocument("<frame></frame>");
        this.validator.validateRpd2s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s1

    @Test
    public void testRpd3s1BoldMarkup() throws Exception
    {
        setValidatorDocument("<p><b></b></p>");
        this.validator.validateRpd3s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd3s1ItalicMarkup() throws Exception
    {
        setValidatorDocument("<p><i></i></p>");
        this.validator.validateRpd3s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s2

    @Test
    public void testRpd3s2NoHeading() throws Exception
    {
        setValidatorDocument("<body></body>");
        this.validator.validateRpd3s2();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s3

    @Test
    public void testRpd3s3HeadingsValid() throws Exception
    {
        setValidatorDocument("<body><h1></h1><h2></h2><h2></h2><h3></h3></body>");
        this.validator.validateRpd3s3();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd3s3HeadingsMissingLevel() throws Exception
    {
        setValidatorDocument("<body><h1></h1><h3></h3></body>");
        this.validator.validateRpd3s3();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s4

    @Test
    public void testRpd3s4ValidParagraphs() throws Exception
    {
        setValidatorDocument("<body><p>content<br/>content<br/>content<br/></p>"
            + "<p>content<br/>content<br/>content<br/></p><p>content<br/>content<br/>content<br/></p></body>");
        this.validator.validateRpd3s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd3s4MissingParagraph() throws Exception
    {
        // Consecutive line breaks.
        setValidatorDocument("<body><p>content<br/><br/>content</p></body>");
        this.validator.validateRpd3s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        // Consecutive line breaks separated by white spaces.
        setValidatorDocument("<body><p>content<br/>   <br/>content</p></body>");
        this.validator.validateRpd3s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s5

    @Test
    public void testRpd3s5InvalidMarkup() throws Exception
    {
        setValidatorDocument("<body><p><b>bold</b></p></body>");
        this.validator.validateRpd3s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><p><i>italic</i></p></body>");
        this.validator.validateRpd3s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s9

    @Test
    public void testRpd3s9Sub() throws Exception
    {
        setValidatorDocument("<body><p><sub>sub</sub></p></body>");
        this.validator.validateRpd3s9();
        assertEquals("WARNING: The use of <sub> is not recommended.", getErrors(validator));
        assertTrue(isValid(this.validator));
    }

    @Test
    public void testRpd3s9Sup() throws Exception
    {
        setValidatorDocument("<body><p><sup>sup</sup></p></body>");
        this.validator.validateRpd3s9();
        assertEquals("WARNING: The use of <sup> is not recommended.", getErrors(validator));
        assertTrue(isValid(this.validator));
    }

    // RPD 3s11

    @Test
    public void testRpd3s11Quotation() throws Exception
    {
        setValidatorDocument("<body><p><q>quotation</q></p></body>");
        this.validator.validateRpd3s11();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    // RPD 3s13

    @Test
    public void testRpd3s13BulletList() throws Exception
    {
        setValidatorDocument("<body> * item1<br/> * item2 <br/> * item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body>*item1<br/>*item2<br/>*item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(this.validator), isValid(this.validator));

    }

    @Test
    public void testRpd3s13DashList() throws Exception
    {
        setValidatorDocument("<body> - item1<br/> - item2 <br/> - item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body>-item1<br/>-item2 <br/>-item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd3s13NumberedList() throws Exception
    {
        setValidatorDocument("<body> 1. item1<br/> 2. item2 <br/> 3. item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body>1.item1<br/>2.item2<br/>3.item3</body>");
        this.validator.validateRpd3s13();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd6s1Doctypes() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//ORG//DTD FOO 1.0 Bar//EN' "
            + "'http://www.foo.bar/xhtml1-strict.dtd'><html></html>");
        this.validator.validateRpd6s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        this.validator.validateRpd6s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd7s1ValidAlts() throws Exception
    {
        setValidatorDocument("<body><img alt='' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><area alt='' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><input alt='' type='image' /></body>");
        this.validator.validateRpd7s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd7s1MissingAlts() throws Exception
    {
        setValidatorDocument("<body><img /></body>");
        this.validator.validateRpd7s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><area /></body>");
        this.validator.validateRpd7s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><input type='image' /></body>");
        this.validator.validateRpd7s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd7s4ImagesInLinks() throws Exception
    {
        setValidatorDocument("<body><a><img alt=''/></a></body>");
        this.validator.validateRpd7s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a><img alt=''/>text</a></body>");
        this.validator.validateRpd7s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a><img alt='text' /></a></body>");
        this.validator.validateRpd7s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd7s5ImageMaps() throws Exception
    {
        setValidatorDocument("<body><img alt='' usemap='#map' /></body>");
        this.validator.validateRpd7s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='' /></map></body>");
        this.validator.validateRpd7s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='text' /></map></body>");
        this.validator.validateRpd7s5();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd8s1Links() throws Exception
    {
        setValidatorDocument("<body><a>to get the resource, click here</a></body>");
        this.validator.validateRpd8s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a>resource</a></body>");
        this.validator.validateRpd8s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd8s11Accesskeys() throws Exception
    {
        setValidatorDocument("<body><a accesskey='a'></a></body>");
        this.validator.validateRpd8s11();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a accesskey='8'></a></body>");
        this.validator.validateRpd8s11();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd8s14Links() throws Exception
    {
        setValidatorDocument("<body><a target='any'></a></body>");
        this.validator.validateRpd8s14();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a onclick='window.open'></a></body>");
        this.validator.validateRpd8s14();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd8s16Links() throws Exception
    {
        setValidatorDocument("<body><a href='mailto:text@text.com?subject=foobar'>text@text.com</a></body>");
        this.validator.validateRpd8s16();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a href='mailto:text@text.com'>mail</a></body>");
        this.validator.validateRpd8s16();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd8s17Links() throws Exception
    {
        setValidatorDocument("<body><a href='mailto:text@text.com?subject=foobar'>text@text.com</a></body>");
        this.validator.validateRpd8s17();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><a href='mailto:text@text.com'>text@text.com</a></body>");
        this.validator.validateRpd8s17();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd9s1StyleAttr() throws Exception
    {
        setValidatorDocument("<body><div style='test'></div></body>");
        this.validator.validateRpd9s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd9s1StyleTag() throws Exception
    {
        setValidatorDocument("<body><style></style></body>");
        this.validator.validateRpd9s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd11s2Table() throws Exception
    {
        setValidatorDocument("<body><table></table></body>");
        this.validator.validateRpd11s2();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><table><th/></table></body>");
        this.validator.validateRpd11s2();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd11s4Table() throws Exception
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        this.validator.validateRpd11s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        this.validator.validateRpd11s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        this.validator.validateRpd11s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd11s5Table() throws Exception
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        this.validator.validateRpd11s5();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        this.validator.validateRpd11s5();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        this.validator.validateRpd11s5();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd12s1Iframe() throws Exception
    {
        setValidatorDocument("<body><iframe/></body>");
        this.validator.validateRpd12s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd13s1Label() throws Exception
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><input name='test' id='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><label /><input name='test' id='test' /></form></body>");
        this.validator.validateRpd13s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><label for='test' /><input name='test' id='test' /><input type='button'/></form></body>");
        this.validator.validateRpd13s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><label for='test' /><input name='test' id='test' /><input type='button'/>"
            + "<input type='hidden' /><input type='image' /><input type='reset' /><input type='submit' /></form></body>");
        this.validator.validateRpd13s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd13s4Submits() throws Exception
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><fieldset><input name='test' /><input type='submit' /></fieldset></form></body>");
        this.validator.validateRpd13s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<body><form><input name='test' /></form></body>");
        this.validator.validateRpd13s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd13s18Reset() throws Exception
    {
        setValidatorDocument("<body><form><input type='reset' /></form></body>");
        this.validator.validateRpd13s18();
        assertFalse(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd15s6Language() throws Exception
    {
        setValidatorDocument("<html></html>");
        this.validator.validateRpd15s6();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<html lang='en'></html>");
        this.validator.validateRpd15s6();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd16s1Charset() throws Exception
    {
        setValidatorDocument("<html><head></head></html>");
        this.validator.validateRpd16s1();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<html><head><meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s1();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd16s2Charset() throws Exception
    {
        setValidatorDocument("<html><head></head></html>");
        this.validator.validateRpd16s2();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<html><head>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s2();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<?xml version='1.0' encoding='ISO-8859-1' ?>"
            + "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head></html>");
        this.validator.validateRpd16s2();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head></html>");
        this.validator.validateRpd16s2();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

    @Test
    public void testRpd16s4CharsetPosition() throws Exception
    {
        setValidatorDocument("<html><head><meta/>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        this.validator.validateRpd16s4();
        assertFalse(getErrors(this.validator), isValid(this.validator));

        setValidatorDocument("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /><meta/></head></html>");
        this.validator.validateRpd16s4();
        assertTrue(getErrors(this.validator), isValid(this.validator));
    }

}
