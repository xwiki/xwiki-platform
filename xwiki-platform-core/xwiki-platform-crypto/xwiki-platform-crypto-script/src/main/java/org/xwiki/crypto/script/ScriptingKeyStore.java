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
package org.xwiki.crypto.script;

import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.script.internal.AbstractScriptingStore;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Wrapper over {@link KeyStore} for scripting.
 *
 * @version $Id$
 * @since 8.4RC1
 */
public class ScriptingKeyStore extends AbstractScriptingStore
{
    private KeyStore store;

    ScriptingKeyStore(KeyStore store, StoreReference reference,
        ContextualAuthorizationManager contextualAuthorizationManager)
    {
        super(reference, contextualAuthorizationManager);
        this.store = store;
    }

    /**
     * Store a private key and its certificate into a given store.
     *
     * NOT VERY SECURE, since the key will be store AS IS without encryption.
     *
     * @param keyPair the key pair to be stored.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public void store(CertifiedKeyPair keyPair) throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.EDIT);
        store.store(storeReference, keyPair);
    }

    /**
     * Store a private key and its certificate into a given store, encrypting the key with a password.
     *
     * @param keyPair the key pair to be stored.
     * @param password the password to encrypt the private key.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public void store(CertifiedKeyPair keyPair, String password) throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.EDIT);
        store.store(storeReference, keyPair, password.getBytes(UTF8));
    }

    /**
     * Retrieve a private key from a given store that may contains only a single key.
     *
     * @return the certified key pair, or null if none have been found.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public CertifiedKeyPair retrieve() throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.retrieve(storeReference);
    }

    /**
     * Retrieve the certified key pair from a given store that may contains only a single key and decrypt it using
     * the given password.
     *
     * @param password the password to decrypt the private key.
     * @return the certified key pair, or null if none have been found.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public CertifiedKeyPair retrieve(String password) throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.retrieve(storeReference, password.getBytes(UTF8));
    }

    /**
     * Retrieve the certified key pair from a given store that match the given certificate.
     *
     * @param publicKey for which the private key is requested.
     * @return the certified key pair corresponding to the given certificate, or null if none have been found.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public CertifiedKeyPair retrieve(CertifiedPublicKey publicKey) throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.retrieve(storeReference, publicKey);
    }

    /**
     * Retrieve the certified key pair from a given store that match the given certificate and decrypt it using
     * the given password.
     *
     * @param publicKey for which the private key is requested.
     * @param password the password to decrypt the private key.
     * @return the certified key pair corresponding to the given certificate, or null if none have been found.
     * @throws KeyStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public CertifiedKeyPair retrieve(CertifiedPublicKey publicKey, String password)
        throws KeyStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.retrieve(storeReference, publicKey, password.getBytes(UTF8));
    }
}
