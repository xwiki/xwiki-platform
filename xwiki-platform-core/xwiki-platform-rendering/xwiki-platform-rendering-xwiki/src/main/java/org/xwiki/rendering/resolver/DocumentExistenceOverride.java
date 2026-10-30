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
package org.xwiki.rendering.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Overrides the existence of some documents when resolving resource references.
 * <p>
 * The entity a document or attachment resource reference targets depends on the existence of documents: for example
 * {@code Space.Page} targets the terminal document {@code Space.Page} when it exists, and the space home page
 * {@code Space.Page.WebHome} otherwise. Passing an instance of this class among the parameters of
 * {@code EntityReferenceResolver<ResourceReference>#resolve} resolves the reference as if the documents it lists
 * existed or not, whatever their actual state. This is needed for example to know which entity a reference was
 * targeting before a document got renamed.
 *
 * @version $Id$
 * @since 17.10.15
 * @since 18.4.8
 * @since 18.10.0RC1
 */
@Unstable
public final class DocumentExistenceOverride
{
    /**
     * Overrides the existence of no document: the actual state of all documents is used.
     *
     * @since 17.10.15
     * @since 18.4.8
     * @since 18.10.0RC1
     */
    @Unstable
    public static final DocumentExistenceOverride NONE = new DocumentExistenceOverride(Map.of());

    private final Map<DocumentReference, Boolean> existence = new HashMap<>();

    /**
     * @param existence the existence to assume for each document, whatever its actual state; the locale of the
     *     document references is ignored
     */
    public DocumentExistenceOverride(Map<DocumentReference, Boolean> existence)
    {
        existence.forEach((reference, exists) -> this.existence.put(reference.withoutLocale(), exists));
    }

    /**
     * @param documentReference the reference of the document
     * @return the existence to assume for the given document, or an empty optional if its actual state should be used
     */
    public Optional<Boolean> getExistence(DocumentReference documentReference)
    {
        return Optional.ofNullable(this.existence.get(documentReference.withoutLocale()));
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof DocumentExistenceOverride other && this.existence.equals(other.existence);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.existence);
    }

    @Override
    public String toString()
    {
        return this.existence.toString();
    }
}
