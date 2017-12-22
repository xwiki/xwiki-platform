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

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

/**
 * Script service allowing a user to create keys pairs and issue certificates.
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Named(CryptoScriptService.ROLEHINT + '.' + StoreScriptService.ROLEHINT)
@Singleton
public class StoreScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "store";

    /**
     * Used to get permanent directory.
     */
    @Inject
    private Environment environment;

    @Inject
    @Named("X509file")
    private KeyStore x509FileKeyStore;

    @Inject
    @Named("X509wiki")
    private KeyStore x509WikiKeyStore;

    @Inject
    @Named("X509wiki")
    private CertificateStore x509WikiCertificateStore;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    private StoreReference getFileStoreReference(String filename, boolean multi)
    {
        File file;

        if (!filename.startsWith("/")) {
            file = new File(environment.getPermanentDirectory(), filename);
        } else {
            file = new File(filename);
        }
        return new FileStoreReference(file, multi);
    }

    /**
     * Returns a X509 key store based on a folder of files. This store allows storage of multiple keys.
     *
     * @param filename the name of the folder. If it does not starts with "/", it will be located
     *                 in the permanent directory.
     * @return a multi-key keystore.
     */
    public ScriptingKeyStore getX509FileKeyStore(String filename)
    {
        return new ScriptingKeyStore(x509FileKeyStore, getFileStoreReference(filename, true),
            contextualAuthorizationManager);
    }

    /**
     * Returns a X509 key store based on a single file. This store allows storage of a single key.
     *
     * @param filename the name of the file. If it does not starts with "/", it will be located
     *                 in the permanent directory.
     * @return a single key store.
     */
    public ScriptingKeyStore getX509FileSingleKeyStore(String filename)
    {
        return new ScriptingKeyStore(x509FileKeyStore, getFileStoreReference(filename, false),
            contextualAuthorizationManager);
    }

    /**
     * Returns a X509 key store based on a wiki space. This store allows storage of multiple keys.
     *
     * @param reference the space reference.
     * @return a multi-key store.
     */
    public ScriptingKeyStore getX509SpaceKeyStore(SpaceReference reference)
    {
        return new ScriptingKeyStore(x509WikiKeyStore, new WikiStoreReference(reference),
            contextualAuthorizationManager);
    }

    /**
     * Returns a X509 key store based on a wiki document. This store allows storage of a single key.
     *
     * @param reference the document reference.
     * @return a single key store.
     */
    public ScriptingKeyStore getX509DocumentKeyStore(DocumentReference reference)
    {
        return new ScriptingKeyStore(x509WikiKeyStore, new WikiStoreReference(reference),
            contextualAuthorizationManager);
    }

    /**
     * Returns a X509 certificate store based on a wiki space. This store allows storage of multiple certificates.
     *
     * @param reference the space reference.
     * @return a multi-certificate store.
     */
    public ScriptingCertificateStore getX509SpaceCertificateStore(SpaceReference reference)
    {
        return new ScriptingCertificateStore(x509WikiCertificateStore, new WikiStoreReference(reference),
            contextualAuthorizationManager);
    }

    /**
     * Returns a X509 certificate store based on a wiki document. This store allows storage of a single certificate.
     *
     * @param reference the document reference.
     * @return a single key store.
     */
    public ScriptingCertificateStore getX509DocumentCertificateStore(DocumentReference reference)
    {
        return new ScriptingCertificateStore(x509WikiCertificateStore, new WikiStoreReference(reference),
            contextualAuthorizationManager);
    }
}
