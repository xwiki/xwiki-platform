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
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.internal.safe.ScriptSafeProvider;

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
        Extension safe;

        if (unsafe instanceof CoreExtension) {
            safe = new SafeCoreExtension<CoreExtension>((CoreExtension) unsafe, this.defaultSafeProvider);
        } else if (unsafe instanceof InstalledExtension) {
            safe = new SafeInstalledExtension<InstalledExtension>((InstalledExtension) unsafe, this.defaultSafeProvider,
                this.documentReferenceResolver);
        } else if (unsafe instanceof LocalExtension) {
            safe = new SafeLocalExtension<LocalExtension>((LocalExtension) unsafe, this.defaultSafeProvider);
        } else if (unsafe instanceof RatingExtension) {
            safe = new SafeRatingExtension<RatingExtension>((RatingExtension) unsafe, this.defaultSafeProvider);
        } else if (unsafe instanceof RemoteExtension) {
            safe = new SafeRemoteExtension<RemoteExtension>((RemoteExtension) unsafe, this.defaultSafeProvider);
        } else {
            safe = new SafeExtension<Extension>(unsafe, this.defaultSafeProvider);
        }

        return (S) safe;
    }
}
