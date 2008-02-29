package org.xwiki.platform.patchservice;

import junit.framework.TestCase;

import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;

import com.xpn.xwiki.XWikiException;

public class CheckAllOperationsImplementedTest extends TestCase
{
    public void testAllOperationsImplemented() throws XWikiException
    {
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_CONTENT_INSERT));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_CONTENT_DELETE));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_PROPERTY_SET));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_CLASS_PROPERTY_ADD));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_CLASS_PROPERTY_CHANGE));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_CLASS_PROPERTY_DELETE));
        assertNotNull(OperationFactoryImpl.getInstance()
            .newOperation(RWOperation.TYPE_OBJECT_ADD));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_OBJECT_DELETE));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_OBJECT_PROPERTY_SET));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_ATTACHMENT_ADD));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_ATTACHMENT_SET));
        assertNotNull(OperationFactoryImpl.getInstance().newOperation(
            RWOperation.TYPE_ATTACHMENT_DELETE));
    }

    /* Uncomment to get a sample of the XML output.when running tests. */
//    public void testXmlOutput() throws Exception
//    {
//        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
//        Element root = doc.createElement("patch");
//        doc.appendChild(root);
//        root.appendChild(doc.createTextNode("\n"));
//
//        RWOperation operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_INSERT);
//        operation.insert(" as/<>\"'&d ", new PositionImpl(2, 3, " \"'<>befo&amp;re", "after "));
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_CONTENT_DELETE);
//        operation.delete("Here ", new PositionImpl(0, 0));
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_PROPERTY_SET);
//        operation.setProperty("prop", "val");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        BaseClass b = new BaseClass();
//        b.addBooleanField("field", "The field", "yesno");
//        operation = getOperation((PropertyClass) b.get("field"), true);
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation = getOperation((PropertyClass) b.get("field"), false);
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(
//                RWOperation.TYPE_CLASS_PROPERTY_DELETE);
//        operation.deleteType("XWiki.Class", "prop1");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation = OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_ADD);
//        operation.addObject("XWiki.Class");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_DELETE);
//        operation.deleteObject("XWiki.Class", 2);
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_OBJECT_PROPERTY_SET);
//        operation.setObjectProperty("XWiki.Class", 2, "propertyName", "value");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(
//                RWOperation.TYPE_OBJECT_PROPERTY_INSERT_AT);
//        operation.insertInProperty("XWiki.Class", 0, "property", "inserted text",
//            new PositionImpl(2, 0));
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(
//                RWOperation.TYPE_OBJECT_PROPERTY_DELETE_AT);
//        operation.deleteFromProperty("XWiki.Class", 0, "property", "deleted text",
//            new PositionImpl(2, 0));
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_ADD);
//        operation.addAttachment(new ByteArrayInputStream("hello".getBytes()), "file", "XWiki.Me");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_SET);
//        operation.setAttachment(new ByteArrayInputStream("hello".getBytes()), "file", "XWiki.Me");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        operation =
//            OperationFactoryImpl.getInstance().newOperation(RWOperation.TYPE_ATTACHMENT_DELETE);
//        operation.deleteAttachment("file");
//        root.appendChild(operation.toXml(doc));
//        root.appendChild(doc.createTextNode("\n"));
//
//        DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();
//        System.out.println(ls.createLSSerializer().writeToString(doc));
//    }
//
//    private RWOperation getOperation(PropertyClass property, boolean create)
//        throws XWikiException
//    {
//        String type = property.getClass().getCanonicalName();
//        Map config = new HashMap();
//        for (Iterator it2 = property.getFieldList().iterator(); it2.hasNext();) {
//            BaseProperty pr = (BaseProperty) it2.next();
//            config.put(pr.getName(), pr.getValue());
//        }
//        RWOperation operation =
//            OperationFactoryImpl.getInstance().newOperation(
//                create ? RWOperation.TYPE_CLASS_PROPERTY_ADD
//                    : RWOperation.TYPE_CLASS_PROPERTY_CHANGE);
//        if (create) {
//            operation.createType("XWiki.Class", property.getName(), type, config);
//        } else {
//            operation.modifyType("XWiki.Class", property.getName(), config);
//        }
//        return operation;
//    }
}
