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
package org.xwiki.rest;

/**
 * <p> This class contains the constants defining the relation types that can be specified for links in the
 * representations. <p>
 *
 * @version $Id$
 */
public final class Relations
{
    /**
     * Relation for links pointing to the resource that returned the current representation.
     */
    public static final String SELF = "self";

    /**
     * Relation for links pointing to a WADL description.
     */
    public static final String WADL = "wadl";

    /**
     * Relation for links pointing to the list of available wikis.
     */
    public static final String WIKIS = "http://www.xwiki.org/rel/wikis";

    /**
     * Relation for links pointing to the list of spaces.
     */
    public static final String SPACES = "http://www.xwiki.org/rel/spaces";

    /**
     * Relation for links pointing to list of pages.
     */
    public static final String PAGES = "http://www.xwiki.org/rel/pages";

    /**
     * Relation for links pointing to a page translation.
     */
    public static final String TRANSLATION = "http://www.xwiki.org/rel/translation";

    /**
     * Relation for links pointing to a page.
     */
    public static final String PAGE = "http://www.xwiki.org/rel/page";

    /**
     * Relation for links pointing to space.
     */
    public static final String SPACE = "http://www.xwiki.org/rel/space";

    /**
     * Relation for links pointing to the parent resource.
     */
    public static final String PARENT = "http://www.xwiki.org/rel/parent";

    /**
     * Relation for links pointing to the space's home.
     */
    public static final String HOME = "http://www.xwiki.org/rel/home";

    /**
     * Relation for links pointing to attachment data.
     */
    public static final String ATTACHMENT_DATA = "http://www.xwiki.org/rel/attachmentData";

    /**
     * Relation for links pointing to attachment metadata.
     */
    public static final String ATTACHMENT_METADATA = "http://www.xwiki.org/rel/attachmentMetadata";

    /**
     * Relation for links pointing to the list of comments.
     */
    public static final String COMMENTS = "http://www.xwiki.org/rel/comments";

    /**
     * Relation for links pointing to the list of attachments for a page.
     */
    public static final String ATTACHMENTS = "http://www.xwiki.org/rel/attachments";

    /**
     * Relation for links pointing to the list of objects.
     */
    public static final String OBJECTS = "http://www.xwiki.org/rel/objects";

    /**
     * Relation for links pointing to an object.
     */
    public static final String OBJECT = "http://www.xwiki.org/rel/object";

    /**
     * Relation for links pointing to the list of classes.
     */
    public static final String CLASSES = "http://www.xwiki.org/rel/classes";

    /**
     * Relation for links pointing to the page history.
     */
    public static final String HISTORY = "http://www.xwiki.org/rel/history";

    /**
     * Relation for links pointing to the class associated with a resource.
     */
    public static final String CLASS = "http://www.xwiki.org/rel/class";

    /**
     * Relation for links pointing to an object's property.
     */
    public static final String PROPERTY = "http://www.xwiki.org/rel/property";

    /**
     * Relation for links pointing to the list of values of an object property.
     */
    public static final String PROPERTY_VALUES = "http://www.xwiki.org/rel/propertyValues";

    /**
     * Relation for links pointing to the list of object properties.
     */
    public static final String PROPERTIES = "http://www.xwiki.org/rel/properties";

    /**
     * Relation for links pointing to the modifications.
     */
    public static final String MODIFICATIONS = "http://www.xwiki.org/rel/modifications";

    /**
     * Relation for links pointing to the children of a page.
     */
    public static final String CHILDREN = "http://www.xwiki.org/rel/children";

    /**
     * Relation for links pointing to the list of tags for a resource.
     */
    public static final String TAGS = "http://www.xwiki.org/rel/tags";

    /**
     * Relation for links pointing to a specific tag for a resource.
     */
    public static final String TAG = "http://www.xwiki.org/rel/tag";

    /**
     * Relation for links pointing to the resource for retrieving search results.
     */
    public static final String SEARCH = "http://www.xwiki.org/rel/search";

    /**
     * Relation for links pointing to the resource for retrieving search results using advanced queries.
     */
    public static final String QUERY = "http://www.xwiki.org/rel/query";

    /**
     * Relation for links pointing to the list of available syntaxes.
     */
    public static final String SYNTAXES = "http://www.xwiki.org/rel/syntaxes";

    /**
     * Relation for links pointing to the REST client information.
     */
    public static final String CLIENT = "http://www.xwiki.org/rel/client";

    /**
     * Avoid instantiation.
     */
    private Relations()
    {
    }
}
