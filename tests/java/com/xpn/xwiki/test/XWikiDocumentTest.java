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
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.api.Document;

import java.util.HashMap;
import java.util.Map;

public class XWikiDocumentTest  extends HibernateTestCase {

    protected String invalidGroovy1 = "public class ValidationTest implements XWikiValidationInterface { public boolean validateDocument(doc, context} { return false; };";
    protected String validGroovy1 = "import com.xpn.xwiki.validation.*; import com.xpn.xwiki.*; import com.xpn.xwiki.doc.*; public class ValidationTest extends XWikiDefaultValidation implements XWikiValidationInterface { public boolean validateDocument(XWikiDocument doc, XWikiContext context) { if (doc.getContent().length()>10) return false; else return true; }}";
    protected String invalidGroovy2 = invalidGroovy1;
    protected String validGroovy2 = "import com.xpn.xwiki.validation.*; import com.xpn.xwiki.*; import com.xpn.xwiki.objects.*; public class ValidationTest extends XWikiDefaultValidation implements XWikiValidationInterface { public boolean validateObject(BaseObject object, XWikiContext context) { if (object.get(\"test\").getValue().length()>10) return false; else return true; }}";

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
        context.setRequest(request);

        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        assertTrue("Validation should have failed with an exception", context.getValidationStatus().hasExceptions());
    }

    public void testValidationScriptFails() throws Throwable {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        context.setRequest(request);
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
    }

    public void testValidationScriptWorks() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdef");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        Map map = new HashMap();
        map.put("xvalidation", "Test.ValidationGroovy");
        XWikiRequest request = new XWikiFakeRequest(map);
        context.setRequest(request);
        assertTrue("Validation should have worked", xwiki.validateDocument(doc, context));
        assertNull("Validation should not have failed with an exception", context.getValidationStatus());
    }

    public void testValidationDocScriptFailsToCompile() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(invalidGroovy1);
        xwiki.saveDocument(scriptdoc, context);

        doc.setValidationScript("Test.ValidationGroovy");
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        assertTrue("Validation should have failed with an exception", context.getValidationStatus().hasExceptions());
   }

    public void testValidationDocScriptFails() throws Throwable {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        doc.setValidationScript("Test.ValidationGroovy");
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
    }

    public void testValidationDocScriptWorks() throws Throwable {
        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setContent("abcdef");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy", context);
        scriptdoc.setContent(validGroovy1);
        xwiki.saveDocument(scriptdoc, context);
        doc.setValidationScript("Test.ValidationGroovy");
        assertTrue("Validation should have worked", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
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
        context.setRequest(new XWikiFakeRequest(map));
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        assertTrue("Validation should have failed with an exception", context.getValidationStatus().hasExceptions());
    }

    public void testValidationClassScriptFails() throws Throwable {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();
        bclass.setValidationScript("Test.ValidationGroovy1");
        bclass.addTextField("test", "test", 10);

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdefghijklmnopqrstuvwxyz");
        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy1", context);
        scriptdoc.setContent(validGroovy2);

        xwiki.saveDocument(scriptdoc, context);
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
    }

    public void testValidationClassScriptWorks() throws Throwable {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();
        bclass.setValidationScript("Test.ValidationGroovy1");
        bclass.addTextField("test", "test", 10);

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdef");

        XWikiDocument scriptdoc  = xwiki.getDocument("Test.ValidationGroovy1", context);
        scriptdoc.setContent(validGroovy2);

        assertTrue("Validation should have worked", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
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
        context.setRequest(request);
        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        assertTrue("Validation should have failed with an exception", context.getValidationStatus().hasExceptions());
    }

    public void testValidationFieldRegexpFails() throws Throwable {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();


        // Add regexp to field
        bclass.addTextField("test", "test", 10);
        ((StringClass)bclass.get("test")).setValidationRegExp("/abc.*xyz/i");

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "nopqrstuvwxyzfkjfdabcdefghijklm");

        assertFalse("Validation should have failed", xwiki.validateDocument(doc, context));
        assertEquals("Validation should have failed but not with an exception", context.getValidationStatus().getExceptions().size(), 0);
    }

    public void testValidationFieldRegexpWorks() throws Throwable {
        XWikiDocument classdoc  = xwiki.getDocument("Test.ValidationClass", context);
        BaseClass bclass = classdoc.getxWikiClass();

        // Add regexp to field
        bclass.addTextField("test", "test", 10);
        ((StringClass)bclass.get("test")).setValidationRegExp("/abc.*xyz/i");

        XWikiDocument doc  = xwiki.getDocument("Test.ValidationTest", context);
        doc.setStringValue("Test.ValidationClass", "test", "abcdefghijklmnopqrstuvwxyz");

        assertTrue("Validation should have worked", xwiki.validateDocument(doc, context));
        XWikiValidationStatus status = context.getValidationStatus();
        Throwable e = (status==null) ? null : (Throwable) status.getExceptions().get(0);
        String message =  (e==null) ? "" : e.getMessage();
        if (e!=null) throw e;
        assertNull("Validation should have failed but not with an exception: " + message, context.getValidationStatus());
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
