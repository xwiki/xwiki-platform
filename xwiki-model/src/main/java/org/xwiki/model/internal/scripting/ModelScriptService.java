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
package org.xwiki.model.internal.scripting;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Model-specific Scripting APIs.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component("model")
public class ModelScriptService implements ScriptService
{
    @Requirement
    private ComponentManager componentManager;

    /**
     * Create a Document Reference from a passed wiki, space and page names, which can be empty strings or null in
     * which case they are resolved against the Resolver having the hint passed as parameter. Valid hints are for
     * example "default", "current" "currentmixed".
     *
     * @param wiki the wiki reference name to use (can be empty or null)
     * @param space the space reference name to use (can be empty or null)
     * @param page the page referene name to use (can be empty or null)
     * @param hint the hint of the Resolver to use in case any parameter is empty or null
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     */
    public DocumentReference createDocumentReference(String wiki, String space, String page, String hint)
    {
        EntityReference reference = null;
        if (!StringUtils.isEmpty(wiki)) {
            reference = new EntityReference(wiki, EntityType.WIKI);
        }
        if (!StringUtils.isEmpty(space)) {
            reference = new EntityReference(space, EntityType.SPACE, reference);
        }
        if (!StringUtils.isEmpty(page)) {
            reference = new EntityReference(page, EntityType.DOCUMENT, reference);
        }

        DocumentReference documentReference;
        try {
            documentReference = this.componentManager.lookup(DocumentReferenceResolver.class, hint).resolve(reference);
        } catch (ComponentLookupException e) {
            documentReference = null;
        }
        return documentReference;
    }

    /**
     * @param stringRepresentation the document reference specified as a String (using the "wiki:space.page" format
     *        and with special characters escaped where required).
     * @param hint the hint of the Resolver to use in case any part of the reference is missing (no wiki specified,
     *        no space or no page)
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     */
    public DocumentReference resolveDocument(String stringRepresentation, String hint)
    {
        DocumentReference result;
        try {
            result = this.componentManager.lookup(DocumentReferenceResolver.class, hint).resolve(
                stringRepresentation);
        } catch (ComponentLookupException e) {
            result = null;
        }
        return result;
    }

    /**
     * @param reference the entity reference to transform into a String representation
     * @param hint the hint of the Serializer to use (valid hints are for example "default", "compact", "local")
     * @return the string representation of the passed entity reference
     */
    public String serialize(EntityReference reference, String hint)
    {
        String result;
        try {
            result = (String) this.componentManager.lookup(EntityReferenceSerializer.class, hint).serialize(reference);
        } catch (ComponentLookupException e) {
            result = null;
        }
        return result;
    }
}
