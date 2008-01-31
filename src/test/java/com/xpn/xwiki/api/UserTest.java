package com.xpn.xwiki.api;

import org.jmock.cglib.MockObjectTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Unit tests for {@link com.xpn.xwiki.api.User}.
 * 
 * @version $Id: $
 */
public class UserTest extends MockObjectTestCase
{
    /**
     * Checks that XWIKI-2040 remains fixed.
     */
    public void testIsUserInGroupDoesNotThrowNPE()
    {
        User u = new User(null, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiUser xu = new XWikiUser(null);
        u = new User(xu, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiContext c = new XWikiContext();
        u = new User(xu, c);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));
    }
}
