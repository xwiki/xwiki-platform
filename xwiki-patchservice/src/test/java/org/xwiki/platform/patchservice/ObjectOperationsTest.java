package org.xwiki.platform.patchservice;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Mock;
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
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;

public class ObjectOperationsTest extends AbstractXWikiComponentTestCase
{
    Document domDoc;

    XWikiDocument doc;

    XWikiContext context;

    private Mock mockXWiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        try {
            this.domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            this.doc = new XWikiDocument();
            this.doc.setFullName("XWiki.XWikiTestDocument");
            this.context = new XWikiContext();
            this.mockXWiki =
                mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {new XWikiConfig(),
                this.context});
            BaseClass bclass = new BaseClass();
            bclass.addTextField("property", "The Property", 20);
            bclass.addTextAreaField("textarea", "Textarea property", 60, 10);
            bclass.addNumberField("number", "Number Property", 10, "integer");
            this.mockXWiki.stubs().method("getClass").with(eq("XWiki.SomeClass"), eq(this.context)).will(
                returnValue(bclass));
            this.context.setWiki((XWiki) this.mockXWiki.proxy());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void testApplyObjectAddOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        operation.apply(this.doc, this.context);
        assertEquals(1, this.doc.getObjectNumbers("XWiki.SomeClass"));
        assertNotNull(this.doc.getObject("XWiki.SomeClass"));
        assertNull(this.doc.getObject("Invalid.Class"));
    }

    public void testApplyTwiceObjectAddOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        operation.apply(this.doc, this.context);
        operation.apply(this.doc, this.context);
        assertEquals(2, this.doc.getObjectNumbers("XWiki.SomeClass"));
    }

    public void testXmlRoundtripObjectAddOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
        operation.addObject("XWiki.SomeClass");
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.newObject("XWiki.SomeClass", this.context);
        assertNotNull(this.doc.getObject("XWiki.SomeClass", 0));
        operation.apply(this.doc, this.context);
        assertNull(this.doc.getObject("XWiki.SomeClass", 0));
    }

    public void testApplyTwiceObjectDeleteOperation() throws XWikiException
    {
        this.doc.newObject("XWiki.SomeClass", this.context);
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        operation.apply(this.doc, this.context);
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertNull(this.doc.getObject("XWiki.SomeClass", 0));
    }

    public void testApplyInvalidObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.InvalidClass", 0);
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectDeleteOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
        operation.deleteObject("XWiki.SomeClass", 0);
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "value");
        this.doc.newObject("XWiki.SomeClass", this.context);
        assertEquals("", this.doc.getObject("XWiki.SomeClass", 0).displayView("property", this.context));
        operation.apply(this.doc, this.context);
        assertEquals("value", this.doc.getObject("XWiki.SomeClass", 0).displayView("property", this.context));
        operation.setObjectProperty("XWiki.SomeClass", 0, "number", "30");
        operation.apply(this.doc, this.context);
        assertEquals("30", this.doc.getObject("XWiki.SomeClass", 0).displayView("number", this.context));
    }

    public void testApplyInvalidObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "value");
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.newObject("XWiki.SomeClass", this.context);
        operation.apply(this.doc, this.context);
        operation.setObjectProperty("XWiki.SomeClass", 0, "number", "value");
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals("value", this.doc.getObject("XWiki.SomeClass", 0).displayView("property", this.context));
    }

    public void testXmlRoundtripObjectPropertySetOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
        operation.setObjectProperty("XWiki.SomeClass", 0, "property", "  val<\">'ue   ");
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        this.doc.newObject("XWiki.SomeClass", this.context);
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nthird row");
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "second row\n", new PositionImpl(1, 0,
            "first row\n", "third"));
        assertEquals("first row\nthird row", this.doc.getObject("XWiki.SomeClass", 0).displayView("textarea",
            this.context));
        operation.apply(this.doc, this.context);
        assertEquals("first row\nsecond row\nthird row", this.doc.getObject("XWiki.SomeClass", 0).displayView(
            "textarea", this.context));
    }

    public void testApplyInvalidObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "second row\n", new PositionImpl(1, 0,
            "first row\n", "third row"));
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.newObject("XWiki.SomeClass", this.context);
        this.doc.setIntValue("XWiki.SomeClass", "number", 42);
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row and \n the third row");
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nthird row");
        operation.apply(this.doc, this.context);
        operation.insertInProperty("XWiki.SomeClass", 0, "number", "value", new PositionImpl());
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals("42", this.doc.getObject("XWiki.SomeClass", 0).displayView("number", this.context));
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl(0, 0, "invalid",
            "invalid"));
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectPropertyInsertAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
        operation.insertInProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl());
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testApplyObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        this.doc.newObject("XWiki.SomeClass", this.context);
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nsecond row\nthird row");
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "second row\n", new PositionImpl(1, 0,
            "first row\n", "second"));
        assertEquals("first row\nsecond row\nthird row", this.doc.getObject("XWiki.SomeClass", 0).displayView(
            "textarea", this.context));
        operation.apply(this.doc, this.context);
        assertEquals("first row\nthird row", this.doc.getObject("XWiki.SomeClass", 0).displayView("textarea",
            this.context));
    }

    public void testApplyInvalidObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "second row\n", new PositionImpl(1, 0,
            "first row\n", "second row\nthird row"));
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.newObject("XWiki.SomeClass", this.context);
        this.doc.setIntValue("XWiki.SomeClass", "number", 42);
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row and \n the third row");
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        this.doc.setLargeStringValue("XWiki.SomeClass", "textarea", "first row\nsecond row\nthird row");
        operation.apply(this.doc, this.context);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "number", "value", new PositionImpl());
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl(0, 0, "invalid",
            "invalid"));
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripObjectPropertyDeleteAtOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
        operation.deleteFromProperty("XWiki.SomeClass", 0, "textarea", "value", new PositionImpl());
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }
}
