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
package org.xwiki.platform.wiki.creationjob;

import java.util.List;

import org.xwiki.extension.ExtensionId;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

/**
 * A wiki creation request containing all information about a wiki to create.
 *
 * @version $Id$
 * @since 7.0M2
 */
public class WikiCreationRequest extends AbstractRequest
{
    private static final long serialVersionUID = -1887940302223327347L;
    
    private static final String PROPERTY_PREFIX = "wikicreationrequest.";
    
    private static final String PROPERTY_WIKI_ID = PROPERTY_PREFIX + "wikiId";

    private static final String PROPERTY_PRETTY_NAME = PROPERTY_PREFIX + "prettyName";

    private static final String PROPERTY_ALIAS = PROPERTY_PREFIX + "alias";

    private static final String PROPERTY_DESCRIPTION = PROPERTY_PREFIX + "description";

    private static final String PROPERTY_IS_TEMPLATE = PROPERTY_PREFIX + "isTemplate";

    private static final String PROPERTY_TEMPLATE_ID = PROPERTY_PREFIX + "templateId";

    private static final String PROPERTY_EXTENSION_ID = PROPERTY_PREFIX + "extensionId";

    private static final String PROPERTY_OWNER_ID = PROPERTY_PREFIX + "ownerId";

    private static final String PROPERTY_USER_SCOPE = PROPERTY_PREFIX + "userScope";

    private static final String PROPERTY_MEMBERSHIP_TYPE = PROPERTY_PREFIX + "membershipType";

    private static final String PROPERTY_MEMBERS = PROPERTY_PREFIX + "members";

    private static final String PROPERTY_FAIL_ON_EXISTS = PROPERTY_PREFIX + "failOnExist";

    private static final String PROPERTY_WIKI_SOURCE = PROPERTY_PREFIX + "wikiSource";

    /**
     * Default constructor.
     */
    public WikiCreationRequest()
    {
    }

    /**
     * @param request the request to copy
     */
    public WikiCreationRequest(Request request)
    {
        super(request);
    }

    /**
     * @return id of the wiki to create
     */
    public String getWikiId()
    {
        return getProperty(PROPERTY_WIKI_ID);
    }

    /**
     * @param wikiId id of the wiki to create
     */
    public void setWikiId(String wikiId)
    {
        setProperty(PROPERTY_WIKI_ID, wikiId);
    }

    /**
     * @return pretty name of the wiki to create
     */
    public String getPrettyName()
    {
        return getProperty(PROPERTY_PRETTY_NAME);
    }

    /**
     * @param prettyName pretty name of the wiki to create
     */
    public void setPrettyName(String prettyName)
    {
        setProperty(PROPERTY_PRETTY_NAME, prettyName);
    }

    /**
     * @return default alias of the wiki to create
     */
    public String getAlias()
    {
        return getProperty(PROPERTY_ALIAS);
    }

    /**
     * @param alias default alias of the wiki to create
     */
    public void setAlias(String alias)
    {
        setProperty(PROPERTY_ALIAS, alias);
    }

    /**
     * @return the description field of the wiki to create
     */
    public String getDescription()
    {
        return getProperty(PROPERTY_DESCRIPTION);
    }

    /**
     * @param description the description field of the wiki to create
     */
    public void setDescription(String description)
    {
        setProperty(PROPERTY_DESCRIPTION, description);
    }

    /**
     * @return if the wiki to create will be a template or no
     */
    public boolean isTemplate()
    {
        return getProperty(PROPERTY_IS_TEMPLATE, false);
    }

    /**
     * @param isTemplate if the wiki to create will be a template or no
     */
    public void setTemplate(boolean isTemplate)
    {
        setProperty(PROPERTY_IS_TEMPLATE, isTemplate);
    }

    /**
     * @return id of the template used to fill the wiki to create
     */
    public String getTemplateId()
    {
        return getProperty(PROPERTY_TEMPLATE_ID);
    }

    /**
     * Set the id of the template of the wiki to create. Do not forget to use setWikiSource() too.
     * @param templateId id of the template used to fill the wiki to create
     */
    public void setTemplateId(String templateId)
    {
        setProperty(PROPERTY_TEMPLATE_ID, templateId);
    }

    /**
     * @return id of the main extension of the wiki to create
     */
    public ExtensionId getExtensionId()
    {
        return getProperty(PROPERTY_EXTENSION_ID);
    }

    /**
     * Set the id of the main extension of the wiki to create. Do not forget to use setWikiSource() too.
     * @param extensionId id of the main extension of the wiki to create
     */
    public void setExtensionId(ExtensionId extensionId)
    {
        setProperty(PROPERTY_EXTENSION_ID, extensionId);
    }

    /**
     * Set the id of the main extension of the wiki to create. Do not forget to use setWikiSource() too.
     * @param extensionId id of the main extension of the wiki to create
     * @param version version of the extension
     */
    public void setExtensionId(String extensionId, String version)
    { 
        setProperty(PROPERTY_EXTENSION_ID, new ExtensionId(extensionId, version));
    }

    /**
     * @return id of the owner of the wiki to create
     */
    public String getOwnerId()
    {
        return getProperty(PROPERTY_OWNER_ID);
    }

    /**
     * @param ownerId id of the owner of the wiki to create
     */
    public void setOwnerId(String ownerId)
    {
        setProperty(PROPERTY_OWNER_ID, ownerId);
    }

    /**
     * @return the user scope of the wiki to create
     */
    public UserScope getUserScope()
    {
        return getProperty(PROPERTY_USER_SCOPE);
    }

    /**
     * @param userScope the user scope of the wiki to create
     */
    public void setUserScope(UserScope userScope)
    {
        setProperty(PROPERTY_USER_SCOPE, userScope);
    }

    /**
     * @return the membership type of the wiki to create
     */
    public MembershipType getMembershipType()
    {
        return getProperty(PROPERTY_MEMBERSHIP_TYPE);
    }

    /**
     * @param membershipType the membership type of the wiki to create
     */
    public void setMembershipType(MembershipType membershipType)
    {
        setProperty(PROPERTY_MEMBERSHIP_TYPE, membershipType);
    }

    /**
     * @return the list of the members to add in the wiki to create
     */
    public List<String> getMembers()
    {
        return getProperty(PROPERTY_MEMBERS);
    }

    /**
     * @param members the list of the members to add in the wiki to create
     */
    public void setMembers(List<String> members)
    {
        setProperty(PROPERTY_MEMBERS, members);
    }

    /**
     * @return whether or not the wiki creation should fail if the database already exists
     */
    public boolean isFailOnExist()
    {
        return getProperty(PROPERTY_FAIL_ON_EXISTS, true);
    }

    /**
     * @param failOnExist whether or not the wiki creation should fail if the database already exists
     */
    public void setFailOnExist(boolean failOnExist)
    {
        setProperty(PROPERTY_FAIL_ON_EXISTS, failOnExist);
    }

    /**
     * @return source of the wiki to create
     */
    public WikiSource getWikiSource()
    {
        return getProperty(PROPERTY_WIKI_SOURCE);
    }

    /**
     * @param wikiSource source of the wiki to create
     */
    public void setWikiSource(WikiSource wikiSource)
    {
        setProperty(PROPERTY_WIKI_SOURCE, wikiSource);
    }
}

