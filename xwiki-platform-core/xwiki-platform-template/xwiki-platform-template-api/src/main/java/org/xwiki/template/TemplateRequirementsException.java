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
package org.xwiki.template;

import java.util.List;

import org.xwiki.logging.AbstractMessageException;

/**
 * Any exception raised in the XWiki Extension Manager must raise an exception of this type.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 */
public class TemplateRequirementsException extends AbstractMessageException
{
    /**
     * The translation key to use to translate this error.
     */
    public static final String TRANSLATION_KEY = "template.error.requirement";

    /**
     * The prefix to use in various template requirement errors.
     */
    public static final String TRANSLATION_KEY_PREFIX = TRANSLATION_KEY + '.';

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private final List<Throwable> causes;

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param template the name of the failing template
     * @param causes the causes (which is saved for later retrieval by the {@link #getCauses()} method)
     */
    public TemplateRequirementsException(String template, List<Throwable> causes)
    {
        super(TRANSLATION_KEY, "Template [{}] requirements are not met.", template, causes.get(0));

        this.causes = causes;
    }

    /**
     * @return the causes which leaded to this exception
     */
    public List<Throwable> getCauses()
    {
        return this.causes;
    }
}
