package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public interface XWikiUserManagementTools {

    public String inviteUser(String name, String email, XWikiContext context) throws XWikiException;
    public boolean resendInvitation(String email, XWikiContext context) throws XWikiException;
    public String getUserSpace(XWikiContext context);
    public String getUserPage(String email, XWikiContext context);
    public boolean isValidEmail(String email);
    public String getUserName(String userPage, XWikiContext context) throws XWikiException;
    public String getEmail(String userPage, XWikiContext context) throws XWikiException;
}
