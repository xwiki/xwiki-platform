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

import java.io.IOException;

import org.bouncycastle.asn1.x500.X500Name;
import org.xwiki.crypto.pkix.internal.BcPrincipalIdentifier;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.stability.Unstable;

/**
 * Represent a Principal distinguished name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class DistinguishedName implements PrincipalIndentifier, BcPrincipalIdentifier
{
    private final X500Name dn;

    /**
     * Create a new distinguished name.
     * @param name the DN name like in "CN=Common Name, O=Organisation"

     */
    public DistinguishedName(Object name) {
        if (name instanceof String) {
            this.dn = new X500Name((String) name);
        } else {
            this.dn = X500Name.getInstance(name);
        }
    }

    @Override
    public byte[] getEncoded() throws IOException
    {
        return dn.getEncoded();
    }

    @Override
    public String getName()
    {
        return dn.toString();
    }

    // BcPrincipalIdentifier internal interface

    @Override
    public X500Name getX500Name()
    {
        return dn;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrincipalIndentifier)) {
            return false;
        }

        X500Name name;
        if (o instanceof BcPrincipalIdentifier) {
            name = getX500Name();
        } else {
            name = new X500Name(((PrincipalIndentifier) o).getName());
        }

        return this.dn.equals(name);
    }

    @Override
    public int hashCode()
    {
        return dn.hashCode();
    }
}
