/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */
package org.xwiki.plugin.spacemanager.impl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import org.xwiki.plugin.spacemanager.api.Space;
import org.xwiki.plugin.spacemanager.api.SpaceManagerException;

import java.util.List;
import java.util.Date;

/**
 * Implementing class
 */
public class SpaceImpl extends Document implements Space {

    public static final String SPACE_DISPLAYTITLE = "displayTitle";
    public static final String SPACE_DESCRIPTION = "description";
    public static final String SPACE_TYPE = "type";
    public static final String SPACE_URLSHORTCUT = "urlshortcut";
    public static final String SPACE_POLICY = "policy";
	public static final String SPACE_LANGUAGE = "language";
	protected SpaceManagerImpl manager = null;

    /**
	 * Space constructor creating a space if does not exist
	 * @param spaceTitle The display name of the space
	 * @throws SpaceManagerException 
	 */
	public SpaceImpl(String spaceName, String spaceTitle, boolean create, SpaceManagerImpl manager, XWikiContext context) throws SpaceManagerException{
        super(null, context);
		this.manager = manager; 

        // if we haven't been provided with a space name we computer it from the space title
        if (spaceName==null)
         spaceName = manager.getSpaceWikiName(spaceTitle, true, context );

        // we init the space document
        initSpaceDoc(spaceName);

        // if we are asked to create the space
        if (create) {
            // we created it if it does not yet exist, otherwise throw exception
            if (isNew())
             createNewSpaceDoc(spaceTitle);
            else
             throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER, SpaceManagerException.ERROR_SPACE_ALREADY_EXISTS, "Space already exists");
        }
    }

    /**
     * Checks if the space was marked as deleted
     * @return
     */
    public boolean isDeleted() {
        return !(getType().equals(manager.getSpaceTypeName()));
    }

    /**
     * Gets the name of the space
     * @return
     */
    public String getSpaceName() {
        return doc.getSpace();
    }

    /**
     * Initializes (gets or creates) the XWiki documet for this space
     * @param spaceName The name of this space
     * @throws SpaceManagerException
     */
    protected void initSpaceDoc(String spaceName) throws SpaceManagerException{
    	String docName = manager.getSpaceDocumentName( spaceName );
		
    	try {
			doc = context.getWiki().getDocument( docName, context);
		} catch (XWikiException e) {
			throw new SpaceManagerException(e);
		}
	}
	
    /**
     * Creates a new document for this space
     * @param spaceTitle The display title of this space
     */
	protected void createNewSpaceDoc( String spaceTitle) {
        XWikiDocument spaceDoc = getDoc();
        String className = manager.getSpaceClassName();
		BaseObject spaceObj = new BaseObject();
		spaceObj.setName(doc.getFullName());
		spaceObj.setClassName(className);
		spaceDoc.addObject( className, spaceObj );
		
		className = "XWiki.XWikiPreferences";
		BaseObject xWikiPrefObj = new BaseObject();
		xWikiPrefObj.setName(spaceDoc.getFullName());
		xWikiPrefObj.setClassName( className );
		spaceDoc.addObject(className, xWikiPrefObj);
		
		spaceObj.setStringValue("displayTitle", spaceTitle);
	}

    /**
     * Sets the creator of the space
     * @param creator The creators reference (Eg: XWiki.SampleUser)
     */
    public void setCreator(String creator) {
        getDoc().setCreator(creator);
    }

    /**
     * Gets the display title of the space
     * @return
     */
    public String getDisplayTitle() {
        return doc.getStringValue(manager.getSpaceClassName(), SPACE_DISPLAYTITLE);
    }

    /**
     * Sets the display title of the space
     * @param title The new title
     */
    public void setDisplayTitle(String title) {
        getDoc().setStringValue(manager.getSpaceClassName(), SPACE_DISPLAYTITLE, title);
     }

    /**
     * Gets the type of the space
     * @return
     */
    public String getType() {
        return doc.getStringValue(manager.getSpaceClassName(), SPACE_TYPE);
	}

	/**
	 * Sets the type of the space
	 * @param type Space Type
	 */
    public void setType(String type) {
        getDoc().setStringValue(manager.getSpaceClassName(), SPACE_TYPE, type);
	}

	/**
	 * Gets the description of the space
	 * @return
	 */
    public String getDescription() {
        return doc.getStringValue(manager.getSpaceClassName(), SPACE_DESCRIPTION);
    }

    /**
     * Sets the description of the space
     * @param description The new description
     */
    public void setDescription(String description) {
        getDoc().setLargeStringValue(manager.getSpaceClassName(), SPACE_DESCRIPTION, description);
    }

    /**
     * Gets the membership policy of the space
     * @return
     */
    public String getPolicy() {
        return doc.getStringValue(manager.getSpaceClassName(), SPACE_POLICY);
	}

	/**
	 * Sets the policy of the space
	 * @param policy Space Type
	 */
    public void setPolicy(String policy) {
        getDoc().setStringValue(manager.getSpaceClassName(), SPACE_POLICY, policy);
	}

    /**
     * Gets a preference for the space
     * @param prefName The preference name
     * @return
     * @throws SpaceManagerException 
     */
    public String getPreference(String prefName) throws SpaceManagerException {
        try {
			return context.getWiki().getPrefsClass(context).getStringValue(prefName);
		} catch (XWikiException e) {
			throw new SpaceManagerException(e);
		}
    }

    /**
     * Returns the URL for the homepage of this space
     * @return
     */
    public String getHomeShortcutURL() {
    	return doc.getStringValue(manager.getSpaceClassName(), "urlshortcut");
    }

    /**
     * Sets the URL for the homepage of the space
     * @param homeShortCutURL new URL the homepage of this space
     */
    public void setHomeShortcutURL(String homeShortCutURL) {
    	getDoc().setStringValue(manager.getSpaceClassName(), "urlshortcut", homeShortCutURL);
    }

    /**
     * Returns the URL to the Wiki Homepage of the space
     * @return
     * @throws SpaceManagerException
     */
    public String getHomeURL() throws SpaceManagerException {
        try {
			return context.getWiki().getURL(getSpace()+".WebHome", "view", context);
		} catch (XWikiException e) {
			throw new SpaceManagerException(e);
		}
    }

    /**
     * Returns the list of fields in the Space class
     * @throws SpaceManagerException
     * @return
     */
    public List getFieldNames() throws SpaceManagerException {
    	try {
			return (List)manager.getSpaceClass(context).getFieldList();
		} catch (XWikiException e) {
			throw new SpaceManagerException(e);
		}
    }

    public void updateSpaceFromRequest() throws SpaceManagerException {
            XWikiDocument doc = getDoc();
        try {
            doc.updateObjectFromRequest(manager.getSpaceClassName(), context);
            doc.updateObjectFromRequest("XWiki.XWikiPreferences", context);       
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    public boolean validateSpaceData() throws SpaceManagerException {
        try {
            return doc.validate(context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    public void setCreationDate(Date date) {
        getDoc().setCreationDate(date);
    }
}
