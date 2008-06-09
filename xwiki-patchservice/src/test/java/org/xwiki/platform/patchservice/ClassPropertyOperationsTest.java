package org.xwiki.platform.patchservice;

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

import com.xpn.xwiki.XWikiContext;
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

    XWikiContext context;

    @Override
    protected void setUp()
    {
        try {
            this.domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            this.doc = new XWikiDocument();
            this.doc.setFullName("XWiki.XWikiTestClass");
            this.bclass = new BaseClass();
            this.bclass.setName("XWiki.XWikiTestClass");
            this.context = new XWikiContext();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void testApplyPropertyAddOperation() throws XWikiException
    {
        this.bclass.addTextField("prop1", "Property 1", 30);
        this.bclass.addBooleanField("prop2", "Property 2", "yesno");
        for (Iterator<PropertyClass> it = this.bclass.getFieldList().iterator(); it.hasNext();) {
            RWOperation operation = getOperation(it.next(), true);
            operation.apply(this.doc, this.context);
        }
        assertEquals(2, this.doc.getxWikiClass().getProperties().length);
        assertNotNull(this.doc.getxWikiClass().getField("prop1"));
        assertNotNull(this.doc.getxWikiClass().getField("prop2"));
        assertNull(this.doc.getxWikiClass().getField("prop3"));
        assertEquals("prop1", this.doc.getxWikiClass().get("prop1").getName());
    }

    public void testApplyTwicePropertyAddOperation() throws XWikiException
    {
        this.bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) this.bclass.getProperties()[0], true);
        operation.apply(this.doc, this.context);
        operation.apply(this.doc, this.context);
        assertEquals(1, this.doc.getxWikiClass().getProperties().length);
        assertEquals("prop1", this.doc.getxWikiClass().get("prop1").getName());
    }

    public void testInvalidPropertyAddOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_ADD);
        operation.createType(this.doc.getFullName(), "invalid", "invalid", new HashMap<String, Object>());
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertEquals(0, this.doc.getxWikiClass().getProperties().length);
    }

    public void testXmlRoundtripPropertyAddOperation() throws XWikiException
    {
        this.bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) this.bclass.getProperties()[0], true);
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testApplyPropertySetOperation() throws XWikiException
    {
        testApplyPropertyAddOperation();
        ((PropertyClass) this.bclass.get("prop1")).setPrettyName("new Property 1");
        RWOperation operation = getOperation((PropertyClass) this.bclass.get("prop1"), false);
        operation.apply(this.doc, this.context);
        assertEquals("new Property 1", ((PropertyClass) this.doc.getxWikiClass().get("prop1")).getPrettyName());
    }

    public void testXmlRoundtripPropertySetOperation() throws XWikiException
    {
        this.bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) this.bclass.getProperties()[0], true);
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testApplyPropertyDeleteOperation() throws XWikiException
    {
        testApplyPropertyAddOperation();
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType(this.doc.getFullName(), "prop1");
        operation.apply(this.doc, this.context);
        assertEquals(1, this.doc.getxWikiClass().getProperties().length);
        assertNull(this.doc.getxWikiClass().get("prop1"));
        assertNotNull(this.doc.getxWikiClass().get("prop2"));
    }

    public void testApplyInvalidPropertyDeleteOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType(this.doc.getFullName(), "prop3");
        try {
            operation.apply(this.doc, this.context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
        assertNull(this.doc.getxWikiClass().get("prop3"));
    }

    public void testXmlRoundtripPropertyDeleteOperation() throws XWikiException
    {
        RWOperation operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType(this.doc.getFullName(), "prop1");
        Element e = operation.toXml(this.domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testConsecutiveClassOperations() throws XWikiException
    {
        this.bclass.addTextField("prop1", "Property 1", 30);
        RWOperation operation = getOperation((PropertyClass) this.bclass.get("prop1"), true);
        operation.apply(this.doc, this.context);
        assertEquals(1, this.doc.getxWikiClass().getProperties().length);
        assertNotNull(this.doc.getxWikiClass().getField("prop1"));
        assertEquals("prop1", this.doc.getxWikiClass().get("prop1").getName());

        ((PropertyClass) this.bclass.get("prop1")).setPrettyName("new Property 1");
        operation = getOperation((PropertyClass) this.bclass.get("prop1"), false);
        operation.apply(this.doc, this.context);
        assertEquals("new Property 1", ((PropertyClass) this.doc.getxWikiClass().get("prop1")).getPrettyName());
        assertEquals(1, this.doc.getxWikiClass().getProperties().length);

        this.bclass.addBooleanField("prop2", "Property 2", "yesno");
        operation = getOperation((PropertyClass) this.bclass.get("prop2"), true);
        operation.apply(this.doc, this.context);
        assertEquals(2, this.doc.getxWikiClass().getProperties().length);

        operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CLASS_PROPERTY_DELETE);
        operation.deleteType(this.doc.getFullName(), "prop1");
        assertEquals(2, this.doc.getxWikiClass().getProperties().length);
        operation.apply(this.doc, this.context);
        assertEquals(1, this.doc.getxWikiClass().getProperties().length);
        assertNotNull(this.doc.getxWikiClass().getField("prop2"));
        assertNull(this.doc.getxWikiClass().getField("prop1"));
    }

    @SuppressWarnings("unchecked")
    private RWOperation getOperation(PropertyClass property, boolean create) throws XWikiException
    {
        String type = property.getClass().getCanonicalName();
        Map<String, Object> config = new HashMap<String, Object>();
        for (Iterator<BaseProperty> it2 = property.getFieldList().iterator(); it2.hasNext();) {
            BaseProperty pr = it2.next();
            config.put(pr.getName(), pr.getValue());
        }
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(
                create ? RWOperation.TYPE_CLASS_PROPERTY_ADD : RWOperation.TYPE_CLASS_PROPERTY_CHANGE);
        if (create) {
            operation.createType(this.doc.getFullName(), property.getName(), type, config);
        } else {
            operation.modifyType(this.doc.getFullName(), property.getName(), config);
        }
        return operation;
    }
}
