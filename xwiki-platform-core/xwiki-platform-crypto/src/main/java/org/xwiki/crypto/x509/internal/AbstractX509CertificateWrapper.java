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
package org.xwiki.crypto.x509.internal;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

/**
 * Wrapper class for a X509 certificate.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractX509CertificateWrapper extends X509Certificate
{
    /** The actual certificate. */
    protected final X509Certificate certificate;

    /**
     * Create new {@link AbstractX509CertificateWrapper}.
     * 
     * @param certificate the certificate to wrap
     */
    public AbstractX509CertificateWrapper(X509Certificate certificate)
    {
        // If we are wrapping a wrapper, then unwrap and rewrap the internal cert.
        if (certificate instanceof AbstractX509CertificateWrapper) {
            this.certificate = ((AbstractX509CertificateWrapper) certificate).certificate;
        } else {
            this.certificate = certificate;
        }
    }

    @Override
    public int hashCode()
    {
        return this.certificate.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractX509CertificateWrapper) {
            return this.certificate.equals(((AbstractX509CertificateWrapper) obj).certificate);
        }
        return false;
    }

    @Override
    public boolean hasUnsupportedCriticalExtension()
    {
        return this.certificate.hasUnsupportedCriticalExtension();
    }

    @Override
    public Set<String> getCriticalExtensionOIDs()
    {
        return this.certificate.getCriticalExtensionOIDs();
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs()
    {
        return this.certificate.getNonCriticalExtensionOIDs();
    }

    @Override
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException
    {
        this.certificate.checkValidity();
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException
    {
        return this.certificate.getEncoded();
    }

    @Override
    public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
        NoSuchProviderException, SignatureException
    {
        this.certificate.verify(key);
    }

    @Override
    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException
    {
        this.certificate.checkValidity(date);
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException, SignatureException
    {
        this.certificate.verify(key, sigProvider);
    }

    @Override
    public byte[] getExtensionValue(String oid)
    {
        return this.certificate.getExtensionValue(oid);
    }

    @Override
    public int getVersion()
    {
        return this.certificate.getVersion();
    }

    @Override
    public BigInteger getSerialNumber()
    {
        return this.certificate.getSerialNumber();
    }

    @Override
    public PublicKey getPublicKey()
    {
        return this.certificate.getPublicKey();
    }

    @Override
    public Principal getIssuerDN()
    {
        return this.certificate.getIssuerDN();
    }

    @Override
    public X500Principal getIssuerX500Principal()
    {
        return this.certificate.getIssuerX500Principal();
    }

    @Override
    public Principal getSubjectDN()
    {
        return this.certificate.getSubjectDN();
    }

    @Override
    public X500Principal getSubjectX500Principal()
    {
        return this.certificate.getSubjectX500Principal();
    }

    @Override
    public Date getNotBefore()
    {
        return this.certificate.getNotBefore();
    }

    @Override
    public Date getNotAfter()
    {
        return this.certificate.getNotAfter();
    }

    @Override
    public byte[] getTBSCertificate() throws CertificateEncodingException
    {
        return this.certificate.getTBSCertificate();
    }

    @Override
    public byte[] getSignature()
    {
        return this.certificate.getSignature();
    }

    @Override
    public String getSigAlgName()
    {
        return this.certificate.getSigAlgName();
    }

    @Override
    public String getSigAlgOID()
    {
        return this.certificate.getSigAlgOID();
    }

    @Override
    public byte[] getSigAlgParams()
    {
        return this.certificate.getSigAlgParams();
    }

    @Override
    public boolean[] getIssuerUniqueID()
    {
        return this.certificate.getIssuerUniqueID();
    }

    @Override
    public boolean[] getSubjectUniqueID()
    {
        return this.certificate.getSubjectUniqueID();
    }

    @Override
    public boolean[] getKeyUsage()
    {
        return this.certificate.getKeyUsage();
    }

    @Override
    public List<String> getExtendedKeyUsage() throws CertificateParsingException
    {
        return this.certificate.getExtendedKeyUsage();
    }

    @Override
    public int getBasicConstraints()
    {
        return this.certificate.getBasicConstraints();
    }

    @Override
    public Collection<List< ? >> getSubjectAlternativeNames() throws CertificateParsingException
    {
        return this.certificate.getSubjectAlternativeNames();
    }

    @Override
    public Collection<List< ? >> getIssuerAlternativeNames() throws CertificateParsingException
    {
        return this.certificate.getIssuerAlternativeNames();
    }
}
