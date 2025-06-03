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
package org.xwiki.filter.xar.internal;

import org.xwiki.xar.internal.model.XarDocumentModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
public final class XARFilterUtils
{
    /**
     * @version $Id$
     * @since 6.2M1
     */
    public static class EventParameter
    {
        public String name;

        public Class<?> type;

        public EventParameter(String name, Class<?> type)
        {
            this.name = name;
            this.type = type;
        }

        public EventParameter(String name)
        {
            this(name, String.class);
        }
    }

    private static final String ROLEHINT_PREFIX = "xwiki+xar/";

    /**
     * @since 7.2M1
     */
    public static final String ROLEHINT_11 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_11;

    /**
     * @since 7.2M1
     */
    public static final String ROLEHINT_12 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_12;

    /**
     * @since 9.0RC1
     */
    public static final String ROLEHINT_13 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_13;

    /**
     * @since 12.0RC1
     */
    public static final String ROLEHINT_14 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_14;

    /**
     * @since 14.0RC1
     */
    public static final String ROLEHINT_15 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_15;

    /**
     * @since 16.10.0RC1
     */
    public static final String ROLEHINT_16 = ROLEHINT_PREFIX + XarDocumentModel.VERSION_16;

    /**
     * @since 7.2M1
     */
    public static final String ROLEHINT_CURRENT = ROLEHINT_16;

    /**
     * @since 6.2M1
     * @deprecated 7.2M1
     */
    @Deprecated
    public static final String ROLEHINT = ROLEHINT_11;
}
