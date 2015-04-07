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
package org.xwiki.crypto.store.wiki.internal.query;

/**
 * Reference to an Object containing an X509Certificate.
 *
 * The ObjectReference should have been used, but is not comfortable.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class CertificateObjectReference
{
    private final String documentName;

    private final int objectNumber;

    /**
     * Construct a reference based on document name and object number.
     *
     * @param documentName the name of the document containing the XObject.
     * @param objectNumber the XObject number.
     */
    public CertificateObjectReference(String documentName, int objectNumber)
    {
        this.documentName = documentName;
        this.objectNumber = objectNumber;
    }

    /**
     * @return the name of the document containing the XObject.
     */
    public String getDocumentName()
    {
        return this.documentName;
    }

    /**
     * @return he XObject number.
     */
    public int getObjectNumber()
    {
        return this.objectNumber;
    }
}
