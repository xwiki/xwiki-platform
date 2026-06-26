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
package com.xpn.xwiki.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;

import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link PropertyChangedRule} in the notification mechanism. <br>
 * The tests are done for the {@link DocChangeRule#verify(XWikiDocument, XWikiDocument, XWikiContext)} function and,
 * since this function should be symmetric with respect to its two document parameters, all the tests are done symmetric
 * too: each test function contains two assertions, one for the result of {@code verify(newdoc, olddoc, context)}
 * and the other one for {@code verify(olddoc, newdoc, context)}; some tests cases might be duplicated by the
 * symmetric call in another test function.
 */
@Deprecated
@OldcoreTest
@AllComponents
class PropertyChangedRuleTest implements XWikiDocChangeNotificationInterface
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private PropertyChangedRule rule;

    private XWikiDocument classDoc;

    private String testClassName = "Test.TestClass";

    private DocumentReference testClassReference = new DocumentReference("Test", "Test", "TestClass");

    private BaseClass testClass;

    private BaseClass otherClass;

    private String otherClassName = "Test.OtherClass";

    private DocumentReference testOtherClassReference = new DocumentReference("Test", "Test", "OtherClass");

    private String testPropertyName = "field";

    private XWikiContext context;

    private boolean passed;

    /**
     * Creates 2 classes: one that would actually be used to create objects and check property changes and the other one
     * to be able to test behaviour when the document contains more than one type of objects. Creates a xwiki mock that
     * only returns the above mentioned classes on {@link XWiki#getClass(String, XWikiContext)}.
     */
    @BeforeEach
    void setUp() throws Exception
    {
        this.context = this.oldcore.getXWikiContext();

        XWiki mockXWiki = mock(XWiki.class);
        this.context.setWiki(mockXWiki);

        this.classDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestClass"));
        this.testClass = this.classDoc.getXClass();
        this.testClass.addTextField(this.testPropertyName, this.testPropertyName, 10);

        XWikiDocument otherClassDoc = new XWikiDocument(new DocumentReference("Test", "Test", "OtherClass"));
        this.otherClass = otherClassDoc.getXClass();
        // Just to make tests a little more interesting
        this.otherClass.addTextField(this.testPropertyName, this.testPropertyName, 10);

        this.rule = new PropertyChangedRule(this, this.testClassName, this.testPropertyName);
    }

    @Test
    void verifySingleObjectNotChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        oldObj.setStringValue(this.testPropertyName, "value");
        newObj.setStringValue(this.testPropertyName, "value");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifySingleObjectChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        oldObj.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifySingleObjectXWikiClassNotChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        oldObj.setStringValue(this.testPropertyName, "value");
        newObj.setStringValue(this.testPropertyName, "value");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsXWikiClassNotChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject otherOldObject = oldDoc.newObject(this.otherClassName, this.context);
        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject otherNewObject = newDoc.newObject(this.otherClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        otherOldObject.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value");
        otherNewObject.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsXWikiClassChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject otherOldObject = oldDoc.newObject(this.otherClassName, this.context);
        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject otherNewObject = newDoc.newObject(this.otherClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        otherOldObject.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value1");
        otherNewObject.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifySingleObjectXWikiClassChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        oldObj.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsMultipleXWikiClassChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject otherOldObject = oldDoc.newObject(this.otherClassName, this.context);
        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        BaseObject otherNewObject = newDoc.newObject(this.otherClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);

        otherOldObject.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value1");
        oldObj2.setStringValue(this.testPropertyName, "value2");
        otherNewObject.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value1");
        newObj2.setStringValue(this.testPropertyName, "value3");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsMultipleXWikiClassNotChanged() throws XWikiException
    {
        XWikiDocument newDoc = this.classDoc.clone();
        XWikiDocument oldDoc = this.classDoc.clone();

        BaseObject otherOldObject = oldDoc.newObject(this.otherClassName, this.context);
        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        BaseObject otherNewObject = newDoc.newObject(this.otherClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);

        otherOldObject.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value1");
        oldObj2.setStringValue(this.testPropertyName, "value2");
        otherNewObject.setStringValue(this.testPropertyName, "value1");
        newObj.setStringValue(this.testPropertyName, "value1");
        newObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyObjectAdded() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj = newDoc.newObject(this.testClassName, this.context);

        newObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyObjectDeleted() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);

        oldObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyNoObjectOfClassObjectDeleted() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject oldObj = oldDoc.newObject(this.testClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.otherClassName, this.context);

        newObj.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyNoObjectOfClass() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject oldObj = oldDoc.newObject(this.otherClassName, this.context);
        BaseObject newObj = newDoc.newObject(this.otherClassName, this.context);

        newObj.setStringValue(this.testPropertyName, "value1");
        oldObj.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsPropertyNotChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsPropertyChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value3");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsOtherObjectAdded() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObjOtherClass = newDoc.newObject(this.otherClassName, this.context);
        newObjOtherClass.setStringValue(this.testPropertyName, "value2");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsOtherObjectDeleted() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObjOtherClass = oldDoc.newObject(this.otherClassName, this.context);
        oldObjOtherClass.setStringValue(this.testPropertyName, "value2");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertFalse(this.passed, "My notification should not have been called");
    }

    @Test
    void verifyMultipleObjectsObjectAdded() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsObjectDeleted() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsOtherObjectAddedPropertyChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObjOtherClass = newDoc.newObject(this.otherClassName, this.context);
        newObjOtherClass.setStringValue(this.testPropertyName, "value2");
        BaseObject newObj2 = newDoc.newObject(this.testClassName, this.context);
        newObj2.setStringValue(this.testPropertyName, "value3");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Test
    void verifyMultipleObjectsClassChanged() throws XWikiException
    {
        XWikiDocument newDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));
        XWikiDocument oldDoc = new XWikiDocument(new DocumentReference("Test", "Test", "TestDoc"));

        BaseObject newObj1 = newDoc.newObject(this.testClassName, this.context);
        newObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject newObjOtherClass = newDoc.newObject(this.otherClassName, this.context);
        newObjOtherClass.setStringValue(this.testPropertyName, "value2");

        BaseObject oldObj1 = oldDoc.newObject(this.testClassName, this.context);
        oldObj1.setStringValue(this.testPropertyName, "value1");
        BaseObject oldObj2 = oldDoc.newObject(this.testClassName, this.context);
        oldObj2.setStringValue(this.testPropertyName, "value2");

        this.passed = false;
        this.rule.verify(newDoc, oldDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");

        // The rule should be symmetric. Do the inverse test too.
        this.passed = false;
        this.rule.verify(oldDoc, newDoc, this.context);
        assertTrue(this.passed, "My notification should have been called");
    }

    @Override
    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        this.passed = true;
    }
}
