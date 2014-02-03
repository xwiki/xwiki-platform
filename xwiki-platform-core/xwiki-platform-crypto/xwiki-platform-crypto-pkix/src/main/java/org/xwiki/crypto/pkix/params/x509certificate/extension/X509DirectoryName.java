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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.stability.Unstable;

/**
 * X.509 Directory name general name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509DirectoryName extends DistinguishedName implements X509StringGeneralName, BcGeneralName
{
    /**
     * Create a new directory name.
     *
     * @param name the DN name like in "CN=Common Name, O=Organisation"
     */
    public X509DirectoryName(Object name)
    {
        super(name);
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509DirectoryName(GeneralName name)
    {
        super(X500Name.getInstance(name.getName()));

        if (name.getTagNo() != GeneralName.directoryName) {
            throw new IllegalArgumentException("Incompatible general name: " + name.getTagNo());
        }
    }

    @Override
    public GeneralName getGeneralName()
    {
        return new GeneralName(getX500Name());
    }
}
