package com.xpn.xwiki.plugin.charts.mocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.jrcs.rcs.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;


public class MockStore implements XWikiStoreInterface {
	private Map docMap = new HashMap();
	private Map objMap = new HashMap();

    public MockStore(XWiki xwiki, XWikiContext context) {
    	
    }
	

	public void saveXWikiDoc(XWikiDocument doc, XWikiContext context)
			throws XWikiException {
		docMap.put(doc.getFullName(), doc.clone());
		Collection objects = doc.getxWikiObjects().values();
		Iterator it = objects.iterator();
		while (it.hasNext()) {
			List list = new LinkedList((Vector)it.next());
			Iterator it2 = list.iterator();
			while (it2.hasNext()) {
				BaseObject obj = (BaseObject)it2.next();
				objMap.put(new Long(obj.getId()), obj);
			}
		}
	}

	public void saveXWikiDoc(XWikiDocument doc, XWikiContext context,
			boolean bTransaction) throws XWikiException {
		saveXWikiDoc(doc, context);
	}

	public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context)
			throws XWikiException {
		XWikiDocument document = (XWikiDocument)docMap.get(doc.getFullName());
		if (document != null) {
			doc.setNew(false);			
			return document;
		} else {
			doc.setNew(true);
			saveXWikiDoc(doc, context);
			return doc;
		}
	}

	public XWikiDocument loadXWikiDoc(XWikiDocument doc, String version,
			XWikiContext context) throws XWikiException {
		return loadXWikiDoc(doc, context); // only one version kept here
	}

	public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context)
			throws XWikiException {
		docMap.remove(doc.getFullName());
	}

	// TODO: how to implement this?
	public List getClassList(XWikiContext context) throws XWikiException {
		return null;
	}

	public List searchDocumentsNames(String wheresql, XWikiContext context)
			throws XWikiException {
		if (wheresql.equals("")) {
			List list = new LinkedList();
			Iterator it = docMap.keySet().iterator();
			while (it.hasNext()) {
				String docName = (String)it.next();
				list.add(docMap.get(docName));
			}
			return list;
		} else if (wheresql.matches("doc.fullName LIKE '.*'")) {
			List list = new LinkedList();
			String like = wheresql.substring("doc.fullName LIKE '".length(), wheresql.length()-1);
			like = like.replaceAll("%", ".*");
			Pattern pattern = Pattern.compile(like);
			Iterator it = docMap.keySet().iterator();
			while (it.hasNext()) {
				String docName = (String)it.next();
				if (pattern.matcher(docName).matches()) {
					list.add(docMap.get(docName));
				}
			}
			return list;
		}
		return null;
	}

	public List searchDocumentsNames(String wheresql, int nb, int start,
			XWikiContext context) throws XWikiException {
		return null;
	}

	public List searchDocuments(String wheresql, boolean distinctbyname,
			XWikiContext context) throws XWikiException {
		return null;
	}

    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, XWikiContext context) throws XWikiException {
        return null;
    }

    public List searchDocuments(String wheresql, boolean distinctbyname,
			int nb, int start, XWikiContext context) throws XWikiException {
		return null;
	}

    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException {
        return null;
    }

    public List searchDocuments(String wheresql, XWikiContext context)
			throws XWikiException {
		return null;
	}

	public List searchDocuments(String wheresql, int nb, int start,
			XWikiContext context) throws XWikiException {
		return null;
	}

	public void saveAttachmentContent(XWikiAttachment attachment,
			XWikiContext context, boolean bTransaction) throws XWikiException {

	}

	public void saveAttachmentContent(XWikiAttachment attachment,
			boolean bParentUpdate, XWikiContext context, boolean bTransaction)
			throws XWikiException {

	}

	public void loadAttachmentContent(XWikiAttachment attachment,
			XWikiContext context, boolean bTransaction) throws XWikiException {

	}

	public void loadAttachmentArchive(XWikiAttachment attachment,
			XWikiContext context, boolean bTransaction) throws XWikiException {

	}

	public void deleteXWikiAttachment(XWikiAttachment attachment,
			XWikiContext context, boolean bTransaction) throws XWikiException {

	}

	public XWikiLock loadLock(long docId, XWikiContext context,
			boolean bTransaction) throws XWikiException {
		return null;
	}

	public void saveLock(XWikiLock lock, XWikiContext context,
			boolean bTransaction) throws XWikiException {

	}

	public void deleteLock(XWikiLock lock, XWikiContext context,
			boolean bTransaction) throws XWikiException {

	}

	public List search(String sql, int nb, int start, XWikiContext context)
			throws XWikiException {
		String prefix1 = "from "+BaseObject.class.getName()+ " as obj where obj.id='";
		String prefix2 = "from "+BaseProperty.class.getName() + " as p where p.id.id='";
		
		if (sql.matches(prefix1+"-??[0-9]+'")) {
			String idString = sql.substring(prefix1.length(), sql.length()-1);
			Long id = new Long(Long.parseLong(idString));
			BaseObject obj = (BaseObject)objMap.get(id);
			List list = new LinkedList();
			list.add(obj);
			return list;
		} else if (sql.matches(prefix2+"-??[0-9]+'")) {
			String idString = sql.substring(prefix2.length(), sql.length()-1);
			Long id = new Long(Long.parseLong(idString));
			BaseObject obj = (BaseObject)objMap.get(id);
			List list = new LinkedList();
			Object[] prop = obj.getProperties();
			for (int i = 0; i<prop.length; i++) {
				list.add((BaseProperty)prop[i]);
			}
			return list;
		}
		return null;
	}

	public List search(String sql, int nb, int start, Object[][] whereParams,
			XWikiContext context) throws XWikiException {
		return null;
	}

	public void cleanUp(XWikiContext context) {

	}

	public void createWiki(String wikiName, XWikiContext context)
			throws XWikiException {

	}

	public boolean exists(XWikiDocument doc, XWikiContext context)
			throws XWikiException {
		return docMap.get(doc.getFullName()) != null;
	}

	public List searchDocumentsNames(String wheresql, int nb, int start,
			String selectColumns, XWikiContext context) throws XWikiException {
		return null;
	}
	
    public Version[] getXWikiDocVersions(XWikiDocument doc,
    		XWikiContext context) throws XWikiException {
    	return new Version[] { new Version(1) };
    }
    
    public List getCustomMappingPropertyList(BaseClass bclass) {
    	return null;
    }
    
    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException {
    	return false;
    }
    
    public void injectCustomMappings(XWikiContext context) throws XWikiException {
    }
    
    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException {
    	return false;
    }
    
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context) throws XWikiException {
    	return false;
    }

    public List loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException{
        return null;
    };
    public List loadBacklinks(String fullName, XWikiContext context, boolean bTransaction) throws XWikiException{
        return null;
    };
    public void saveLinks(List links, XWikiContext context, boolean bTransaction) throws XWikiException{};
    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException{};

}
