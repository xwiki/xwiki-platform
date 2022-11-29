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
package org.xwiki.security.authentication;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;

/**
 * A dedicated {@link org.xwiki.resource.ResourceReference} to perform authentication related actions.
 * See {@link AuthenticationAction} for the possible actions.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public class AuthenticationResourceReference extends AbstractResourceReference
{
    /**
     * Identifier of the resource type also used for associated components.
     */
    public static final String RESOURCE_TYPE_ID = "authenticate";

    /**
     * Dedicated resource type used for authentication.
     */
    public static final ResourceType TYPE = new ResourceType(RESOURCE_TYPE_ID);

    private final WikiReference wikiReference;

    private final AuthenticationAction action;

    /**
     * Default constructor.
     *
     * @param wikiReference the reference of the wiki where the action should be performed.
     * @param action the action to perform.
     */
    public AuthenticationResourceReference(WikiReference wikiReference, AuthenticationAction action)
    {
        setType(TYPE);
        this.wikiReference = wikiReference;
        this.action = action;
    }

    /**
     * @return the action associated to this resource reference.
     */
    public AuthenticationAction getAction()
    {
        return action;
    }

    /**
     * @return the reference of the wiki where the action should be performed.
     * @since 14.6RC1
     * @since 14.4.3
     * @since 13.10.8
     */
    public WikiReference getWikiReference()
    {
        return wikiReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AuthenticationResourceReference that = (AuthenticationResourceReference) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(action, that.action)
            .append(wikiReference, that.wikiReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(action)
            .append(wikiReference)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .appendSuper(super.toString())
            .append("wikiReference", wikiReference)
            .append("action", action)
            .toString();
    }
}
