package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.jcr.XWikiJcrBaseStore;
import com.xpn.xwiki.store.jcr.XWikiJcrBaseStore.JcrCallBack;
import com.xpn.xwiki.store.jcr.XWikiJcrSession;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;

public class JcrQuery implements IQuery {
	String	query,
			language;
	IQueryFactory qf;
	public JcrQuery(String query, String language, IQueryFactory qf) {
		this.query = query;
		this.language = language;
		this.qf = qf;
	}
	
	public XWikiJcrStore getStore() throws RepositoryException {
		return (XWikiJcrStore) ((XWikiJcrBaseStore)qf.getContext().getWiki().getNotCacheStore());
	}

    public String getNativeQuery() {
        return "store"+query;
    }

    public List list() throws XWikiException {
		final List result = new ArrayList();
		try {
			getStore().executeRead(qf.getContext(), new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Query q = session.getQueryManager().createQuery(getNativeQuery(), language);
					QueryResult qr = q.execute();
					RowIterator ri = qr.getRows();
					if (ri!=null) {
						if (firstResult > 0)
							ri.skip(firstResult);
						int count = maxResults > 0 ? maxResults : -1;
						while (ri.hasNext() && count--!=0) {
							Row row = ri.nextRow();
							Value[] values = row.getValues();
							if (values.length == 3) {
								result.add( fromValue(values[0]) );
							} else {
								Object os[] = new Object[values.length-2];
								for (int i=0; i<os.length-2; i++) {
									os[i] = fromValue( values[i] );
								}
								result.add(os);
							}
						}
					} else {
						NodeIterator ni = qr.getNodes();
						while (ni.hasNext()) {
							Node node = ni.nextNode();
							result.add(node.getPath());
						}
					}
					return null;
				}
			});
		} catch (Exception e) {
			Object[] args = { query };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SEARCH,
                    "Exception while searching in jcr with query {0}", e, args);
		}
		return result;
	}
	
	Object fromValue(Value v) throws ValueFormatException, IllegalStateException, RepositoryException {
		if (v==null) return null;
		switch (v.getType()) {
		case PropertyType.DATE: return v.getDate();
		case PropertyType.DOUBLE: return new Double(v.getDouble());
		case PropertyType.LONG: return new Long(v.getLong());
		case PropertyType.BOOLEAN: return new Boolean(v.getBoolean());
		default: return v.getString();
		}
	}

	int maxResults = 0;
	public IQuery setMaxResults(int fs) {
		this.maxResults = fs;
		return this;
	}
	int firstResult = 0;
	public IQuery setFirstResult(int fr) {
		this.firstResult = fr;
		return this;
	}

	public IQuery setDistinct(boolean d) {
		// TODO How?
		return this;
	}
}
