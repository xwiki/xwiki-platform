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

import org.xwiki.crypto.pkix.params.CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.stability.Unstable;

/**
 * X.509 subject certificate parameters.
 *
 * @version $Id$
 */
@Unstable
public class X509CertificateParameters implements CertificateParameters
{
    private X509Extensions extensions;

    /**
     * Create a new instance from given arguments.
     */
    public X509CertificateParameters()
    {
        this.extensions = null;
    }

    /**
     * Create a new instance from given arguments.
     *
     * @param extensions the subject specific v3 certificate extensions for a certificate, or null for none.
     */
    public X509CertificateParameters(X509Extensions extensions)
    {
        this.extensions = extensions;
    }

    /**
     * @return the subject specific v3 certificate extensions for a certificate, or null for none.
     */
    public X509Extensions getExtensions()
    {
        return extensions;
    }
}
