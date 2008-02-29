package org.xwiki.platform.patchservice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentOperationsTest extends TestCase
{
    Document domDoc;

    XWikiDocument doc;

    XWikiContext context;

    InputStream helloContent;

    protected void setUp()
    {
        try {
            domDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc = new XWikiDocument();
            context = new XWikiContext();
            helloContent = new ByteArrayInputStream("hello".getBytes());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void testApplyAttachmentAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_ADD);
        operation.addAttachment(helloContent, "file.txt", "XWiki.Admin");
        assertNull(doc.getAttachment("file.txt"));
        operation.apply(doc, context);
        assertNotNull(doc.getAttachment("file.txt"));
        assertTrue(ArrayUtils.isEquals("hello".getBytes(), doc.getAttachment("file.txt")
            .getContent(context)));
    }

    public void testApplyTwiceAttachmentAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_ADD);
        operation.addAttachment(helloContent, "file.txt", "XWiki.Admin");
        assertNull(doc.getAttachment("file.txt"));
        operation.apply(doc, context);
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripAttachmentAddOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_ADD);
        operation.addAttachment(helloContent, "file.txt", "XWiki.Admin");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    public void testApplyAttachmentSetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_SET);
        XWikiAttachment a = new XWikiAttachment(doc, "file.txt");
        a.setContent("old hello".getBytes());
        doc.getAttachmentList().add(a);
        assertNotNull(doc.getAttachment("file.txt"));
        operation.setAttachment(helloContent, "file.txt", "XWiki.Admin");
        assertTrue(ArrayUtils.isEquals("old hello".getBytes(), doc.getAttachment("file.txt")
            .getContent(context)));
        operation.apply(doc, context);
        assertNotNull(doc.getAttachment("file.txt"));
        assertTrue(ArrayUtils.isEquals("hello".getBytes(), doc.getAttachment("file.txt")
            .getContent(context)));
    }

    public void testApplyInvalidAttachmentSetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_SET);
        operation.setAttachment(helloContent, "file.txt", "XWiki.Admin");
        assertNull(doc.getAttachment("file.txt"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripAttachmentSetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_SET);
        operation.setAttachment(helloContent, "file.txt", "XWiki.Admin");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }

    // We can't easily test the attachment delete due to XWIKI-1982

    public void testApplyInvalidAttachmentDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_DELETE);
        operation.deleteAttachment("file.txt");
        assertNull(doc.getAttachment("file.txt"));
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripAttachmentDeleteOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_DELETE);
        operation.deleteAttachment("file.txt");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(loadedOperation, operation);
    }
}
