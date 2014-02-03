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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.stability.Unstable;

/**
 * Email address general name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509Rfc822Name implements X509StringGeneralName, BcGeneralName
{
    private final String str;
    private final InternetAddress addr;

    /**
     * Constructs a RFC 822 general name by parsing the given string.
     *
     * @param address the address compliant with RFC 822.
     */
    public X509Rfc822Name(String address)
    {
        String newStr = null;
        InternetAddress newAddr = null;

        try {
            newAddr = new InternetAddress(address);
            newStr = newAddr.getAddress();
        } catch (AddressException e) {
            newStr = address;
        }

        this.str = newStr;
        this.addr = newAddr;
    }

    /**
     * Constructs a RFC 822 general name from an internet address.
     *
     * @param address the address compliant with RFC 822.
     */
    public X509Rfc822Name(InternetAddress address)
    {
        this.addr = address;
        this.str = address.getAddress();
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509Rfc822Name(GeneralName name)
    {
        this(DERIA5String.getInstance(name.getName()).getString());

        if (name.getTagNo() != GeneralName.rfc822Name) {
            throw new IllegalArgumentException("Incompatible general name: " + name.getTagNo());
        }
    }

    /**
     * @return the internet address represented by the general name, or null if the address could not be parsed.
     */
    public InternetAddress getAddress()
    {
        return addr;
    }

    @Override
    public String getName()
    {
        return str;
    }

    @Override
    public GeneralName getGeneralName()
    {
        return new GeneralName(GeneralName.rfc822Name, str);
    }
}
