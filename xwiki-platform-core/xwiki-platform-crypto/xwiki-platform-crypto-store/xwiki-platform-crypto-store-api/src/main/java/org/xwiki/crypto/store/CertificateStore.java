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
package org.xwiki.crypto.store;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Store and retrieve certificates of entities and provide certificates for chain verification.
 *
 * @version $Id$
 * @since 6.0
 */
@Role
@Unstable
public interface CertificateStore
{
    /**
     * Store a certificate into a certificate store.
     *
     * @param store the reference of a document or a space where the certificate should be stored.
     * @param certificate the certificate to store.
     * @throws CertificateStoreException on error.
     */
    void store(EntityReference store, CertifiedPublicKey certificate) throws CertificateStoreException;

    /**
     * Return a certificate provider for certificate store.
     *
     * @param store the reference of a document or a space where the certificate are stored.
     * @return a certificate provider.
     * @throws CertificateStoreException on error.
     */
    CertificateProvider getCertificateProvider(EntityReference store) throws CertificateStoreException;

    /**
     * Return a all the certificates available in a certificate store.
     *
     * @param store the reference of a document or a space where the certificate are stored.
     * @return a collection of certificates.
     * @throws CertificateStoreException on error.
     */
    Collection<CertifiedPublicKey> getAllCertificates(EntityReference store) throws CertificateStoreException;
}
