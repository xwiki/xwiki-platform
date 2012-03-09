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
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.xwiki.validator.framework.AbstractXMLValidator;

public class XMLValidatorTest extends TestCase
{
    public class XMLValidator extends AbstractXMLValidator
    {
        public String getName()
        {
            return "XML validator";
        }

    }

    private XMLValidator validator;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.validator = new XMLValidator();
    }

    public void testValidate() throws UnsupportedEncodingException
    {
        this.validator.setValidateXML(false);
        
        this.validator.setDocument(new ByteArrayInputStream("<element/>".getBytes("UTF-8")));

        assertTrue(this.validator.validate().isEmpty());
        
        this.validator.setDocument(new ByteArrayInputStream("not XML".getBytes("UTF-8")));
        
        assertFalse(this.validator.validate().isEmpty());
    }
}
