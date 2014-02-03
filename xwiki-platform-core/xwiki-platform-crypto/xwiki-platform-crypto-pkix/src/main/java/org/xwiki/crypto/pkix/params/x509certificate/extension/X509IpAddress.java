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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.stability.Unstable;

/**
 * IP address general name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509IpAddress implements X509StringGeneralName, BcGeneralName
{
    private final byte[] ipAddress;

    /**
     * Construct a IP address general name from an ip address.
     *
     * @param ipAddress the ip address.
     */
    public X509IpAddress(String ipAddress)
    {
        GeneralName name = new GeneralName(GeneralName.iPAddress, ipAddress);
        this.ipAddress = DEROctetString.getInstance(name.getName()).getOctets();
    }

    /**
     * Construct a IP address general name from an ip address.
     *
     * @param ipAddress the ip address.
     */
    public X509IpAddress(InetAddress ipAddress)
    {
        this.ipAddress = ipAddress.getAddress();
    }

    /**
     * Construct a IP address general name from an ip address.
     *
     * @param ipAddress the ip address.
     * @param ipMask the ip mask.
     */
    public X509IpAddress(InetAddress ipAddress, InetAddress ipMask)
    {
        byte[] ip = ipAddress.getAddress();
        byte[] mask = ipMask.getAddress();

        if (ip.length != mask.length) {
            throw new IllegalArgumentException("Incompatible ip address (" + ip.length
                + ") and ip mask (" + mask.length + ")");
        }
        this.ipAddress = new byte[ip.length + mask.length];
        System.arraycopy(ip, 0, this.ipAddress, 0, ip.length);
        System.arraycopy(mask, 0, this.ipAddress, ip.length, mask.length);
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509IpAddress(GeneralName name)
    {
        if (name.getTagNo() != GeneralName.iPAddress) {
            throw new IllegalArgumentException("Incompatible general name: " + name.getTagNo());
        }
        this.ipAddress = DEROctetString.getInstance(name.getName()).getOctets();
    }

    /**
     * @return the ip net address represented by this general name.
     * @throws UnknownHostException if the ip address is malformed.
     */
    public InetAddress getIpAddress() throws UnknownHostException
    {
        byte[] ip = ipAddress;

        if (ip.length == 8 || ip.length == 32) {
            ip = new byte[ip.length / 2];
            System.arraycopy(ipAddress, 0, ip, 0, ip.length);
        }

        return InetAddress.getByAddress(ip);
    }

    /**
     * @return the ip net mask represented by this general name, or null if no mask was given.
     * @throws UnknownHostException if the ip mask is malformed.
     */
    public InetAddress getIpMask() throws UnknownHostException
    {
        if (ipAddress.length != 8 && ipAddress.length != 32) {
            return null;
        }

        byte[] mask = new byte[ipAddress.length / 2];
        System.arraycopy(ipAddress, mask.length, mask, 0, mask.length);

        return InetAddress.getByAddress(mask);
    }

    @Override
    public String getName()
    {
        try {
            if (ipAddress.length != 8 && ipAddress.length != 32) {
                return getIpAddress().getHostAddress();
            }
            return getIpAddress().getHostAddress() + "/" + getIpMask().getHostAddress();
        } catch (UnknownHostException e) {
            return Arrays.toString(ipAddress);
        }
    }

    @Override
    public GeneralName getGeneralName()
    {
        return new GeneralName(GeneralName.iPAddress, new DEROctetString(ipAddress));
    }
}
