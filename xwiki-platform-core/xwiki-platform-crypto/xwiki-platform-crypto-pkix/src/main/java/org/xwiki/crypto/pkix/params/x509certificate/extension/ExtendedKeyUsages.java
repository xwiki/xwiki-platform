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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.asn1.x509.Extension;
import org.xwiki.stability.Unstable;

/**
 * Extended Key Usage.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class ExtendedKeyUsages
{
    /**
     * OID of ExtendedKeyUsage.
     */
    public static final String OID = Extension.extendedKeyUsage.getId();

    /**
     * { 2 5 29 37 0 }.
     */
    public static final String ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";

    /**
     * Server authentication { 1 3 6 1 5 5 7 3 1 }.
     */
    public static final String SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
    /**
     * Client authentication { 1 3 6 1 5 5 7 3 2 }.
     */
    public static final String CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";
    /**
     * Code signing { 1 3 6 1 5 5 7 3 3 }.
     */
    public static final String CODE_SIGNING = "1.3.6.1.5.5.7.3.3";
    /**
     * Email protection { 1 3 6 1 5 5 7 3 4 }.
     */
    public static final String EMAIL_PROTECTION = "1.3.6.1.5.5.7.3.4";
    /**
     * Timestamping { 1 3 6 1 5 5 7 3 8 }.
     */
    public static final String TIME_STAMPING = "1.3.6.1.5.5.7.3.8";
    /**
     * OCSP Signing { 1 3 6 1 5 5 7 3 9 }.
     */
    public static final String OCSP_SIGNING = "1.3.6.1.5.5.7.3.9";

    private Set<String> usages = new HashSet<String>();

    /**
     * Constructor from string array.
     *
     * @param usages array of usage OID to add.
     */
    public ExtendedKeyUsages(String[] usages)
    {
        Collections.addAll(this.usages, usages);
    }

    /**
     * Constructor from string array.
     *
     * @param usages list of usage OID to add.
     */
    public ExtendedKeyUsages(Collection<String> usages)
    {
        this.usages.addAll(usages);
    }

    /**
     * Check if a given usage is authorized.
     *
     * @param usage the usage oid to check.
     * @return true if the usage is authorized.
     */
    public boolean hasUsage(String usage)
    {
        return usages.contains(usage);
    }

    /**
     * @return all extended usage oid.
     */
    public Set<String> getAll()
    {
        return Collections.unmodifiableSet(usages);
    }

    /**
     * @return true if no extended usage has been added.
     */
    public boolean isEmpty()
    {
        return usages.isEmpty();
    }
}
