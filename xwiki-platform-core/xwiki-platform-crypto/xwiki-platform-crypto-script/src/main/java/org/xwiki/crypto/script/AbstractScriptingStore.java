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
package org.xwiki.crypto.script;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Common base class for scripting stores.
 *
 * @version $Id$
 * @since 13.9RC1
 */
public abstract class AbstractScriptingStore
{
    /**
     * @deprecated use {@link StandardCharsets#UTF_8} instead
     */
    @Deprecated
    protected static final Charset UTF8 = Charset.forName("UTF-8");

    protected StoreReference storeReference;

    private ContextualAuthorizationManager contextualAuthorizationManager;

    protected AbstractScriptingStore(StoreReference reference,
        ContextualAuthorizationManager contextualAuthorizationManager)
    {
        this.storeReference = reference;
        this.contextualAuthorizationManager = contextualAuthorizationManager;
    }

    protected void checkAccess(Right right) throws AccessDeniedException
    {
        if (storeReference instanceof WikiStoreReference) {
            contextualAuthorizationManager.checkAccess(right, ((WikiStoreReference) storeReference).getReference());
        } else {
            contextualAuthorizationManager.checkAccess(Right.PROGRAM);
        }
    }
}
