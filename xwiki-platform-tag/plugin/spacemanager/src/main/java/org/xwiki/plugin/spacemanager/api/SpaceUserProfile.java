package org.xwiki.plugin.spacemanager.api;

import com.xpn.xwiki.XWikiException;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 30 nov. 2007
 * Time: 00:10:14
 * To change this template use File | Settings | File Templates.
 */
public interface SpaceUserProfile {

    /**
     * Profile text of the user in this group
     * @return
     */
    public String getProfile();

    /**
     * Allows to modify the profile text of the user in the group
     * It is needed to call "save" to really make the change
     * @param profile
     */
    public void setProfile(String profile);

    /**
     * Setting to see if the user wishes to receive email notifications
     * @return
     */
    public boolean getAllowNotifications();

    /**
     * Setting to see if the user wishes to receive email notifications on his own changes
     * @return
     */
    public boolean getAllowNotificationsFromSelf();

    /**
     * Allows to change the email notification setting
     * @param allowNotifications
     */
    public void setAllowNotifications(boolean allowNotifications);

    /**
     * Allows to change the email notification from self setting
     * @param allowNotificationsFromSelf
     */
    public void setAllowNotificationsFromSelf(boolean allowNotificationsFromSelf);

    /**
     * Allows to update the profile settings from the request object
     * @throws SpaceManagerException
     */
    public void updateProfileFromRequest() throws SpaceManagerException;

    /**
     * Saves changes made to the profile
     * @throws XWikiException
     */
    public void save() throws XWikiException;

    /**
     * Saves changes made to the profile
     * @throws XWikiException
     */
    public void saveWithProgrammingRights() throws XWikiException;

    /**
     * Allows to retrieve a user property from the User page
     * @param propName
     * @return
     */
    public String getUserProperty(String propName);

    /**
     * Allows to retrieve the first name of the user
     * @return
     */
    public String getFirstName();

    /**
     * Allows to retrieve the last name of the user
     * @return
     */
    public String getLastName();

    /**
     * Allows to retrieve the email of the user
     * @return
     */
    public String getEmail();

    /**
     * Allows to retrieve the user URL
     * @return
     */
    public String getUserURL();

}
