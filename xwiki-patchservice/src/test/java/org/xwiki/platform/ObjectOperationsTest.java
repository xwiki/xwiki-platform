package org.xwiki.platform;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;
import org.xwiki.platform.patchservice.impl.PositionImpl;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

public class ObjectOperationsTest extends MockObjectTestCase
{
    Document domDoc;

    XWikiDocument doc;

    XWikiContext context;

    private Mock mockXWiki;

    protected void setUp()
    {
        try {
            domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc = new XWikiDocument();
            doc.setFullName("XWiki.XWikiTestDocument");
            context = new XWikiContext();
            this.mockXWiki =
                mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
                    new Object[] {new XWikiConfig(), this.context});
            BaseClass bclass = new BaseClass();
            bclass.addTextField("property", "The Property", 20);
            bclass.addTextAreaField("textarea", "Textarea property", 60, 10);
            bclass.addNumberField("number", "Number Property", 10, "integer");
            this.mockXWiki.stubs().method("getClass").with(eq("XWiki.SomeClass"), eq(context))
                .will(returnValue(bclass));
            this.context.setWiki((XWiki) this.mockXWiki.proxy());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void testApplyObjectAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        operation.apply(doc, context);
        assertEquals(1, doc.getObjectNumbers("XWiki.SomeClass"));
        assertNotNull(doc.getObject("XWiki.SomeClass"));
        assertNull(doc.getObject("Invalid.Class"));
    }

    public void testApplyTwiceObjectAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        operation.apply(doc, context);
        operation.apply(doc, context);
        assertEquals(2, doc.getObjectNumbers("XWiki.SomeClass"));
    }

    public void testXmlRoundtripObjectAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc.newObject("XWiki.SomeClass", context);
        assertNotNull(doc.getObject("XWiki.SomeClass", 0));
        operation.apply(doc, context);
        assertNull(doc.getObject("XWiki.SomeClass", 0));
    }

    public void testApplyTwiceObjectDeleteOperation() throws XWikiException
    {
        doc.newObject("XWiki.SomeClass", context);
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        operation.apply(doc, context);
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertNull(doc.getObject("XWiki.SomeClass", 0));
    }

    public void testApplyInvalidObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.InvalidClass", 0);
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "value");
        doc.newObject("XWiki.SomeClass", context);
        assertEquals("", doc.getObject("XWiki.SomeClass", 0).displayView("property", context));
        operation.apply(doc, context);
        assertEquals("value", doc.getObject("XWiki.SomeClass", 0)
            .displayView("property", context));
        operation.setObjectProperty("XWiki.SomeClass", 0, "number", "30");
        operation.apply(doc, context);
        assertEquals("30", doc.getObject("XWiki.SomeClass", 0).displayView("number", context));
    }

    public void testApplyInvalidObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "value");
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc.newObject("XWiki.SomeClass", context);
        operation.apply(doc, context);
        operation.setObjectProperty("XWiki.SomeClass", 0, "number", "value");
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals("value", doc.getObject("XWiki.SomeClass", 0)
            .displayView("property", context));
    }

    public void testXmlRoundtripObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "  val<\">'ue   ");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        doc.newObject("XWiki.SomeClass", context);
        doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nthird row");
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "second row\n",
            new PositionImpl(1, 0, "first row\n", "third"));
        assertEquals("first row\nthird row", doc.getObject("XWiki.SomeClass", 0).displayView(
            "textarea", context));
        operation.apply(doc, context);
        assertEquals("first row\nsecond row\nthird row", doc.getObject("XWiki.SomeClass", 0)
            .displayView("textarea", context));
    }

    public void testApplyInvalidObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "second row\n",
            new PositionImpl(1, 0, "first row\n", "third row"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc.newObject("XWiki.SomeClass", context);
        doc.setIntValue("XWiki.SomeClass", "number", 42);
        doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row and \n the third row");
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nthird row");
        operation.apply(doc, context);
        operation.insertInProperty("XWiki.SomeClass", 0, "number", "value", new PositionImpl());
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals("42", doc.getObject("XWiki.SomeClass", 0).displayView("number", context));
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl(0,
            0,
            "invalid",
            "invalid"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl());
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        doc.newObject("XWiki.SomeClass", context);
        doc
            .setLargeStringValue("XWiki.SomeClass", "textarea",
                "first row\nsecond row\nthird row");
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "second row\n",
            new PositionImpl(1, 0, "first row\n", "second"));
        assertEquals("first row\nsecond row\nthird row", doc.getObject("XWiki.SomeClass", 0)
            .displayView("textarea", context));
        operation.apply(doc, context);
        assertEquals("first row\nthird row", doc.getObject("XWiki.SomeClass", 0).displayView(
            "textarea", context));
    }

    public void testApplyInvalidObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "second row\n",
            new PositionImpl(1, 0, "first row\n", "second row\nthird row"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc.newObject("XWiki.SomeClass", context);
        doc.setIntValue("XWiki.SomeClass", "number", 42);
        doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row and \n the third row");
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        doc
            .setLargeStringValue("XWiki.SomeClass", "textarea",
                "first row\nsecond row\nthird row");
        operation.apply(doc, context);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "number", "value", new PositionImpl());
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "value",
            new PositionImpl(0, 0, "invalid", "invalid"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "value",
            new PositionImpl());
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }
}
