package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 17:50:47
 * To change this template use File | Settings | File Templates.
 */
public class XWiki {
    private com.xpn.xwiki.XWiki xwiki;
    private XWikiContext context;

    public XWiki(com.xpn.xwiki.XWiki xwiki, XWikiContext context) {
       this.xwiki = xwiki;
       this.context = context;
    }

     public String getVersion() {
          return xwiki.getVersion();
     }

     public String getRealPath(String path) {
          return xwiki.getRealPath(path);
     }

     public Document getDocument(String fullname) throws XWikiException {
         return new Document(xwiki.getDocument(fullname, context), context);
     }

     public Document getDocument(String web, String fullname, XWikiContext context) throws XWikiException {
         return new Document(xwiki.getDocument(web, fullname, context), context);
     }

     public String getBase() {
         return xwiki.getBase();
     }

     public String getFormEncoded(String content) {
        return xwiki.getFormEncoded(content);
     }

     public String getXMLEncoded(String content) {
        return xwiki.getXMLEncoded(content);
     }

     public String getTextArea(String content) {
        return xwiki.getTextArea(content);
     }


    public List getClassList() throws XWikiException {
        return xwiki.getClassList(context);
    }

    public List search(String wheresql) throws XWikiException {
        return xwiki.search(wheresql, context);
    }

    public List search(String wheresql, int nb, int start) throws XWikiException {
        return xwiki.search(wheresql, nb, start, context);
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return xwiki.searchDocuments(wheresql, context);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        return xwiki.searchDocuments(wheresql, nb, start, context);
    }

    public String getTemplate(String template) {
        return xwiki.getTemplate(template, context);
    }

    public String getSkin() {
        return xwiki.getSkin(context);
    }

    public String getWebCopyright() {
        return xwiki.getWebCopyright(context);
    }

    public String getXWikiPreference(String prefname) {
        return xwiki.getXWikiPreference(prefname, context);
    }

    public String getWebPreference(String prefname) {
        return xwiki.getWebPreference(prefname, context);
    }

    public String getUserPreference(String prefname) {
        return xwiki.getUserPreference(prefname, context);
    }

    public void flushCache() {
        xwiki.flushCache();
    }

    public int createUser() throws XWikiException {
        return xwiki.createUser(context);
    }

    public String includeTopic(String topic) {
        return xwiki.includeTopic(topic, context);
    }

    public String includeForm(String topic) {
        return xwiki.includeForm(topic, context);
    }

}
