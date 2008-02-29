package org.xwiki.platform.patchservice;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;
import org.xwiki.platform.patchservice.impl.PositionImpl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentOperationsTest extends TestCase
{
    Document domDoc;

    XWikiDocument doc;

    XWikiContext context;

    protected void setUp()
    {
        try {
            domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc = new XWikiDocument();
            context = new XWikiContext();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void testApplyContentInsertOperation() throws XWikiException
    {
        doc.setContent("this is the content");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
        operation.insert("new ", new PositionImpl(0, 12));
        operation.apply(doc, context);
        assertEquals("this is the new content", doc.getContent());
    }

    public void testXmlRoundtripContentInsertOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
        operation.insert("added <con\"ten>t", new PositionImpl(0, 10));
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testInvalidContentInsertOperation() throws XWikiException
    {
        doc.setContent("this is the short content");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
        operation.insert("something", new PositionImpl(0, 42));
        try {
            operation.apply(doc, context);
            assertFalse(true);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testApplyContentDeleteOperation() throws XWikiException
    {
        doc.setContent("this is the old content");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
        operation.delete("old ", new PositionImpl(0, 12));
        operation.apply(doc, context);
        assertEquals("this is the content", doc.getContent());
    }

    public void testXmlRoundtripContentDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
        operation.delete("something", new PositionImpl(0, 10));
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }

    public void testInvalidContentDeleteOperation() throws XWikiException
    {
        doc.setContent("this is the short content");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
        operation.delete("something", new PositionImpl(0, 42));
        try {
            operation.apply(doc, context);
            assertFalse(true);
        } catch (XWikiException ex) {
            // This is expected
        }
        operation.delete("this", new PositionImpl(0, 2));
        try {
            operation.apply(doc, context);
            assertFalse(true);
        } catch (XWikiException ex) {
            // This is expected
        }
    }

    public void testConsecutiveContentInsertDeleteOperations() throws XWikiException
    {
        doc.setContent("this is the old content");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
        operation.delete("old", new PositionImpl(0, 12));
        operation.apply(doc, context);
        operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
        operation.insert("new", new PositionImpl(0, 12));
        operation.apply(doc, context);
        assertEquals("this is the new content", doc.getContent());
        operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
        operation.insert("restored", new PositionImpl(0, 15));
        operation.apply(doc, context);
        operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
        operation.delete("new", new PositionImpl(0, 12));
        operation.apply(doc, context);
        assertEquals("this is the restored content", doc.getContent());
    }
}
