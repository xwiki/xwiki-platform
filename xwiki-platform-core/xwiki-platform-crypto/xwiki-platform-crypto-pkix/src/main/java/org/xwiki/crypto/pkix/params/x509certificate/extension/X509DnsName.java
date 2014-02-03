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

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.stability.Unstable;

/**
 * DNS domain general name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509DnsName implements X509StringGeneralName, BcGeneralName
{
    private final String domain;

    /**
     * Constructs a DNS domain general name from the given string.
     *
     * @param domain the domain name compliant with RFC 1034.
     */
    public X509DnsName(String domain)
    {
        this.domain = domain;
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509DnsName(GeneralName name)
    {
        if (name.getTagNo() != GeneralName.dNSName) {
            throw new IllegalArgumentException("Incompatible general name: " + name.getTagNo());
        }

        this.domain = DERIA5String.getInstance(name.getName()).getString();
    }

    /**
     * @return the domain name represented by this general name.
     */
    public String getDomain()
    {
        return domain;
    }

    @Override
    public String getName()
    {
        return domain;
    }

    @Override
    public GeneralName getGeneralName()
    {
        return new GeneralName(GeneralName.dNSName, domain);
    }
}
