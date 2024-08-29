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
package org.xwiki.model.reference;

import java.io.Serializable;

/**
 * A reference to a document version.
 * 
 * @version $Id$
 * @since 14.8RC1
 */
public class DocumentVersionReference extends DocumentReference
{
    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the parameter used to store the version.
     */
    private static final String VERSION = "version";

    /**
     * Creates a new reference to the specified document version.
     * 
     * @param documentReference the document reference
     * @param version the document version
     */
    public DocumentVersionReference(DocumentReference documentReference, Serializable version)
    {
        super(documentReference);

        setParameter(VERSION, version);
    }

    /**
     * Cast a {@link DocumentReference} to a {@link DocumentVersionReference}.
     * 
     * @param documentReference the document reference to cast
     */
    public DocumentVersionReference(DocumentReference documentReference)
    {
        super(documentReference);
    }

    /**
     * @return the document version
     */
    public Serializable getVersion()
    {
        return getParameter(VERSION);
    }

    /**
     * @return the {@link DocumentReference} without the version
     */
    public DocumentReference removeVersion()
    {
        return new DocumentReference(new DocumentVersionReference(this, null));
    }
}
