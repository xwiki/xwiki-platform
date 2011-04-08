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
package org.xwiki.accessibility;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.xwiki.validator.DutchWebGuidelinesValidator;

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
        validator.setDocument(document);
    }
    
    private void setValidatorDocument(String content) throws Exception
    {        
        validator.setDocument(new ByteArrayInputStream(content.getBytes("UTF-8")));
    }
    
    private String getErrors(DutchWebGuidelinesValidator validator)
    {
        return validator.getErrors().toString();        
    }
    
    private boolean isValid(DutchWebGuidelinesValidator validator)
    {
        return validator.getErrors().isEmpty();
    }
    
    // All
    
    public void testValidate() throws Exception
    {
        setValidatorDocument(getClass().getResourceAsStream("/dwg-valid.html"));
        validator.validate();
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
    
    public void testRpd1s3LinkWrongAttribute() throws Exception
    {
        setValidatorDocument("<a href='test' mouseover=''>test</a>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));
    }
    
    public void testRpd1s3FormValid() throws Exception
    {
        setValidatorDocument("<form><fieldset><submit/></fieldset></form>");
        validator.validateRpd1s3();
        assertTrue(getErrors(validator), isValid(validator));
    }
    
    public void testRpd1s3FormNoSubmit() throws Exception
    {
        setValidatorDocument("<form></form>");
        validator.validateRpd1s3();
        assertFalse(getErrors(validator), isValid(validator));
    }
    
    // RPD 2s5
    
    public void testRpd2s5FramesetDoctype() throws Exception 
    {
        setValidatorDocument(getClass().getResourceAsStream("/dwg-frame-framesetdoctype.html"));
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
    
    public void testRpd3s1HeadingsValid() throws Exception 
    {
        setValidatorDocument("<body><h1></h1><h2></h2><h2></h2><h3></h3></body>");
        validator.validateRpd3s1();
        assertTrue(getErrors(validator), isValid(validator));
    }
    
    public void testRpd3s1HeadingsMissingLevel() throws Exception 
    {
        setValidatorDocument("<body><h1></h1><h3></h3></body>");
        validator.validateRpd3s1();
        assertFalse(getErrors(validator), isValid(validator));
    }
    
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
}
