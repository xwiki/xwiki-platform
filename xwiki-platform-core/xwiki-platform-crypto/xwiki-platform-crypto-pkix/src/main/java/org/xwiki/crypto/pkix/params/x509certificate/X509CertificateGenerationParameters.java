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
package org.xwiki.crypto.pkix.params.x509certificate;

import org.xwiki.crypto.pkix.params.CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.stability.Unstable;

/**
 * X.509 common certificate parameters.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509CertificateGenerationParameters implements CertificateGenerationParameters
{
    /**
     * X.509 version.
     */
    public static enum Version {
        /**
         * Request a version 1 X.509 certificate.
         */
        V1,

        /**
         * Request a version 2 X.509 certificate (actually unsupported).
         */
        V2,

        /**
         * Request a version 3 X.509 certificate.
         */
        V3
    }

    private static final int DEFAULT_VALIDITY = 500;

    private final Version version;
    private final int validity;
    private final X509Extensions extensions;

    /**
     * Create a new instance with the default parameters.
     *
     * The default certificate version will be V1.
     * The default validity will be 500 days.
     */
    public X509CertificateGenerationParameters()
    {
        this(DEFAULT_VALIDITY);
    }

    /**
     * Create a new instance with the given arguments.
     *
     * The default certificate version will be V3.
     * The default validity will be 500 days.
     *
     * @param extensions the common v3 certificate extensions for all certificate issued by a generator, or null for
     *                   none.
     */
    public X509CertificateGenerationParameters(X509Extensions extensions)
    {
        this(DEFAULT_VALIDITY, extensions);
    }

    /**
     * Create a new instance with the given arguments.
     *
     * The default certificate version will be V1.
     *
     * @param validity the validity period in days from the time of issuance.
     */
    public X509CertificateGenerationParameters(int validity)
    {
        this(Version.V1, validity, null);
    }

    /**
     * Create a new instance with the given arguments.
     *
     * The default certificate version will be V3.
     *
     * @param validity the validity period in days from the time of issuance.
     * @param extensions the common v3 certificate extensions for all certificate issued by a generator, or null for
     *                   none.
     */
    public X509CertificateGenerationParameters(int validity, X509Extensions extensions)
    {
        this(Version.V3, validity, extensions);
    }

    /**
     * Create a new instance with the given arguments.
     *
     * @param version the X.509 version of certificate to create.
     * @param validity the validity period in days from the time of issuance.
     * @param extensions the common v3 certificate extensions for all certificate issued by a generator, or null for
     *                   none.
     */
    public X509CertificateGenerationParameters(Version version, int validity, X509Extensions extensions)
    {
        this.version = version;
        this.validity = validity;
        this.extensions = extensions;
    }

    /**
     * @return the X.509 version of the certificate to generate.
     */
    public Version getX509Version()
    {
        return version;
    }

    /**
     * @return the validity period in days from the time of issuance.
     */
    public int getValidity()
    {
        return validity;
    }

    /**
     * @return the common v3 certificate extensions for all certificate issued by a generator, or null for none.
     */
    public X509Extensions getExtensions()
    {
        return extensions;
    }
}
