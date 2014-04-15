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

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Store and retrieve private key of entities.
 *
 * @version $Id$
 * @since 6.0
 */
@Role
@Unstable
public interface KeyStore
{
    /**
     * Store a private key and its certificate into a given entity.
     *
     * NOT VERY SECURE, since the key will be store as is without encryption.
     *
     * The key will be stored in the same document as the certificate. If a document reference is used, this
     * store could only contain one privateKey since no identifier are associated to private key. They are linked
     * to their certificate just by being in the same document as the certificate.
     *
     * @param store the entity where to store the key, could be a document reference or a space reference.
     * @param keyPair the key pair to be stored.
     * @throws KeyStoreException on error.
     */
    void store(EntityReference store, CertifiedKeyPair keyPair) throws KeyStoreException;

    /**
     * Store a private key and its certificate into a given entity.
     *
     * The private key will encrypted using the provided password.
     *
     * The key will be stored in the same document as the certificate. If a document reference is used, this
     * store could only contain one privateKey since no identifier are associated to private key. They are linked
     * to their certificate just by being in the same document as the certificate.
     *
     * @param store the entity where to store the key, could be a document reference or a space reference.
     * @param keyPair the key pair to be stored.
     * @param password the password to encrypt the private key.
     * @throws KeyStoreException on error.
     */
    void store(EntityReference store, CertifiedKeyPair keyPair, byte[] password) throws KeyStoreException;

    /**
     * Retrieve the private key for a given entity.
     *
     * @param entity the entity where the key is stored with its certificate, should be a document reference.
     * @return the signature corresponding to the entity, or null if none have been found.
     * @throws KeyStoreException on error.
     */
    CertifiedKeyPair retrieve(EntityReference entity) throws KeyStoreException;

    /**
     * Retrieve the certified key pair for a given entity.
     *
     * @param entity the entity where the key is stored with its certificate, should be a document reference.
     * @param password the password to decrypt the private key.
     * @return the signature corresponding to the entity, or null if none have been found.
     * @throws KeyStoreException on error.
     */
    CertifiedKeyPair retrieve(EntityReference entity, byte[] password) throws KeyStoreException;


    /**
     * Retrieve the certified key pair from a store for a given public key.
     *
     * @param store the store where the key has been stored with its certificate, should be a space reference.
     * @param publicKey for which the private key is requested.
     * @return the signature corresponding to the entity, or null if none have been found.
     * @throws KeyStoreException on error.
     */
    CertifiedKeyPair retrieve(EntityReference store, CertifiedPublicKey publicKey) throws KeyStoreException;

    /**
     * Retrieve the certified key pair from a store for a given public key.
     *
     * @param store the store where the key has been stored with its certificate, should be a space reference.
     * @param publicKey for which the private key is requested.
     * @param password the password to decrypt the private key.
     * @return the signature corresponding to the entity, or null if none have been found.
     * @throws KeyStoreException on error.
     */
    CertifiedKeyPair retrieve(EntityReference store, CertifiedPublicKey publicKey, byte[] password)
        throws KeyStoreException;
}
