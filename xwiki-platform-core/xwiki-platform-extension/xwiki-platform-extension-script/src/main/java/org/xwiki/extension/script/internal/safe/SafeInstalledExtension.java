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

import java.util.Collection;
import java.util.Date;

import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Provide a public script access to a installed extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeInstalledExtension<T extends InstalledExtension> extends SafeLocalExtension<T> implements
    InstalledExtension
{
    /**
     * Used to resolve the reference of the user that installed the extension.
     */
    private final DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * @param localExtension the wrapped local extension
     * @param safeProvider the provider of instances safe for public scripts
     * @param documentReferenceResolver used to resolve the reference of the user that installed the extension
     */
    public SafeInstalledExtension(T localExtension, ScriptSafeProvider<Object> safeProvider,
        DocumentReferenceResolver<String> documentReferenceResolver)
    {
        super(localExtension, safeProvider);

        this.documentReferenceResolver = documentReferenceResolver;
    }

    // InstalledExtension

    @Override
    public LocalExtension getLocalExtension()
    {
        return safe(getWrapped().getLocalExtension());
    }

    @Override
    public boolean isInstalled()
    {
        return getWrapped().isInstalled();
    }

    @Override
    public boolean isInstalled(String namespace)
    {
        return getWrapped().isInstalled(namespace);
    }

    @Override
    public boolean isValid(String namespace)
    {
        return getWrapped().isValid(namespace);
    }

    @Override
    public Collection<String> getNamespaces()
    {
        return safe(getWrapped().getNamespaces());
    }

    @Override
    public boolean isDependency()
    {
        return getWrapped().isDependency();
    }

    @Override
    public boolean isDependency(String namespace)
    {
        return getWrapped().isDependency(namespace);
    }

    @Override
    public Date getInstallDate(String namespace)
    {
        return getWrapped().getInstallDate(namespace);
    }

    @Override
    public Object getNamespaceProperty(String key, String namespace)
    {
        return safe(getWrapped().getNamespaceProperty(key, namespace));
    }

    /**
     * @param namespace the namespace to look for
     * @return the reference of the user that installed this extension on the given namespace
     * @since 7.0M2
     */
    public DocumentReference getUserReference(String namespace)
    {
        DocumentReference userReference = null;
        Object value = getNamespaceProperty("user.reference", namespace);
        if (value instanceof DocumentReference) {
            userReference = (DocumentReference) value;
        } else if (value instanceof String) {
            // The extension store doesn't know how to serialize a DocumentReference and so it saves the string value.
            userReference = this.documentReferenceResolver.resolve((String) value);
        }
        return userReference;
    }
}
