package com.xpn.xwiki.user.impl.xwiki;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/*
  Implements a authentication mecanism which is trusting the App Server authentication
  If it fails it falls back to the standard XWiki authentication
 */
public class AppServerTrustedAuthServiceImpl  extends XWikiAuthServiceImpl {
    private static final Log log = LogFactory.getLog(AppServerTrustedAuthServiceImpl.class);

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(context);
        }
        else {
            createUser(user, context);
            user = "XWiki." + user;
        }
        context.setUser(user);
        return new XWikiUser(user);
    }

    /**
     * We cannot authenticate locally since we need to trust the app server for authentication
     * @param username
     * @param password
     * @param context
     * @return
     * @throws XWikiException
     */
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context) throws XWikiException {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(username, password, rememberme, context);
        }
        else {
            createUser(user, context);
            user = "XWiki." + user;
        }
        context.setUser(user);
        return new XWikiUser(user);
    }
}