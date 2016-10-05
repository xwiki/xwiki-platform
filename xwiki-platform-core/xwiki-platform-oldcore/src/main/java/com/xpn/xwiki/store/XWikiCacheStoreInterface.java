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
package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public interface XWikiCacheStoreInterface extends XWikiStoreInterface
{
    XWikiStoreInterface getStore();

    void setStore(XWikiStoreInterface store);

    void flushCache();

    /**
     * @deprecated since 8.3. It does not make much sense to make this method public and it was not really doing
     *             anything for a very long time in practice (since Infinispan is the default cache inmplementation)
     */
    @Deprecated
    void initCache(int capacity, int pageExistCapacity, XWikiContext context) throws XWikiException;
}
