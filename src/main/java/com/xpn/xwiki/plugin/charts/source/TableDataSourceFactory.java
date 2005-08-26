package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class TableDataSourceFactory
        extends AbstractDataSourceFactory
        implements DataSourceFactory {
    
    public static final String XWIKI_CLASS_NAME = "XWiki.TableDataSource";

    private static DataSourceFactory uniqueInstance = new TableDataSourceFactory();

    private TableDataSourceFactory() {
        // empty
    }

    public static DataSourceFactory getInstance() {
        return uniqueInstance;
    }

    public DataSource create(String[] args, XWikiContext context)
            throws DataSourceException {
        checkArgumentCount(args, 3);

        String docName = args[1];
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(docName, context);
        } catch (XWikiException e) {
            throw new DataSourceException(e);
        }
        int number;
        try {
            number = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new DataSourceException(e);
        }

        BaseObject xobj = doc.getObject(XWIKI_CLASS_NAME, number);
        if (xobj == null) {
            throw new DataSourceException(XWIKI_CLASS_NAME + "#"
                    + number + " object not found");
        }

        return new TableDataSource(xobj, context);
    }
}
