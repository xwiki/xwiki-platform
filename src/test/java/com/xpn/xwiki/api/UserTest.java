package com.xpn.xwiki.api;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Unit tests for {@link com.xpn.xwiki.api.User}.
 * 
 * @version $Id$
 */
public class UserTest extends MockObjectTestCase
{
    private Mock mockXWiki;

    private XWikiContext context;

    protected void setUp() throws XWikiException
    {
        this.context = new XWikiContext();
        this.mockXWiki = mock(XWiki.class);
        context.setWiki((XWiki) mockXWiki.proxy());
        XWikiDocument doc = new XWikiDocument("XWiki", "Admin");
        BaseClass userClass = new BaseClass();
        userClass.addTextField("email", "email address", 20);
        mockXWiki.stubs().method("getClass").will(returnValue(userClass));
        BaseObject userObj = doc.newObject("XWiki.XWikiUsers", context);
        userObj.setStringValue("email", "admin@mail.com");
        mockXWiki.stubs().method("getDocument").will(returnValue(doc));
    }

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

    public void testGetEmail()
    {
        User u = new User(null, null);
        assertNull(u.getEmail());

        XWikiUser xu = new XWikiUser("XWiki.Admin");
        u = new User(xu, context);
        assertEquals("admin@mail.com", u.getEmail());
    }
}
