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

import java.util.Collection;

import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.script.internal.AbstractScriptingStore;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Wrapper over {@link CertificateStore} for scripting.
 *
 * @version $Id$
 * @since 8.4RC1
 */
public class ScriptingCertificateStore extends AbstractScriptingStore
{
    private CertificateStore store;

    ScriptingCertificateStore(CertificateStore store, StoreReference reference,
        ContextualAuthorizationManager contextualAuthorizationManager)
    {
        super(reference, contextualAuthorizationManager);
        this.store = store;
    }

    /**
     * Store a certificate into a certificate store.
     *
     * @param certificate the certificate to store.
     * @throws CertificateStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public void store(CertifiedPublicKey certificate) throws CertificateStoreException, AccessDeniedException
    {
        checkAccess(Right.EDIT);
        store.store(storeReference, certificate);
    }

    /**
     * Return a certificate provider providing from the given certificate store.
     *
     * @return a certificate provider.
     * @throws CertificateStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public CertificateProvider getCertificateProvider() throws CertificateStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.getCertificateProvider(storeReference);
    }

    /**
     * Return all the certificates available in a certificate store.
     *
     * @return a collection of certificates.
     * @throws CertificateStoreException on error.
     * @throws AccessDeniedException if you do not have edit access rights to the store.
     */
    public Collection<CertifiedPublicKey> getAllCertificates() throws CertificateStoreException, AccessDeniedException
    {
        checkAccess(Right.VIEW);
        return store.getAllCertificates(storeReference);
    }
}
