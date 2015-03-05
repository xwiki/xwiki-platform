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
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

/**
 * A wiki creation request containing all information about a wiki to create.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Unstable
public class WikiCreationRequest extends AbstractRequest
{
    private static final long serialVersionUID = -1887940302223327347L;

    private String wikiId;

    private String prettyName;

    private String alias;

    private String description;

    private boolean isTemplate;

    private String templateId;

    private ExtensionId extensionId;

    private String ownerId;

    private UserScope userScope;

    private MembershipType membershipType;

    private List<String> members;

    private boolean failOnExist;

    private WikiSource wikiSource;

    /**
     * @return id of the wiki to create
     */
    public String getWikiId()
    {
        return wikiId;
    }

    /**
     * @param wikiId id of the wiki to create
     */
    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }

    /**
     * @return pretty name of the wiki to create
     */
    public String getPrettyName()
    {
        return prettyName;
    }

    /**
     * @param prettyName pretty name of the wiki to create
     */
    public void setPrettyName(String prettyName)
    {
        this.prettyName = prettyName;
    }

    /**
     * @return default alias of the wiki to create
     */
    public String getAlias()
    {
        return alias;
    }

    /**
     * @param alias default alias of the wiki to create
     */
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    /**
     * @return the description field of the wiki to create
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description field of the wiki to create
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return if the wiki to create will be a template or no
     */
    public boolean isTemplate()
    {
        return isTemplate;
    }

    /**
     * @param isTemplate if the wiki to create will be a template or no
     */
    public void setTemplate(boolean isTemplate)
    {
        this.isTemplate = isTemplate;
    }

    /**
     * @return id of the template used to fill the wiki to create
     */
    public String getTemplateId()
    {
        return templateId;
    }

    /**
     * Set the id of the template of the wiki to create. Do not forget to use setWikiSource() too.
     * @param templateId id of the template used to fill the wiki to create
     */
    public void setTemplateId(String templateId)
    {
        this.templateId = templateId;
    }

    /**
     * @return id of the main extension of the wiki to create
     */
    public ExtensionId getExtensionId()
    {
        return extensionId;
    }

    /**
     * Set the id of the main extension of the wiki to create. Do not forget to use setWikiSource() too.
     * @param extensionId id of the main extension of the wiki to create
     */
    public void setExtensionId(ExtensionId extensionId)
    {
        this.extensionId = extensionId;
    }

    /**
     * Set the id of the main extension of the wiki to create. Do not forget to use setWikiSource() too.
     * @param extensionId id of the main extension of the wiki to create
     * @param version version of the extension
     */
    public void setExtensionId(String extensionId, String version)
    {
        this.extensionId = new ExtensionId(extensionId, version);
    }

    /**
     * @return id of the owner of the wiki to create
     */
    public String getOwnerId()
    {
        return ownerId;
    }

    /**
     * @param ownerId id of the owner of the wiki to create
     */
    public void setOwnerId(String ownerId)
    {
        this.ownerId = ownerId;
    }

    /**
     * @return the user scope of the wiki to create
     */
    public UserScope getUserScope()
    {
        return userScope;
    }

    /**
     * @param userScope the user scope of the wiki to create
     */
    public void setUserScope(UserScope userScope)
    {
        this.userScope = userScope;
    }

    /**
     * @return the membership type of the wiki to create
     */
    public MembershipType getMembershipType()
    {
        return membershipType;
    }

    /**
     * @param membershipType the membership type of the wiki to create
     */
    public void setMembershipType(MembershipType membershipType)
    {
        this.membershipType = membershipType;
    }

    /**
     * @return the list of the members to add in the wiki to create
     */
    public List<String> getMembers()
    {
        return members;
    }

    /**
     * @param members the list of the members to add in the wiki to create
     */
    public void setMembers(List<String> members)
    {
        this.members = members;
    }

    /**
     * @return whether or not the wiki creation should fail if the database already exists
     */
    public boolean isFailOnExist()
    {
        return failOnExist;
    }

    /**
     * @param failOnExist whether or not the wiki creation should fail if the database already exists
     */
    public void setFailOnExist(boolean failOnExist)
    {
        this.failOnExist = failOnExist;
    }

    /**
     * @return source of the wiki to create
     */
    public WikiSource getWikiSource()
    {
        return wikiSource;
    }

    /**
     * @param wikiSource source of the wiki to create
     */
    public void setWikiSource(WikiSource wikiSource)
    {
        this.wikiSource = wikiSource;
    }
}
