package org.xwiki.plugin.spacemanager.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
* A SpaceManagerExtension contains a list of methods that need to be implemented by specific SpaceManagers
*/
public interface SpaceManagerExtension {

    /**
     * Transform a title to a space name
     * @param spaceTitle title of the space
     * @param context
     * @return
     */
	public String getSpaceWikiName( String spaceTitle, XWikiContext context );

    /**
     * Transform a title to a unique space name
     * Adding a counter behind the space name
     * @param spaceTitle title of the space
     * @apram unique make name unique
     * @param context
     * @return
     */
	public String getSpaceWikiName( String spaceTitle, boolean unique, XWikiContext context );

    /**
     * API called to get the space type name
     * defaults to "space"
     * @return
     */
    public String getSpaceTypeName();

    /**
     * API called to get the space class name
     * defaults to XWiki.SpaceClass
     * @return
     */
    public String getSpaceClassName();

    /**
     * Does the system use custom mapping
     * @return
     */
    public boolean hasCustomMapping();

  /**
     * API called before the space in created
     *
     * @param spaceName
     * @param context
     * @throws SpaceManagerException when aborting the space creation process
     */
	public void preCreateSpace(String spaceName, XWikiContext context) throws SpaceManagerException;
	
	/**
     * API called after a space is created
     *
     * @param spaceName
     * @param context
	 * @throws SpaceManagerException 
     */
    public void postCreateSpace(String spaceName, XWikiContext context) throws SpaceManagerException;
	
	/**
     * API called before a space is deleted
     *
     * @param spaceName
     * @param deleteData
     * @param context
     * @return If it returns true, it will continue with the space creation, otherwise it will abord it
     */
    public boolean preDeleteSpace(String spaceName, boolean deleteData, XWikiContext context);
	
	/**
     * API called after a space is deleted
     *
     * @param spaceName
     * @param deleteData Determines if the data related with this space will also be deleted
     * @param context
     */
    public void postDeleteSpace(String spaceName, boolean deleteData, XWikiContext context);

    /**
     * Get member group name from the space name
     * @param spaceName
     */
    public String getMemberGroupName(String spaceName);

    /**
     * Get admin group name from the space name
     * @param spaceName
     */
    public String getAdminGroupName(String spaceName);

    /**
     * Get role group name from the space name
     * @param spaceName
     */
    public String getRoleGroupName(String spaceName, String role);
    
    /**
     * Initializes the SpaceManager extension in the main wiki 
     * @throws SpaceManagerException 
     */
    public void init(SpaceManager sm, XWikiContext context) throws SpaceManagerException;

    /**
     * Initializes the SpaceManager extension in the virtual wikis
     */
    public void virtualInit(SpaceManager sm, XWikiContext context) throws SpaceManagerException;

    /**
     * Get space user profile page name
     * @param userName
     * @param spaceName
     */
    public String getSpaceUserProfilePageName(String userName, String spaceName);
}