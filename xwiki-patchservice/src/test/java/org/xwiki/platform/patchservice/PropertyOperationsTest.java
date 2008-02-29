package org.xwiki.platform.patchservice;

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

public class PropertyOperationsTest extends TestCase
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

    public void testApplyPropertySetOperation() throws XWikiException
    {
        doc.setAuthor("XWiki.Admin");
        doc.setCreator("XWiki.Admin");
        doc.setLanguage("en");
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_PROPERTY_SET);
        operation.setProperty("author", "XWiki.XWikiGuest");
        operation.apply(doc, context);
        operation.setProperty("language", "fr");
        operation.apply(doc, context);
        assertEquals("XWiki.XWikiGuest", doc.getAuthor());
        assertEquals("fr", doc.getLanguage());
        assertEquals("XWiki.Admin", doc.getCreator());
    }

    public void testInvalidPropertySetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_PROPERTY_SET);
        operation.setProperty("invalidPropertyName", "value");
        try {
            operation.apply(doc, context);
            assertTrue(false);
        } catch (XWikiException ex) {
            assertTrue(true);
        }
    }

    public void testXmlRoundtripPropertySetOperation() throws XWikiException
    {
        RWOperation operation =
            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_PROPERTY_SET);
        operation.setProperty("property", "value");
        Element e = operation.toXml(domDoc);
        Operation loadedOperation = OperationFactoryImpl.getInstance().loadOperation(e);
        assertEquals(operation, loadedOperation);
    }
}
