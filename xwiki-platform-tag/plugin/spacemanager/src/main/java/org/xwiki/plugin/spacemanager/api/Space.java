/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.plugin.spacemanager.api;

import java.util.Date;
import java.util.List;

import com.xpn.xwiki.XWikiException;

/**
 * Defines a Wiki Space interface
 * 
 * @version $Id: $
 */
public interface Space
{

    /**
     * @return true if the space already exists, false otherwise
     */
    public boolean isNew();

    /**
     * @return true if the space has been marked as deleted, false otherwise
     */
    public boolean isDeleted();

    /**
     * @return the space wikiname
     */
    public String getSpaceName();

    /**
     * @return the type of the space
     */
    public String getType();

    /**
     * Sets the type of the space
     * 
     * @param type the new type
     */
    public void setType(String type);

    /**
     * @return the policy of the space
     */
    public String getPolicy();

    /**
     * Sets the policy of the space
     * 
     * @param policy the new policy
     */
    public void setPolicy(String policy);

    /**
     * @return the name of the creator of this space
     */
    public String getCreator();

    /**
     * Set the name of the creator of this space This should only be used to overide the creator
     */
    public void setCreator(String creator);

    /**
     * @return the nice title of the space
     */
    public String getDisplayTitle();

    /**
     * Set the display title of the space
     * 
     * @param title the new title for the space
     */
    public void setDisplayTitle(String title);

    /**
     * @return the description of the space
     */
    public String getDescription();

    /**
     * Set the description title of a space
     * 
     * @param the new description for the space
     */
    public void setDescription(String description);

    /**
     * Get a preference of a space
     * 
     * @param prefName the name of the preference to retrieve
     * @return the value for asked the preference
     * @throws SpaceManagerException
     */
    public String getPreference(String prefName) throws SpaceManagerException;

    /**
     * Get the Home shortcut URL. The shortcut URL is manual and is not handled by the XWiki server
     * but by the frontal server
     * 
     * @return the home shortcut URL as stored in the space
     */
    public String getHomeShortcutURL();

    /**
     * Set the home shortcut URL
     * 
     * @param homeShortCutURL the new shortcut URL for the space
     */
    public void setHomeShortcutURL(String homeShortCutURL);

    /**
     * @return the Home page URL for the space
     * @throws SpaceManagerException
     */
    public String getHomeURL() throws SpaceManagerException;

    /**
     * @return the list of editable fields for this space
     * @throws SpaceManagerException
     */
    public List getFieldNames() throws SpaceManagerException;

    /**
     * Display a space field in view or edit mode
     * 
     * @param fieldName the field to display
     * @param mode the mode to display the field in
     * @return the HTML content to display
     */
    public String display(String fieldName, String mode);

    /**
     * Save the modified space
     * 
     * @throws XWikiException
     */
    public void save() throws XWikiException;

    /**
     * Save the modified space with programming rights
     * 
     * @throws XWikiException
     */
    public void saveWithProgrammingRights() throws XWikiException;

    /**
     * Update the space data from HTTP request parameters
     */
    public void updateSpaceFromRequest() throws SpaceManagerException;

    /**
     * Validate space data
     * 
     * @return true if the space data is valid, false otherwise
     * @throws SpaceManagerException
     */
    public boolean validateSpaceData() throws SpaceManagerException;

    /**
     * Set the creation date of the space
     * 
     * @param date the new date of creation for the space
     */
    public void setCreationDate(Date date);
}
