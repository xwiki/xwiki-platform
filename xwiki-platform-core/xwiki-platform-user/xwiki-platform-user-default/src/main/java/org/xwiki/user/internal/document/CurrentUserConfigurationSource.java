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
package org.xwiki.user.internal.document;

import java.util.function.Supplier;

import javax.inject.Provider;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.ConfigurationSourceDecorator;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Configuration source decorator that sets a passed user reference as the current user so that the wrapped
 * configuration source operates with that user set as the current user.
 *
 * @version $Id$
 * @since 12.4RC1
 */
public class CurrentUserConfigurationSource extends ConfigurationSourceDecorator
{
    private DocumentUserReference userReference;

    private Provider<XWikiContext> contextProvider;

    /**
     * @param userReference the user reference to set as the current use
     * @param internalConfigurationSource the wrapped configuration source
     * @param contextProvider the context provider used to set the current user
     */
    public CurrentUserConfigurationSource(DocumentUserReference userReference,
        ConfigurationSource internalConfigurationSource, Provider<XWikiContext> contextProvider)
    {
        super(internalConfigurationSource);
        this.userReference = userReference;
        this.contextProvider = contextProvider;
    }

    @Override
    protected <T> T executeRead(Supplier<T> supplier)
    {
        XWikiContext xcontext = this.contextProvider.get();
        DocumentReference originalUserReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.userReference.getReference());
            return supplier.get();
        } finally {
            xcontext.setUserReference(originalUserReference);
        }
    }

    @Override
    protected <E extends Exception> void executeWrite(ThrowingRunnable<E> runnable) throws E
    {
        XWikiContext xcontext = this.contextProvider.get();
        DocumentReference originalUserReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.userReference.getReference());
            runnable.run();
        } finally {
            xcontext.setUserReference(originalUserReference);
        }
    }
}
