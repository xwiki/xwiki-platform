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
package org.xwiki.wiki.provisioning;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;

/**
 * Base class for {@link org.xwiki.job.Request} implementations used by wiki provisioners.
 *
 * @since 5.3M2
 * @version $Id$
 */
public class WikiProvisioningJobRequest extends AbstractRequest
{
    /**
     * Name of the property that stores the id of the wiki to provision.
     */
    public static final String PROPERTY_WIKI_ID = "wikiprovisioning.wikiId";

    /**
     * Name of the property that stores the property used by the provisioning job.
     */
    public static final String PROPERTY_PROVISIONING_JOB_PARAMETER = "wikiprovisioning.parameter";

    /**
     * Name of the property that stores the user that is provisioning the wiki.
     */
    public static final String PROPERTY_PROVISIONING_USER = "wikiprovisioning.user";

    /**
     * Constructor.
     * @param id id of the job request
     * @param wikiId id of the wiki to provision
     * @param parameter the parameter to be used by the provisioning job
     * @deprecated use {@link #WikiProvisioningJobRequest(List, String, Object, DocumentReference)}
     */
    @Deprecated
    public WikiProvisioningJobRequest(List<String> id, String wikiId, Object parameter)
    {
        setId(id);
        setWikiId(wikiId);
        setProvisioningJobParameter(parameter);
    }

    /**
     * Constructor.
     * @param id id of the job request
     * @param wikiId id of the wiki to provision
     * @param parameter the parameter to be used by the provisioning job
     * @param provisioningUser the user who executes the provisioning job
     * @since 6.3M1
     */
    public WikiProvisioningJobRequest(List<String> id, String wikiId, Object parameter,
        DocumentReference provisioningUser)
    {
        setId(id);
        setWikiId(wikiId);
        setProvisioningJobParameter(parameter);
        setProvisioningUser(provisioningUser);
    }

    /**
     * @param wikiId if of the wiki to provision
     */
    public void setWikiId(String wikiId)
    {
        setProperty(PROPERTY_WIKI_ID, wikiId);
    }

    /**
     * @return the id of the wiki to provision
     */
    public String getWikiId()
    {
        return getProperty(PROPERTY_WIKI_ID);
    }

    /**
     * @param parameter the parameter to be used by the provisioning job
     */
    public void setProvisioningJobParameter(Object parameter)
    {
        setProperty(PROPERTY_PROVISIONING_JOB_PARAMETER, parameter);
    }

    /**
     * @return the parameter to be used by the provisioning job
     */
    public Object getProvisioningJobParameter()
    {
        return getProperty(PROPERTY_PROVISIONING_JOB_PARAMETER);
    }

    /**
     * @param userReference the user who executes the provisioning job
     * @since 6.3M1
     */
    public void setProvisioningUser(DocumentReference userReference)
    {
        setProperty(PROPERTY_PROVISIONING_USER, userReference);
    }

    /**
     * @return the user who executes the provisioning job
     * @since 6.3M1
     */
    public DocumentReference getProvisioningUser()
    {
        return getProperty(PROPERTY_PROVISIONING_USER);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (!(o instanceof WikiProvisioningJobRequest)) {
            return false;
        }

        WikiProvisioningJobRequest r = (WikiProvisioningJobRequest) o;
        return new EqualsBuilder().append(r.getId(), getId()).append(r.getWikiId(), getWikiId()).append(
                r.getProvisioningJobParameter(), getProvisioningJobParameter()).isEquals();
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(getId());
        builder.append(getWikiId()).append(getProvisioningJobParameter());
        return builder.hashCode();
    }

}
