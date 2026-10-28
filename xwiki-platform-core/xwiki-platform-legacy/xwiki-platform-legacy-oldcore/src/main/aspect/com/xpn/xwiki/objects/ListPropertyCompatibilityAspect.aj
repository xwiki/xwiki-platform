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
package com.xpn.xwiki.objects;

import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.ListProperty} class.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public aspect ListPropertyCompatibilityAspect
{
    /**
     * @deprecated This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated(since = "7.0M2")
    private String ListProperty.formStringSeparator = ListClass.DEFAULT_SEPARATOR;

    /**
     * @deprecated This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated(since = "7.0M2")
    public String ListProperty.getFormStringSeparator()
    {
        return this.formStringSeparator;
    }

    /**
     * @deprecated This was never used, since it is not the right place to handle separators. They are
     *             defined in {@link ListClass} and that is where they are now handled through
     *             {@link ListClass#toFormString(BaseProperty)}.
     */
    @Deprecated(since = "7.0M2")
    public void ListProperty.setFormStringSeparator(String formStringSeparator)
    {
        this.formStringSeparator = formStringSeparator;
    }

    /**
     * @deprecated This method is here for a long time but it does not seem to have ever been used and it
     *             does not bring any value compared to the existing {@link #toFormString()} method.
     */
    @Deprecated(since = "7.0M2")
    public String ListProperty.toSingleFormString()
    {
        return super.toFormString();
    }
}
