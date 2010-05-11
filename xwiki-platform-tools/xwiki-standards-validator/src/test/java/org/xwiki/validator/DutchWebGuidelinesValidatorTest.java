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

import org.xwiki.validator.ValidationError.Type;

import junit.framework.TestCase;

public class DutchWebGuidelinesValidatorTest extends TestCase
{
    private DutchWebGuidelinesValidator validator;

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        validator = new DutchWebGuidelinesValidator();
    }

    private void setValidatorDocument(InputStream document) throws Exception
    {
        validator.setValidateXML(true);
        validator.setDocument(document);
    }

    private void setValidatorDocument(String content) throws Exception
    {
        validator.setValidateXML(false);
        validator.setDocument(new ByteArrayInputStream(content.getBytes("UTF-8")));
    }

    private String getErrors(DutchWebGuidelinesValidator validator)
    {
        StringBuffer buffer = new StringBuffer();

        for (ValidationError error : validator.getErrors()) {
            buffer.append(error + "\n");
        }

        return buffer.toString();
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

    public void testValidate() throws Exception
    {
        setValidatorDocument(getClass().getResourceAsStream("/dwg-valid.html"));
        validator.validate();

        for (ValidationError error : validator.getErrors()) {
            System.err.println(error);
        }

        assertTrue(getErrors(validator), isValid(validator));
    }

    // RPD 1s3

    public void testRpd1s3LinkValid() throws Exception
    {
        setValidatorDocument("<a href='test'>test</a>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3LinkJavascript() throws Exception
    {
        setValidatorDocument("<a href='javascript:'>test</a>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3LinkEventHandlers() throws Exception
    {
        setValidatorDocument("<a href='' onclick=''></a>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<a href='#' onclick=''></a>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<a href='test' onclick=''></a>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3FormValidSubmit() throws Exception
    {
        setValidatorDocument("<form><fieldset><submit/></fieldset></form>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3FormValidInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='submit' /></fieldset></form>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3FormInvalidImageInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='image' alt='' /></fieldset></form>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3FormValidImageInput() throws Exception
    {
        setValidatorDocument("<form><fieldset><input type='image' alt='submit' /></fieldset></form>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd1s3FormNoSubmit() throws Exception
    {
        setValidatorDocument("<form></form>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 2s3

    public void testRpd2s3NoDoctype() throws Exception
    {
        setValidatorDocument("<html></html>");
        validator.validateRpd2s3();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 2s4

    public void testRpd2s4NoDoctype() throws Exception
    {
        setValidatorDocument("<html></html>");
        validator.validateRpd2s4();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 2s5

    public void testRpd2s5ValidDoctype() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        validator.validateRpd2s5();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd2s5FramesetDoctype() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Frameset//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd'><html></html>");
        validator.validateRpd2s5();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd2s5FramesetTag() throws Exception
    {
        setValidatorDocument("<frameset></frameset>");
        validator.validateRpd2s5();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd2s5FrameTag() throws Exception
    {
        setValidatorDocument("<frame></frame>");
        validator.validateRpd2s5();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s1

    public void testRpd3s1BoldMarkup() throws Exception
    {
        setValidatorDocument("<p><b></b></p>");
        validator.validateRpd3s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd3s1ItalicMarkup() throws Exception
    {
        setValidatorDocument("<p><i></i></p>");
        validator.validateRpd3s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s2

    public void testRpd3s2NoHeading() throws Exception
    {
        setValidatorDocument("<body></body>");
        validator.validateRpd3s2();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s3

    public void testRpd3s3HeadingsValid() throws Exception
    {
        setValidatorDocument("<body><h1></h1><h2></h2><h2></h2><h3></h3></body>");
        validator.validateRpd3s3();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd3s3HeadingsMissingLevel() throws Exception
    {
        setValidatorDocument("<body><h1></h1><h3></h3></body>");
        validator.validateRpd3s3();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s4

    public void testRpd3s4ValidParagraphs() throws Exception
    {
        setValidatorDocument("<body><p>content<br/>content<br/>content<br/></p>"
            + "<p>content<br/>content<br/>content<br/></p><p>content<br/>content<br/>content<br/></p></body>");
        validator.validateRpd3s4();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd3s4MissingParagraph() throws Exception
    {
        // Consecutive line breaks.
        setValidatorDocument("<body><p>content<br/><br/>content</p></body>");
        validator.validateRpd3s4();
        assertFalse(getErrors(validator), isValid(validator));

        // Consecutive line breaks separated by white spaces.
        setValidatorDocument("<body><p>content<br/>   <br/>content</p></body>");
        validator.validateRpd3s4();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s5

    public void testRpd3s5InvalidMarkup() throws Exception
    {
        setValidatorDocument("<body><p><b>bold</b></p></body>");
        validator.validateRpd3s5();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><p><i>italic</i></p></body>");
        validator.validateRpd3s5();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s9

    public void testRpd3s9Sub() throws Exception
    {
        setValidatorDocument("<body><p><sub>sub</sub></p></body>");
        validator.validateRpd3s9();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd3s9Sup() throws Exception
    {
        setValidatorDocument("<body><p><sup>sup</sup></p></body>");
        validator.validateRpd3s9();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s11

    public void testRpd3s11Quotation() throws Exception
    {
        setValidatorDocument("<body><p><q>quotation</q></p></body>");
        validator.validateRpd3s11();
        assertFalse(getErrors(validator), isValid(validator));
    }

    // RPD 3s13

    public void testRpd3s13BulletList() throws Exception
    {
        setValidatorDocument("<body> * item1<br/> * item2 <br/> * item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body>*item1<br/>*item2<br/>*item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));

    }

    public void testRpd3s13DashList() throws Exception
    {
        setValidatorDocument("<body> - item1<br/> - item2 <br/> - item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body>-item1<br/>-item2 <br/>-item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd3s13NumberedList() throws Exception
    {
        setValidatorDocument("<body> 1. item1<br/> 2. item2 <br/> 3. item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body>1.item1<br/>2.item2<br/>3.item3</body>");
        validator.validateRpd3s13();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd6s1Doctypes() throws Exception
    {
        setValidatorDocument("<!DOCTYPE html PUBLIC '-//ORG//DTD FOO 1.0 Bar//EN' "
            + "'http://www.foo.bar/xhtml1-strict.dtd'><html></html>");
        validator.validateRpd6s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' "
            + "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'><html></html>");
        validator.validateRpd6s1();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd7s1ValidAlts() throws Exception
    {
        setValidatorDocument("<body><img alt='' /></body>");
        validator.validateRpd7s1();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><area alt='' /></body>");
        validator.validateRpd7s1();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><input alt='' type='image' /></body>");
        validator.validateRpd7s1();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd7s1MissingAlts() throws Exception
    {
        setValidatorDocument("<body><img /></body>");
        validator.validateRpd7s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><area /></body>");
        validator.validateRpd7s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><input type='image' /></body>");
        validator.validateRpd7s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd7s4ImagesInLinks() throws Exception
    {
        setValidatorDocument("<body><a><img alt=''/></a></body>");
        validator.validateRpd7s4();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a><img alt=''/>text</a></body>");
        validator.validateRpd7s4();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a><img alt='text' /></a></body>");
        validator.validateRpd7s4();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd7s5ImageMaps() throws Exception
    {
        setValidatorDocument("<body><img alt='' usemap='#map' /></body>");
        validator.validateRpd7s5();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='' /></map></body>");
        validator.validateRpd7s5();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><img alt='text' usemap='#map' /><map name='map'><area alt='text' /></map></body>");
        validator.validateRpd7s5();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd8s1Links() throws Exception
    {
        setValidatorDocument("<body><a>to get the resource, click here</a></body>");
        validator.validateRpd8s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a>resource</a></body>");
        validator.validateRpd8s1();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd8s11Accesskeys() throws Exception
    {
        setValidatorDocument("<body><a accesskey='a'></a></body>");
        validator.validateRpd8s11();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a accesskey='8'></a></body>");
        validator.validateRpd8s11();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd8s14Links() throws Exception
    {
        setValidatorDocument("<body><a target='any'></a></body>");
        validator.validateRpd8s14();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a onclick='window.open'></a></body>");
        validator.validateRpd8s14();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd8s16Links() throws Exception
    {
        setValidatorDocument("<body><a href='mailto:text@text.com'>text@text.com</a></body>");
        validator.validateRpd8s16();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><a href='mailto:text@text.com'>mail</a></body>");
        validator.validateRpd8s16();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd8s17Links() throws Exception
    {
        setValidatorDocument("<body><a href='mailto:text@text.com text'>text@text.com</a></body>");
        validator.validateRpd8s17();
        assertFalse(getErrors(validator), isValid(validator));
        
        setValidatorDocument("<body><a href='mailto:text@text.com'>text@text.com</a></body>");
        validator.validateRpd8s17();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd9s1StyleAttr() throws Exception
    {
        setValidatorDocument("<body><div style='test'></div></body>");
        validator.validateRpd9s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd9s1StyleTag() throws Exception
    {
        setValidatorDocument("<body><style></style></body>");
        validator.validateRpd9s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd11s2Table() throws Exception
    {
        setValidatorDocument("<body><table></table></body>");
        validator.validateRpd11s2();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><table><th/></table></body>");
        validator.validateRpd11s2();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd11s4Table() throws Exception
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        validator.validateRpd11s4();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        validator.validateRpd11s4();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        validator.validateRpd11s4();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd11s5Table() throws Exception
    {
        setValidatorDocument("<body><table><th/><th/></table></body>");
        validator.validateRpd11s5();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><table><th scope=''/></table></body>");
        validator.validateRpd11s5();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><table><th id='a' /><td headers='a'/></table></body>");
        validator.validateRpd11s5();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd12s1Iframe() throws Exception
    {
        setValidatorDocument("<body><iframe/></body>");
        validator.validateRpd12s1();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd13s1Label() throws Exception
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        validator.validateRpd13s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><form><input name='test' id='test' /></form></body>");
        validator.validateRpd13s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><form><label /><input name='test' id='test' /></form></body>");
        validator.validateRpd13s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><form><label for='test' /><input name='test' id='test' /><input type='button'/></form></body>");
        validator.validateRpd13s1();
        assertTrue(getErrors(validator), isValid(validator));
        
        setValidatorDocument("<body><form><label for='test' /><input name='test' id='test' /><input type='button'/>" +
        		"<input type='hidden' /><input type='image' /><input type='reset' /><input type='submit' /></form></body>");
        validator.validateRpd13s1();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd13s4Submits() throws Exception
    {
        setValidatorDocument("<body><form><input name='test' /></form></body>");
        validator.validateRpd13s4();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><form><fieldset><input name='test' /><input type='submit' /></fieldset></form></body>");
        validator.validateRpd13s4();
        assertTrue(getErrors(validator), isValid(validator));

        setValidatorDocument("<body><form><input name='test' /></form></body>");
        validator.validateRpd13s4();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd13s18Reset() throws Exception
    {
        setValidatorDocument("<body><form><input type='reset' /></form></body>");
        validator.validateRpd13s18();
        assertFalse(getErrors(validator), isValid(validator));
    }

    public void testRpd15s6Language() throws Exception
    {
        setValidatorDocument("<html></html>");
        validator.validateRpd15s6();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<html lang='en'></html>");
        validator.validateRpd15s6();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd16s1Charset() throws Exception
    {
        setValidatorDocument("<html><head></head></html>");
        validator.validateRpd16s1();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<html><head><meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        validator.validateRpd16s1();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd16s2Charset() throws Exception
    {
        setValidatorDocument("<html><head></head></html>");
        validator.validateRpd16s2();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<html><head>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        validator.validateRpd16s2();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<?xml version='1.0' encoding='ISO-8859-1' ?>"
            + "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head></html>");
        validator.validateRpd16s2();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head></html>");
        validator.validateRpd16s2();
        assertTrue(getErrors(validator), isValid(validator));
    }

    public void testRpd16s4CharsetPosition() throws Exception
    {
        setValidatorDocument("<html><head><meta/>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=foo' /></head></html>");
        validator.validateRpd16s4();
        assertFalse(getErrors(validator), isValid(validator));

        setValidatorDocument("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /><meta/></head></html>");
        validator.validateRpd16s4();
        assertTrue(getErrors(validator), isValid(validator));
    }

}
