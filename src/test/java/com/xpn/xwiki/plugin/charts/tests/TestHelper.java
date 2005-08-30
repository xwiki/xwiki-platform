package com.xpn.xwiki.plugin.charts.tests;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.charts.source.TableDataSource;

public class TestHelper {
	protected static BaseClass createTableDataSourceClass(XWikiContext context) throws XWikiException {
		String className = "XWiki.TableDataSource";
		XWikiDocument classDoc = context.getWiki().getDocument(className, context);
		if (classDoc == null) {
			classDoc = new XWikiDocument();
			classDoc.setFullName(className, context);
			context.getWiki().saveDocument(classDoc, context);
		}
		BaseClass xclass = new BaseClass();
		xclass.setClassName(className);

		xclass.addNumberField(TableDataSource.TABLE_NUMBER, TableDataSource.TABLE_NUMBER, 30, "int");
		xclass.addTextField(TableDataSource.RANGE, TableDataSource.RANGE, 30);
		xclass.addBooleanField(TableDataSource.HAS_HEADER_ROW, TableDataSource.HAS_HEADER_ROW, "");
		xclass.addBooleanField(TableDataSource.HAS_HEADER_COLUMN, TableDataSource.HAS_HEADER_COLUMN, "");
		xclass.addTextField(TableDataSource.DECIMAL_SYMBOL, TableDataSource.DECIMAL_SYMBOL, 10);
		xclass.addBooleanField(TableDataSource.IGNORE_ALPHA, TableDataSource.IGNORE_ALPHA, "");
		classDoc.setxWikiClass(xclass);
		context.getWiki().saveDocument(classDoc, context);
		return xclass;
	}

	protected static BaseObject defineTable(BaseClass xclass, XWikiDocument doc,
			XWikiContext context, int tableNumber, String range,
			boolean hasHeaderRow, boolean hasHeaderColumn) throws XWikiException {
		return defineTable(xclass, doc, context, tableNumber, range, hasHeaderRow,
				hasHeaderColumn, TableDataSource.PERIOD_SELECTOR, false);
	}

	protected static BaseObject defineTable(BaseClass xclass, XWikiDocument doc,
			XWikiContext context, int tableNumber, String range,
			boolean hasHeaderRow, boolean hasHeaderColumn,
			String decimal, boolean ignoreAlpha) throws XWikiException {

		BaseObject xobject = (BaseObject)xclass.newObject();
        
        xobject.setIntValue(TableDataSource.TABLE_NUMBER, tableNumber);
		xobject.setStringValue(TableDataSource.RANGE, range);
        xobject.setIntValue(TableDataSource.HAS_HEADER_ROW, hasHeaderRow?1:0);
        xobject.setIntValue(TableDataSource.HAS_HEADER_COLUMN, hasHeaderColumn?1:0);
        xobject.setStringValue(TableDataSource.DECIMAL_SYMBOL, decimal);
        xobject.setIntValue(TableDataSource.IGNORE_ALPHA, ignoreAlpha?1:0);
        
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
