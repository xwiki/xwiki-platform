package com.xpn.xwiki.plugin.lucene;

import com.xpn.xwiki.store.XWikiStoreInterface;
import org.xwiki.query.QueryManager;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;

import java.util.List;
import java.util.LinkedList;

public class TestStore implements XWikiStoreInterface
{
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {

        return null;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
    }

    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public int countDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return 0;
    }

    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<String>();
    }

    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public List<String> searchDocumentsNames(String parametrizedSqlClause, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public int countDocuments(String parametrizedSqlClause, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return 0;
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return new LinkedList<XWikiDocument>();
    }

    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return null;
    }

    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
    }

    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
    }

    public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return new LinkedList<XWikiLink>();
    }

    public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return new LinkedList<String>();
    }

    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
    }

    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
    }

    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return new LinkedList<T>();
    }

    public <T> List<T> search(String sql, int nb, int start, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<T>();
    }

    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return new LinkedList<T>();
    }

    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return new LinkedList<T>();
    }

    public void cleanUp(XWikiContext context)
    {
    }

    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        return false;
    }

    public void createWiki(String wikiName, XWikiContext context) throws XWikiException
    {
    }

    public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException
    {
    }

    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        if (doc.getFullName().equals("Lorem.Ipsum"))
            return true;
        return false;
    }

    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
        throws XWikiException
    {
        return false;
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException
    {
        return false;
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return false;
    }

    public List<String> getCustomMappingPropertyList(BaseClass bclass)
    {
        return new LinkedList<String>();
    }

    public void injectCustomMappings(XWikiContext context) throws XWikiException
    {
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
    }

    public List<String> getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return new LinkedList<String>();
    }

    public QueryManager getQueryManager()
    {
        return null;
    }
}
