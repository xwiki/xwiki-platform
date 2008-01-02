package org.xwiki.platform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ClassPropertyOperationsTest extends TestCase
{
    Document domDoc;

    XWikiDocument doc;

    BaseClass bclass;

    protected void setUp()
    {
        try {
            domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc = new XWikiDocument();
            doc.setFullName("XWiki.XWikiTestClass");
            bclass = new BaseClass();
            bclass.setName("XWiki.XWikiTestClass");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void testApplyPropertyAddOperation() throws XWikiException
    {
        bclass.addTextField("prop1", "Property 1", 30);
        bclass.addBooleanField("prop2", "Property 2", "yesno");
        for (Iterator it = bclass.getFieldList().iterator(); it.hasNext();) {
            RWOperation operation = getOperation((PropertyClass) it.next(), true);
            operation.apply(doc);
        }
        assertEquals(2, doc.getxWikiClass().getProperties().length);
        assertNotNull(doc.getxWikiClass().getField("prop1"));
        assertNotNull(doc.getxWikiClass().getField("prop2"));
        assertNull(doc.getxWikiClass().getField("prop3"));
        assertEquals("prop1", doc.getxWikiClass().get("prop1").getName());
    }

    public void testApplyTwicePropertyAddOperation() throws XWikiException
    {
        bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) bclass.getProperties()[0], true);
        operation.apply(doc);
        operation.apply(doc);
        assertEquals(1, doc.getxWikiClass().getProperties().length);
        assertEquals("prop1", doc.getxWikiClass().get("prop1").getName());
    }

    public void testInvalidPropertyAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_ADD);
        operation.createType("invalid", new HashMap());
        try {
            operation.apply(doc);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals(0, doc.getxWikiClass().getProperties().length);
    }

    public void testXmlRoundtripPropertyAddOperation() throws XWikiException
    {
        bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) bclass.getProperties()[0], true);
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testApplyPropertySetOperation() throws XWikiException
    {
        testApplyPropertyAddOperation();
        ((PropertyClass) bclass.get("prop1")).setPrettyName("new Property 1");
        RWOperation operation = getOperation((PropertyClass) bclass.get("prop1"), false);
        operation.apply(doc);
        assertEquals("new Property 1", ((PropertyClass) doc.getxWikiClass().get("prop1"))
            .getPrettyName());
    }

    public void testXmlRoundtripPropertySetOperation() throws XWikiException
    {
        bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) bclass.getProperties()[0], true);
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testApplyPropertyDeleteOperation() throws XWikiException
    {
        testApplyPropertyAddOperation();
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType("prop1");
        operation.apply(doc);
        assertEquals(1, doc.getxWikiClass().getProperties().length);
        assertNull(doc.getxWikiClass().get("prop1"));
        assertNotNull(doc.getxWikiClass().get("prop2"));
    }

    public void testApplyInvalidPropertyDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType("prop3");
        try {
            operation.apply(doc);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertNull(doc.getxWikiClass().get("prop3"));
    }

    public void testXmlRoundtripPropertyDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType("prop1");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testConsecutiveClassOperations() throws XWikiException
    {
        bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) bclass.get("prop1"), true);
        operation.apply(doc);
        assertEquals(1, doc.getxWikiClass().getProperties().length);
        assertNotNull(doc.getxWikiClass().getField("prop1"));
        assertEquals("prop1", doc.getxWikiClass().get("prop1").getName());

        ((PropertyClass) bclass.get("prop1")).setPrettyName("new Property 1");
        operation = getOperation((PropertyClass) bclass.get("prop1"), false);
        operation.apply(doc);
        assertEquals("new Property 1", ((PropertyClass) doc.getxWikiClass().get("prop1"))
            .getPrettyName());
        assertEquals(1, doc.getxWikiClass().getProperties().length);

        bclass.addBooleanField("prop2", "Property 2", "yesno");
        operation = getOperation((PropertyClass) bclass.get("prop2"), true);
        operation.apply(doc);
        assertEquals(2, doc.getxWikiClass().getProperties().length);

        operation =
            OperationFactoryImpl.getInstance().newOperation(
                RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType("prop1");
        assertEquals(2, doc.getxWikiClass().getProperties().length);
        operation.apply(doc);
        assertEquals(1, doc.getxWikiClass().getProperties().length);
        assertNotNull(doc.getxWikiClass().getField("prop2"));
        assertNull(doc.getxWikiClass().getField("prop1"));
    }

    private RWOperation getOperation(PropertyClass property, boolean create)
        throws XWikiException
    {
        String type = property.getClass().getCanonicalName();
        Map config = new HashMap();
        for (Iterator it2 = property.getFieldList().iterator(); it2.hasNext();) {
            BaseProperty pr = (BaseProperty) it2.next();
            config.put(pr.getName(), pr.getValue());
        }
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                create ? RWOperation.TYPE_CLASS_PROPERTY_ADD
                    : RWOperation.TYPE_CLASS_PROPERTY_CHANGE);
        if (create) {
            operation.createType(type, config);
        } else {
            operation.modifyType(type, config);
        }
        return operation;
    }
}
