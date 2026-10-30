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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Records the documents whose existence is checked when resolving resource references.
 * <p>
 * The entity a document or attachment resource reference targets can depend on the existence of documents: for example
 * the untyped reference {@code Space.Page} targets the terminal document {@code Space.Page} when it exists, and the
 * space home page {@code Space.Page.WebHome} otherwise. Passing an instance of this class among the parameters of
 * {@code EntityReferenceResolver<ResourceReference>#resolve} collects the documents whose existence was checked, which
 * are the documents whose creation or deletion can change the result of the resolution.
 * <p>
 * This class is not thread safe: an instance is meant to be used by a single resolution process.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public final class DocumentExistenceRecorder
{
    private final Set<DocumentReference> documents = new LinkedHashSet<>();

    /**
     * @param documentReference the reference of a document whose existence was checked; its locale is ignored
     */
    public void add(DocumentReference documentReference)
    {
        this.documents.add(documentReference.withoutLocale());
    }

    /**
     * @return the references (without locale) of the documents whose existence was checked, in the order of the checks
     */
    public Set<DocumentReference> getDocuments()
    {
        return Collections.unmodifiableSet(this.documents);
    }
}
