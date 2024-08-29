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
package org.xwiki.activeinstalls2.internal.data;

/**
 * Represents Java-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class JavaPing
{
    private String vendor;

    private String version;

    private String specificationVersion;

    /**
     * @return the Java Runtime Environment vendor
     */
    public String getVendor()
    {
        return this.vendor;
    }

    /**
     * @param vendor see {@link #getVendor()}
     */
    public void setVendor(String vendor)
    {
        this.vendor = vendor;
    }

    /**
     * @return the Java Runtime Environment version
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version see {@link #getVersion()}
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @return the Java Runtime Environment specification version
     */
    public String getSpecificationVersion()
    {
        return this.specificationVersion;
    }

    /**
     * @param specificationVersion see {@link #getSpecificationVersion()}
     */
    public void setSpecificationVersion(String specificationVersion)
    {
        this.specificationVersion = specificationVersion;
    }
}
