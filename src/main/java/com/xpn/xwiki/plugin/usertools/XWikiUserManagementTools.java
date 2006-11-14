package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Created by IntelliJ IDEA.
 * User: jeremi
 * Date: Aug 18, 2006
 * Time: 4:11:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiUserManagementTools {

    public String inviteUser(String name, String email, XWikiContext context) throws XWikiException;
    public boolean resendInvitation(String email, XWikiContext context) throws XWikiException;
    public String getUserSpace(XWikiContext context);
    public String getUserPage(String email, XWikiContext context);
    public boolean isValidEmail(String email);
    public String getUserName(String userPage, XWikiContext context) throws XWikiException;
    public String getEmail(String userPage, XWikiContext context) throws XWikiException;
}
