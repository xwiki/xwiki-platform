package org.xwiki.plugin.spacemanager.impl;

import org.xwiki.plugin.spacemanager.api.SpaceUserProfile;
import org.xwiki.plugin.spacemanager.api.SpaceManagerException;
import org.xwiki.plugin.spacemanager.api.SpaceManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.api.Document;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 13 déc. 2007
 * Time: 14:37:24
 * To change this template use File | Settings | File Templates.
 */
public class SpaceUserProfileImpl extends Document implements SpaceUserProfile {
    private static final String SPACE_USER_PROFILE_CLASS_NAME = "XWiki.SpaceUserProfileClass";
    private static final String SPACE_USER_PROFILE_PROFILE = "profile";
    private static final String SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS = "allowNotifications";
    private static final String SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS_FROM_SELF = "allowNotificationsFromSelf";

    private String userName;
    private SpaceManager manager;
    private Document userDoc;


    public SpaceUserProfileImpl(String userName, String spaceName, SpaceManager manager, XWikiContext context) throws XWikiException {
        super(null, context);
        this.manager = manager;
        this.userName = userName;
        String docName = manager.getSpaceUserProfilePageName(userName, spaceName);
        doc = context.getWiki().getDocument(docName, context);
        if (doc.getObject(getSpaceUserProfileClassName())==null)
         doc.newObject(getSpaceUserProfileClassName(), context);
    }

      /**
     *
     * @param context Xwiki context
     * @return Returns the Space User Profile Class as defined by the extension
     * @throws XWikiException
     */
    protected static BaseClass getSpaceUserProfileClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(getSpaceUserProfileClassName(), context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setFullName(getSpaceUserProfileClassName());
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(getSpaceUserProfileClassName());

        needsUpdate |= bclass.addTextAreaField(SPACE_USER_PROFILE_PROFILE, "Profile", 80, 7);
        needsUpdate |= bclass.addBooleanField(SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS, "Allow Notifications", "yesno");
        if (needsUpdate)
            ((BooleanClass) bclass.getField(SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS)).setDisplayType("checkbox");
        needsUpdate |= bclass.addBooleanField(SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS_FROM_SELF, "Allow Notifications From Self", "yesno");
        if (needsUpdate)
            ((BooleanClass) bclass.getField(SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS_FROM_SELF)).setDisplayType("checkbox");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Space User Profile Class");
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    protected static String getSpaceUserProfileClassName() {
        return SPACE_USER_PROFILE_CLASS_NAME;
    }

    protected Document getUserDocument() throws XWikiException {
        if (userDoc==null)
         userDoc = new Document(context.getWiki().getDocument(userName, context), context);
        return userDoc;
    }

    public String getProfile() {
        return doc.getStringValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_PROFILE);
    }

    public void setProfile(String profile) {
        getDoc().setLargeStringValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_PROFILE, profile);
    }

    public boolean getAllowNotifications() {
        return (doc.getIntValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS)==1);
    }

    public boolean getAllowNotificationsFromSelf() {
        return (doc.getIntValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS_FROM_SELF)==1);
    }

    public void setAllowNotifications(boolean allowNotifications) {
        getDoc().setIntValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS, allowNotifications ? 1 : 0);
    }

    public void setAllowNotificationsFromSelf(boolean allowNotificationsFromSelf) {
        getDoc().setIntValue(getSpaceUserProfileClassName(), SPACE_USER_PROFILE_ALLOW_NOTIFICATIONS_FROM_SELF, allowNotificationsFromSelf ? 1 : 0);
    }

    public void updateProfileFromRequest() throws SpaceManagerException {
        try {
            updateObjectFromRequest(getSpaceUserProfileClassName());
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    public String getUserProperty(String propName) {
        try {
            return (String) getUserDocument().getObject("XWiki.XWikiUsers").display(propName, "view");
        } catch (XWikiException e) {
            return "";
        }
    }

    public String getFirstName() {
        return getUserProperty("first_name");
    }

    public String getLastName() {
        return getUserProperty("last_name");
    }

    public String getEmail() {
        return getUserProperty("email");
    }

    public String getUserURL() {
        try {
            return getUserDocument().getURL();
        } catch (XWikiException e) {
            return "";
        }
    }

}
