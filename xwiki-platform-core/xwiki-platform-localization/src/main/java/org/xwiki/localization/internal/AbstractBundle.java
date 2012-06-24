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
package org.xwiki.localization.internal;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.WikiInformation;

/**
 * Base class for {@link Bundle} implementations. Defines the bundle priority as an <code>integer</code>.
 * 
 * @version $Id$
 */
public abstract class AbstractBundle extends AbstractLogEnabled implements Bundle
{
    /** Provides access to wiki localization information. */
    @Requirement
    protected WikiInformation wikiInfo;

    /**
     * @see #setPriority(int)
     * @see #compareTo(Bundle)
     * @see Bundle#getPriority()
     */
    private int priority;

    @Override
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * @param priority the Bundle priority, when searching for a translation. Lower is better.
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }
    
    @Override
    public String getTranslation(String key)
    {
        return getTranslation(key, this.wikiInfo.getContextLanguage());
    }

    @Override
    public void use(String bundleLocation)
    {
        // Do nothing, as by default bundles don't accept pulling.
    }

    /**
     * Compares two {@link Bundle}s according to their priority. If they have the same priority, use their class names
     * as the comparison criterion.
     * 
     * @param otherBundle The Bundle to compare to.
     * @return Zero if the two bundles are identical, a negative number if this Bundle takes precedence over the other,
     *         a positive number otherwise.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Bundle otherBundle)
    {
        if (getPriority() != otherBundle.getPriority()) {
            return getPriority() - otherBundle.getPriority();
        }
        return this.getClass().getSimpleName().compareTo(otherBundle.getClass().getSimpleName());
    }
}
