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
package org.xwiki.crypto.store.internal;

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.internal.query.X509CertificateQuery;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * X509 implementation of {@link org.xwiki.crypto.store.CertificateStore}.
 *
 * @version $Id$
 * @since 6.0
 */
@Component
@Named("X509")
@Singleton
public class X509CertificateStore extends AbstractX509Store implements CertificateStore
{
    /**
     * Space where the certificate class is stored.
     */
    public static final String CERTIFICATECLASS_SPACE = "Crypto";

    /**
     * Document name of the certificate class.
     */
    public static final String CERTIFICATECLASS_NAME = "CertificateClass";

    /**
     * Full name of the certificate class.
     */
    public static final String CERTIFICATECLASS_FULLNAME = CERTIFICATECLASS_SPACE + "." + CERTIFICATECLASS_NAME;

    /**
     * Reference to the signature class.
     */
    public static final LocalDocumentReference CERTIFICATECLASS =
        new LocalDocumentReference(CERTIFICATECLASS_SPACE, CERTIFICATECLASS_NAME);

    /**
     * Name of the issuer field.
     */
    public static final String CERTIFICATECLASS_PROP_ISSUER = "issuer";

    /**
     * Name of the serial field.
     */
    public static final String CERTIFICATECLASS_PROP_SERIAL = "serial";

    /**
     * Name of the subject field.
     */
    public static final String CERTIFICATECLASS_PROP_SUBJECT = "subject";

    /**
     * Name of the key identifier field.
     */
    public static final String CERTIFICATECLASS_PROP_KEYID = "keyid";

    /**
     * Name of the certificate field.
     */
    public static final String CERTIFICATECLASS_PROP_CERTIFICATE = "certificate";

    @Override
    public void store(EntityReference store, CertifiedPublicKey certificate) throws CertificateStoreException
    {
        XWikiContext context = getXWikiContext();

        try {
            context.getWiki().saveDocument(storeCertificate(store, certificate, context), context);
        } catch (XWikiException e) {
            throw new CertificateStoreException("Error while saving certificate to store [" + store + "]", e);
        }
    }

    @Override
    public CertificateProvider getCertificateProvider(EntityReference store) throws CertificateStoreException
    {
        return new X509CertificateProvider(resolveStore(store), getCertificateFactory(), getEncoder(),
            getQueryManager());
    }

    @Override
    public Collection<CertifiedPublicKey> getAllCertificates(EntityReference store) throws CertificateStoreException
    {
        return new X509CertificateQuery(resolveStore(store), getCertificateFactory(), getEncoder(), getQueryManager())
            .getCertificates();
    }
}

