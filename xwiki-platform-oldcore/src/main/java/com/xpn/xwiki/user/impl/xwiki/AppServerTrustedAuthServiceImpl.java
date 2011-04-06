package com.xpn.xwiki.user.impl.xwiki;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Implements a authentication mecanism which is trusting the App Server authentication. If it fails it falls back to
 * the standard XWiki authentication.
 * 
 * @version $Id$
 */
public class AppServerTrustedAuthServiceImpl extends XWikiAuthServiceImpl
{
    private static final Log log = LogFactory.getLog(AppServerTrustedAuthServiceImpl.class);

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(context);
        } else {
            if (log.isDebugEnabled())
                log.debug("Launching create user for " + user);
            createUser(user, context);
            if (log.isDebugEnabled())
                log.debug("Create user done for " + user);
            user = "XWiki." + user;
        }
        context.setUser(user);

        return new XWikiUser(user);
    }

    /**
     * We cannot authenticate locally since we need to trust the app server for authentication.
     */
    @Override
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(username, password, rememberme, context);
        } else {
            createUser(user, context);
            user = "XWiki." + user;
        }
        context.setUser(user);

        return new XWikiUser(user);
    }
}
