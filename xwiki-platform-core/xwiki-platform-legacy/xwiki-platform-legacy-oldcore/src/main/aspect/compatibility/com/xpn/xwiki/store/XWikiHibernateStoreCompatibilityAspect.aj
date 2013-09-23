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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.PropertyInterface;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.store.XWikiHibernateStore} class.
 * 
 * @version $Id$
 */
public privileged aspect XWikiHibernateStoreCompatibilityAspect
{
    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.saveXWikiObject(BaseObject object,
                                                    XWikiContext context,
                                                    boolean bTransaction) throws XWikiException
    {
        this.saveXWikiCollection(object, context, bTransaction);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.loadXWikiObject(BaseObject object,
                                                    XWikiContext context,
                                                    boolean bTransaction) throws XWikiException
    {
        this.loadXWikiCollectionInternal(object, null, context, bTransaction, false);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.loadXWikiCollection(BaseCollection object,
                                                        XWikiContext context,
                                                        boolean bTransaction,
                                                        boolean alreadyLoaded) throws XWikiException
    {
        this.loadXWikiCollectionInternal(object, null, context, bTransaction, alreadyLoaded);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.loadXWikiCollection(BaseCollection object1,
                                                        XWikiDocument doc,
                                                        XWikiContext context,
                                                        boolean bTransaction,
                                                        boolean alreadyLoaded) throws XWikiException
    {
        this.loadXWikiCollectionInternal(object1, doc, context, bTransaction, alreadyLoaded);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.deleteXWikiCollection(BaseCollection object,
                                                          XWikiContext context,
                                                          boolean bTransaction) throws XWikiException
    {
        this.deleteXWikiCollection(object, context, bTransaction, false);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.deleteXWikiObject(BaseObject baseObject,
                                                      XWikiContext context,
                                                      boolean bTransaction,
                                                      boolean bEvict)
        throws XWikiException
    {
        this.deleteXWikiCollection(baseObject, context, bTransaction, bEvict);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.deleteXWikiObject(BaseObject baseObject,
                                                      XWikiContext context,
                                                      boolean b) throws XWikiException
    {
        this.deleteXWikiCollection(baseObject, context, b, false);
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void XWikiHibernateStore.saveXWikiProperty(PropertyInterface property,
                                                      XWikiContext context,
                                                      boolean runInOwnTransaction)
        throws XWikiException
    {
        this.saveXWikiPropertyInternal(property, context, runInOwnTransaction);
    }
}
