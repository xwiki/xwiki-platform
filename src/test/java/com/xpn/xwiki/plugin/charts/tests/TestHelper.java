package com.xpn.xwiki.plugin.charts.tests;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.charts.source.TableDataSource;
import com.xpn.xwiki.plugin.charts.source.TableDataSourceFactory;

public class TestHelper {
	protected static BaseClass createTableDataSourceClass(XWikiContext context) throws XWikiException {
		String className = TableDataSourceFactory.XWIKI_CLASS_NAME;
		XWikiDocument classDoc = context.getWiki().getDocument(className, context);
		if (classDoc == null) {
			classDoc = new XWikiDocument();
			classDoc.setFullName(className, context);
			context.getWiki().saveDocument(classDoc, context);
		}
		BaseClass xclass = new BaseClass();
		xclass.setClassName(className);

		xclass.addNumberField(TableDataSource.TABLE_INDEX, TableDataSource.TABLE_INDEX, 30, "int");
		xclass.addTextField(TableDataSource.RANGE, TableDataSource.RANGE, 30);
		xclass.addBooleanField(TableDataSource.HAS_HEADER_ROW, TableDataSource.TABLE_INDEX, "");
		xclass.addBooleanField(TableDataSource.HAS_HEADER_COLUMN, TableDataSource.TABLE_INDEX, "");
		classDoc.setxWikiClass(xclass);
		context.getWiki().saveDocument(classDoc, context);
		return xclass;
	}
	
	protected static BaseObject defineTable(BaseClass xclass, XWikiDocument doc,
			XWikiContext context, int tableIndex, String range,
			boolean hasHeaderRow, boolean hasHeaderColumn) throws XWikiException {

		BaseObject xobject = (BaseObject)xclass.newObject();
        
        xobject.setIntValue(TableDataSource.TABLE_INDEX, tableIndex);
		xobject.setStringValue(TableDataSource.RANGE, range);
        xobject.setIntValue(TableDataSource.HAS_HEADER_ROW, hasHeaderRow?1:0);
        xobject.setIntValue(TableDataSource.HAS_HEADER_COLUMN, hasHeaderColumn?1:0);
        
        doc.addObject(xclass.getClassName(), xobject);
        
        xobject.setName(doc.getFullName());
        xobject.setClassName(xclass.getClassName());
        context.getWiki().saveDocument(doc, context);
		return xobject;
	}
	
	protected static XWikiDocument createDocument(String docName,
			String content, XWikiContext context) throws XWikiException {
		XWikiDocument doc = new XWikiDocument();
    	doc.setFullName(docName, context);
        doc.setContent(content);
        context.getWiki().saveDocument(doc, context);
        return doc;
	}
}
