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

import java.util.Locale;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;

/**
 * Base class for {@link TranslationBundle} implementations. Defines the bundle priority as an <code>integer</code>.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractTranslationBundle implements TranslationBundle
{
    /**
     * An empty bundle.
     */
    public static final TranslationBundle EMPTY = new AbstractTranslationBundle(null)
    {
        @Override
        public Translation getTranslation(String key, Locale locale)
        {
            return null;
        };
    };

    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getPriority()
     * @see #compareTo(TranslationBundle)
     */
    private int priority = DEFAULTPRIORITY;

    /**
     * Default constructor.
     */
    protected AbstractTranslationBundle()
    {

    }

    /**
     * @param id the identifier if the bundle
     */
    public AbstractTranslationBundle(String id)
    {
        this.id = id;
    }

    /**
     * @param id the identifier of the bundle
     * @param priority the priority of the bundle
     */
    public AbstractTranslationBundle(String id, int priority)
    {
        this.id = id;
        this.priority = priority;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier of the bundle
     * @see #getId()
     */
    protected void setId(String id)
    {
        this.id = id;
    }

    @Override
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * @param priority the priority
     * @see #getPriority()
     */
    protected void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * Compares two {@link TranslationBundle}s according to their priority. If they have the same priority, use their
     * class names as the comparison criterion.
     * 
     * @param otherBundle The Bundle to compare to.
     * @return Zero if the two bundles are identical, a negative number if this Bundle takes precedence over the other,
     *         a positive number otherwise.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TranslationBundle otherBundle)
    {
        if (getPriority() != otherBundle.getPriority()) {
            return getPriority() - otherBundle.getPriority();
        }

        int result;

        if (getId() == null) {
            result = otherBundle.getId() == null ? 0 : 1;
        } else {
            result = otherBundle.getId() == null ? -1 : getId().compareTo(otherBundle.getId());
        }

        return result;
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
