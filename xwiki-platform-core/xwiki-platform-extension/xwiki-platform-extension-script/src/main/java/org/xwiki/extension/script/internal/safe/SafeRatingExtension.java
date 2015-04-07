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
package org.xwiki.extension.script.internal.safe;

import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingRatingExtension;

/**
 * Provide a public script access to a rating extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 6.4M3
 */
public class SafeRatingExtension<T extends RatingExtension> extends WrappingRatingExtension<T> implements
    RatingExtension
{
    /**
     * The provider of instances safe for public scripts.
     */
    private ScriptSafeProvider<Object> safeProvider;

    /**
     * @param ratingExtension the wrapped rating extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeRatingExtension(T ratingExtension, ScriptSafeProvider<Object> safeProvider)
    {
        super(ratingExtension);

        this.safeProvider = safeProvider;
    }

    /**
     * @param <S> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the object
     */
    protected <S> S safe(Object unsafe)
    {
        return this.safeProvider.get(unsafe);
    }

    // Extension

    @Override
    public ExtensionFile getFile()
    {
        return safe(super.getFile());
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return safe(super.getRepository());
    }
}
