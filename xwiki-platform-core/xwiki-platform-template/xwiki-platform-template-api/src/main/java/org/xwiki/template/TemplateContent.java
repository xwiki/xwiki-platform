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
package org.xwiki.template;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * The content of a template.
 *
 * @version $Id$
 * @since 7.0M1
 */
public interface TemplateContent
{
    /**
     * The various contexts in which to make a template unique.
     * 
     * @version $Id$
     * @since 11.8RC1
     */
    enum UniqueContext
    {
        /**
         * Unique for the whole request.
         */
        REQUEST
    }

    /**
     * @return the source of the template
     */
    String getContent();

    /**
     * @return the syntax of the content
     */
    Syntax getSourceSyntax();

    /**
     * @return the syntax so set in the {@link org.xwiki.rendering.block.RawBlock}, note taken into account if a source
     *         syntax is provided
     */
    Syntax getRawSyntax();

    /**
     * Return custom property with the provided name and converted (if needed) to the passed type.
     * 
     * @param <T> the type of the value to return
     * @param name the name of the property
     * @param type the type of the property
     * @return the property value in the provided type or null if none could be found
     */
    <T> T getProperty(String name, Type type);

    /**
     * Return custom property with the provided name and converted (if needed) to the passed default value type. If the
     * property does not exist the default value is returned.
     * 
     * @param <T> the type of the value to return
     * @param name the name of the property
     * @param def the default value
     * @return the property value in the provided default value type or the provided default value if none could be
     *         found
     */
    <T> T getProperty(String name, T def);

    /**
     * @return the names of the custom properties associated with the template
     * @since 15.0RC1
     * @since 14.10.1
     */
    @Unstable
    default Collection<String> getPropertyNames()
    {
        return Collections.emptyList();
    }

    /**
     * @return the author of the template
     */
    DocumentReference getAuthorReference();

    /**
     * @return used to make the difference between null author and no author
     * @since 9.4RC1
     */
    default boolean isAuthorProvided()
    {
        return true;
    }

    /**
     * @return the reference of the document to use as secure document (generally the document containing the code to
     *         execute) or null if the template is not associated with any document (for example filesystem template)
     * @since 10.11RC1
     */
    default DocumentReference getDocumentReference()
    {
        return null;
    }

    /**
     * @return true if the result be reused several times
     * @since 11.8RC1
     */
    default boolean isCacheAllowed()
    {
        return false;
    }

    /**
     * @return true if the execution should be asynchronous when possible
     * @since 11.8RC1
     */
    default boolean isAsyncAllowed()
    {
        return false;
    }

    /**
     * @return the list of context entries to take remember for the execution
     * @since 11.8RC1
     */
    default Set<String> getContextEntries()
    {
        return Collections.emptySet();
    }

    /**
     * @return true if the template should be executed only once even if referenced several times
     * @since 11.8RC1
     */
    default UniqueContext getUnique()
    {
        return null;
    }
}
