/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author jeremi
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.validation.XWikiValidationException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.api.Document;

import java.util.HashMap;
import java.util.Map;

public class XWikiDocumentTest  extends HibernateTestCase {

    protected String invalidGroovy1 = "public class ValidationTest implements XWikiValidationInterface { public boolean validateDocument(doc, context} { return false; };";
    protected String validGroovy1 = "public class ValidationTest implements XWikiValidationInterface  extends XWikiDefaultValidation { public boolean validateDocument(doc, context} { if (doc.content.length>10) return false; else return true; };";
    protected String invalidGroovy2 = invalidGroovy1;
    protected String validGroovy2 = "public class ValidationTest implements XWikiValidationInterface  extends XWikiDefaultValidation { public boolean validateObject(object, context} { if (object.get(\"test\").length()>10) return false; else return true; };";

    public void testValidationAPI() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest1", context);
        xwiki.validateDocument(doc, context);
    }

    public void testValidationScriptFailsToCompile() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);

        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have failed with XWikiException and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            // Without a script we should get here
            return;
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationScriptFails() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            // This is good our validation has failed
            return;
        } catch (XWikiException e) {
            assertTrue("Validation should have failed with XWikiValidationException and it failed with XWikiException", false);
            // This is not good the validation script failed to be run
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationScriptWorks() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdef");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have worked and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            assertTrue("Validation should have worked and it failed with XWikiException", false);
        }
    }

    public void testValidationDocScriptFailsToCompile() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);

        doc.setValidationScript("Test.ValidationGroovy");
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have failed with XWikiException and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            // Without a script we should get here
            return;
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationDocScriptFails() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        doc.setValidationScript("Test.ValidationGroovy");
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            // This is good our validation has failed
            return;
        } catch (XWikiException e) {
            assertTrue("Validation should have failed with XWikiValidationException and it failed with XWikiException", false);
            // This is not good the validation script failed to be run
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationDocScriptWorks() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdef");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        doc.setValidationScript("Test.ValidationGroovy");
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have worked and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            assertTrue("Validation should have worked and it failed with XWikiException", false);
        }
    }


    public void testValidationClassScriptFailsToCompile() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();
        bclass.setValidationScript("Test.ValidationGroovy");
        bclass.addTextField("test", "test", 10);

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy2);
        xwiki.saveDocument(scriptdoc, context);

        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have failed with XWikiException and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            // Without a script we should get here
            return;
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationClassScriptFails() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();
        bclass.setValidationScript("Test.ValidationGroovy1");
        bclass.addTextField("test", "test", 10);

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy2);

        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            // This is good our validation has failed
            return;
        } catch (XWikiException e) {
            assertTrue("Validation should have failed with XWikiValidationException and it failed with XWikiException", false);
            // This is not good the validation script failed to be run
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationClassScriptWorks() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();
        bclass.setValidationScript("Test.ValidationGroovy");
        bclass.addTextField("test", "test", 10);

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdef");

        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy2);

        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have worked and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            assertTrue("Validation should have worked and it failed with XWikiException", false);
        }
    }


    public void testValidationFieldRegExpInvalid() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();

        // Add regexp to field
        bclass.addTextField("test", "test", 10);
        ((StringClass)bclass.get("test")).setValidationRegExp("/dfdgfg///");

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy2);
        xwiki.saveDocument(scriptdoc, context);

        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have failed with XWikiException and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            // Without a script we should get here
            return;
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationFieldRegexpFails() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();

        // Add regexp to field
        bclass.addTextField("test", "test", 10);
        ((StringClass)bclass.get("test")).setValidationRegExp("abc.*xyz");

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy2);

        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            // This is good our validation has failed
            return;
        } catch (XWikiException e) {
            assertTrue("Validation should have failed with XWikiValidationException and it failed with XWikiException", false);
            // This is not good the validation script failed to be run
        }
        assertTrue("Validation should have failed with XWikiException and it did not fail", false);
    }

    public void testValidationFieldRegexpWorks() throws XWikiException {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();

        // Add regexp to field
        bclass.addTextField("test", "test", 10);
        ((StringClass)bclass.get("test")).setValidationRegExp("abc.*xyz");

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdefghijklmnopqrstuvwxyz");

        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy2);

        try {
         xwiki.validateDocument(doc, context);
        } catch (XWikiValidationException e) {
            assertTrue("Validation should have worked and it failed with XWikiValidationException", false);
        } catch (XWikiException e) {
            assertTrue("Validation should have worked and it failed with XWikiException", false);
        }
    }


    public void testCustomClass() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.CustomTest", context);
        doc.setCustomClass(CustomDocumentClass.class.getName());
        xwiki.saveDocument(doc, context);


        doc  = xwiki.getDocument("Test.NotCustomTest", context);
        xwiki.saveDocument(doc, context);

        doc  = xwiki.getDocument("Test.CustomTest", context);
        assertEquals(CustomDocumentClass.class.getName(), doc.getCustomClass());
        assertTrue(doc.newDocument(context) instanceof CustomDocumentClass);

        doc  = xwiki.getDocument("Test.NotCustomTest", context);
        assertNotNull(doc);
        assertTrue(doc.newDocument(context) instanceof Document);        
        assertFalse(doc.newDocument(context) instanceof CustomDocumentClass);

        doc  = xwiki.getDocument("Test.CustomTest", context);
        doc.setContent("plop");
        xwiki.saveDocument(doc, context);

        assertEquals(CustomDocumentClass.class.getName(), doc.getCustomClass());
        assertTrue(doc.newDocument(context) instanceof CustomDocumentClass);
    }
}
