package com.xpn.xwiki.api;

import com.opensymphony.module.access.NotFoundException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 15 mars 2004
 * Time: 01:54:43
 * To change this template use File | Settings | File Templates.
 */
public class Api {
    protected XWikiContext context;

    public Api(XWikiContext context) {
       this.context = context;
    }

    public boolean checkProgrammingRights() {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        XWikiDocInterface doc = context.getDoc();
        String username = doc.getAuthor();

        String docname;
        if (context.getDatabase()!=null) {
          docname = context.getDatabase() + ":" + doc.getFullName();
          username = context.getDatabase() + ":" + username;
        }
        else
          docname = doc.getFullName();

        try {
            return xwiki.getAccessmanager().userHasAccessLevel(username, docname, "programming");
        } catch (NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

}
