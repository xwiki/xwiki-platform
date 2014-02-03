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
package org.xwiki.crypto.pkix.params.x509certificate.extension;

import java.io.IOException;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.stability.Unstable;

/**
 * Generic holder for general name not supported by specific class.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509GenericName implements X509GeneralName, BcGeneralName
{
    private final GeneralName name;

    /**
     * Create a new instance from a encoded ASN.1 value.
     *
     * @param tag the tag value.
     * @param encoded the encoded ASN.1 value.
     * @throws IOException on encoding error.
     */
    public X509GenericName(int tag, byte[] encoded) throws IOException
    {
        this.name = new GeneralName(tag, ASN1Primitive.fromByteArray(encoded));
    }

    /**
     * Create a new instance from a encoded ASN.1 name.
     *
     * @param encoded the encoded ASN.1 value.
     * @throws IOException on encoding error.
     */
    public X509GenericName(byte[] encoded) throws IOException
    {
        this.name = GeneralName.getInstance(encoded);
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509GenericName(GeneralName name)
    {
        this.name = name;
    }

    @Override
    public GeneralName getGeneralName()
    {
        return name;
    }
}
