package org.xwiki.plugin.spacemanager.plugin;

import java.util.List;

import org.xwiki.plugin.spacemanager.api.Space;
import org.xwiki.plugin.spacemanager.api.SpaceManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 28 nov. 2007
 * Time: 18:21:50
 * To change this template use File | Settings | File Templates.
 */
public class SpaceApi {
    private XWikiContext context;
    private Space space;

    public SpaceApi(Space space, XWikiContext context) {
        this.space = space;
        this.context = context;
    }

    /**
     * Gets the current space
     * @return
     */
    protected Space getSpace() {
        return space;
    }

    /**
     * Gets the display code for a field from the space document in the specified mode
     * @param fieldName The name of the fields to display
     * @param mode The mode in which to display the fields (eg: edit, view)  
     * @return 
     */
    public String display(String fieldName, String mode) {
        return getSpace().display(fieldName, mode);
    }
    
    /**
     * Checks if the space is newly created
     * @return
     */
    public boolean isNew() {
		return getSpace().isNew();
	}

    /**
     * Checks if the space was marked as deleted
     * @return
     */
    public boolean isDeleted() {
        return getSpace().isDeleted();
    }
    
    /** 
	 * Gets the reference of the creator of this space (Eg: XWiki.SampleUser) 
	 * @return
	 */
    public String getCreator() {
        return getSpace().getCreator();
    }

    /**
     * Sets the creator of the space
     * @param creator The creators reference (Eg: XWiki.SampleUser)
     */
    public void setCreator(String creator) {
        getSpace().setCreator(creator);
    }
    
    public String getSpaceName(){
    	return getSpace().getSpaceName();
    }
    
    /**
     * Gets the display title of the space
     * @return
     */
    public String getDisplayTitle() {
        return getSpace().getDisplayTitle();
    }

    /**
     * Sets the display title of the space
     * @param title The new title
     */
    public void setDisplayTitle(String title) {
        getSpace().setDisplayTitle(title);
     }
    
    /**
     * Gets the type of the space
     * @return
     */
    public String getType() {
        return getSpace().getType();
	}

	/**
	 * Sets the type of the space
	 * @param type Space Type
	 */
    public void setType(String type) {
       getSpace().setType(type);
	}
    
    /**
	 * Gets the description of the space
	 * @return
	 */
    public String getDescription() {
        return getSpace().getDescription();
    }

    /**
     * Sets the description of the space
     * @param description The new description
     */
    public void setDescription(String description) {
       getSpace().setDescription(description);
    }
    
    /**
     * Returns the URL for the homepage of this space
     * @return
     */
    public String getHomeShortcutURL() {
    	return getSpace().getHomeShortcutURL();
    }

    /**
     * Sets the URL for the homepage of the space
     * @param homeShortCutURL new URL the homepage of this space
     */
    public void setHomeShortcutURL(String homeShortCutURL) {
    	getSpace().setHomeShortcutURL(homeShortCutURL);
    }

    /**
     * Returns the URL to the Wiki Homepage of the space
     * @return
     * @throws SpaceManagerException
     */
    public String getHomeURL() throws SpaceManagerException {
        return getSpace().getHomeURL();
    }
    
    /**
     * Returns the list of fields in the Space class
     * @throws SpaceManagerException
     * @return
     */
    public List getFieldNames() throws SpaceManagerException {
    	return getSpace().getFieldNames();
    }
    
    /**
     * Saves the space
     * @throws SpaceManagerException
     */
    public void save(XWikiContext context) throws SpaceManagerException {
        try {
            getSpace().save();
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }
    
    /**
     * Validates the space data
     */
    public boolean validateSpaceData(){
    	return validateSpaceData();
    }
}
