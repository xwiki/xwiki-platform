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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide safe Extension.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class ExtensionScriptSafeProvider implements ScriptSafeProvider<Extension>
{
    /**
     * The provider of instances safe for public scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider defaultSafeProvider;

    /**
     * Required by the {@link SafeInstalledExtension} to resolve the reference of the user that installed the extension.
     */
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public <S> S get(Extension unsafe)
    {
        Extension safe = switch (unsafe) {
            case CoreExtension coreExtension ->
                new SafeCoreExtension<CoreExtension>(coreExtension, this.defaultSafeProvider);
            case InstalledExtension installedExtension -> new SafeInstalledExtension<InstalledExtension>(
                installedExtension, this.defaultSafeProvider, this.documentReferenceResolver);
            case LocalExtension localExtension ->
                new SafeLocalExtension<LocalExtension>(localExtension, this.defaultSafeProvider);
            case IndexedExtension indexedExtension ->
                new SafeIndexedExtension<IndexedExtension>(indexedExtension, this.defaultSafeProvider);
            case RatingExtension ratingExtension ->
                new SafeRatingExtension<RatingExtension>(ratingExtension, this.defaultSafeProvider);
            case RemoteExtension remoteExtension ->
                new SafeRemoteExtension<RemoteExtension>(remoteExtension, this.defaultSafeProvider);
            case null, default -> new SafeExtension<Extension>(unsafe, this.defaultSafeProvider);
        };

        return (S) safe;
    }
}
