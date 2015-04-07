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
package org.xwiki.gwt.wysiwyg.client.wiki;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A reference to a resource that can be linked. A resource represents the target of a link. Resources are usually
 * entity representations.
 * 
 * @version $Id$
 */
public class ResourceReference implements IsSerializable
{
    /**
     * Lists the type of resources that can be linked.
     */
    public static enum ResourceType
    {
        /**
         * Represents a Document.
         */
        DOCUMENT("doc"),

        /**
         * Represents an URL.
         */
        URL("url"),

        /**
         * Represents a document in another wiki.
         */
        INTERWIKI("interwiki"),

        /**
         * Represents a relative URL in the current wiki.
         */
        PATH("path"),

        /**
         * Represents a mail.
         */
        MAILTO("mailto"),

        /**
         * Represents an attachment.
         */
        ATTACHMENT("attach"),

        /**
         * Represents a Windows Explorer shared resource.
         */
        UNC("unc");

        /**
         * The mapping between scheme and resource types.
         */
        private static final Map<String, ResourceType> SCHEME_TO_RESOURCE_TYPE = new HashMap<String, ResourceType>();

        /**
         * The scheme corresponding to this resource type.
         */
        private final String scheme;

        static {
            for (ResourceType type : values()) {
                SCHEME_TO_RESOURCE_TYPE.put(type.getScheme(), type);
            }
        }

        /**
         * Creates a new resource type for the specified scheme.
         * 
         * @param scheme the scheme corresponding to the newly created resource type
         */
        ResourceType(String scheme)
        {
            this.scheme = scheme;
        }

        /**
         * @return the corresponding scheme
         */
        public String getScheme()
        {
            return scheme;
        }

        /**
         * @param scheme a scheme
         * @return the resource type associated with the given scheme
         */
        public static ResourceType forScheme(String scheme)
        {
            return SCHEME_TO_RESOURCE_TYPE.get(scheme);
        }
    }

    /**
     * The resource type.
     */
    private ResourceType type;

    /**
     * @see #isTyped()
     */
    private boolean typed = true;

    /**
     * Specifies the entity that provides the resource.
     */
    private EntityReference entityReference = new EntityReference();

    /**
     * Entity representation parameters.
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * @return the resource type
     */
    public ResourceType getType()
    {
        return type;
    }

    /**
     * Sets the resource type.
     * 
     * @param type the new resource type
     */
    public void setType(ResourceType type)
    {
        this.type = type;
    }

    /**
     * @return {@code true} if the resource type has been explicitly provided, i.e. if the scheme corresponding to the
     *         resource type was included in the resource reference
     */
    public boolean isTyped()
    {
        return typed;
    }

    /**
     * Sets whether the resource type has been explicitly provided or not.
     * 
     * @param typed {@code true} if the resource type has been explicitly provided, i.e. if the scheme corresponding to
     *            the resource type was included in the resource reference, {@code false} otherwise
     */
    public void setTyped(boolean typed)
    {
        this.typed = typed;
    }

    /**
     * @return the entity reference
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * Sets the entity reference.
     * 
     * @param entityReference the new entity reference
     */
    public void setEntityReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    /**
     * @return the resource parameters
     */
    public Map<String, String> getParameters()
    {
        return parameters;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#clone()
     */
    public ResourceReference clone()
    {
        ResourceReference resourceReference = new ResourceReference();
        resourceReference.setType(type);
        resourceReference.setTyped(typed);
        if (entityReference != null) {
            resourceReference.setEntityReference(entityReference.clone());
        }
        resourceReference.getParameters().putAll(parameters);
        return resourceReference;
    }
}
