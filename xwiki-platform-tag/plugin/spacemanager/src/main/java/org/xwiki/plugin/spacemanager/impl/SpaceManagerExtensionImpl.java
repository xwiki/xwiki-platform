package org.xwiki.plugin.spacemanager.impl;

import org.xwiki.plugin.spacemanager.api.*;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Implementation of a specific SpaceManagerExtension
 */
public class SpaceManagerExtensionImpl implements SpaceManagerExtension{

	protected SpaceManager sm = null;

    public String getSpaceTypeName() {
        return SpaceManager.SPACE_DEFAULT_TYPE;
    }

    public String getSpaceClassName() {
        return SpaceManager.SPACE_CLASS_NAME;
    }

    public boolean hasCustomMapping() {
        return false;
    }
    
	public void init(SpaceManager sm, XWikiContext context) throws SpaceManagerException {		
	}

	public void virtualInit(SpaceManager sm, XWikiContext context) throws SpaceManagerException {	
	}

    public String getSpaceUserProfilePageName(String userName, String spaceName) {
        return "UserProfiles_" + spaceName + "." + userName.substring(userName.indexOf(".") + 1);
    }

    /**
     * {@inheritDoc}
     */
	public void preCreateSpace(String spaceName, XWikiContext context) throws SpaceManagerException {
		// @todo: actions done before the creation of the space
	}
	
	/**
     * API called after a space is created
     *
     * @param spaceName
     * @param context
	 * @throws SpaceManagerException 
     */
    public void postCreateSpace(String spaceName, XWikiContext context) throws SpaceManagerException{
		// @todo: actions done after before the creation of the space
	}
	
	/**
     * API called before a space is deleted
     *
     * @param spaceName
     * @param deleteData
     * @param context
     * @return
     */
    public boolean preDeleteSpace(String spaceName, boolean deleteData, XWikiContext context){
		// @todo: actions done before before the deletion of a space
    	return true;
	}
	
	/**
     * API called after a space is deleted
     *
     * @param spaceName
     * @param deleteData
     * @param context
     */
    public void postDeleteSpace(String spaceName, boolean deleteData, XWikiContext context){
		// @todo: actions done after before the deletion of a space
	}

    public String getMemberGroupName(String spaceName) {
        return spaceName + ".MemberGroup";
    }

    public String getAdminGroupName(String spaceName) {
        return spaceName + ".AdminGroup";
    }

    public String getRoleGroupName(String spaceName, String role) {
        return spaceName + "." + role.substring(0,1).toUpperCase() + role.substring(1) + "Group";
    }

    public String getSpaceWikiName(String spaceName, XWikiContext context) {
        return getSpaceWikiName(spaceName, false, context);
    }

    public String getSpaceWikiName(String spaceName, boolean unique, XWikiContext context) {
        String prefix = "";
        try {
            prefix = context.getWiki().Param("xwiki.spacemanager.prefix", "");
        } catch (Exception e) {};

        String sName =  prefix + context.getWiki().clearName(spaceName, context);
        String uniqueName = sName;

        if (unique) {
            int counter = 1;
            while (true) {
                XWikiDocument doc = null;
                try {
                    doc = context.getWiki().getDocument(uniqueName, "WebPreferences", context);
                    if (doc.isNew())
                     break;
                } catch (XWikiException e) {
                    // silent failure
                }
                uniqueName = sName + counter;
                counter++;
            }
            sName = uniqueName;
        }        
        return sName;
	}

}
