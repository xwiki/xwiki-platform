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
package com.xpn.xwiki.internal.store;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.AttachmentRecycleBinContentStore;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiRecycleBinContentStoreInterface;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

/**
 * Various store related configuration.
 * 
 * @version $Id$
 * @since 10.3RC1
 */
@Component(roles = StoreConfiguration.class)
@Singleton
public class StoreConfiguration
{
    private static final String FILE = "file";

    @Inject
    private ComponentManager componentManager;

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    private Logger logger;

    private <S> S getStore(Class<S> storeClass, String property, String defaultHint) throws ComponentLookupException
    {
        String hint = this.configuration.getProperty(property, defaultHint);

        if (this.componentManager.hasComponent(storeClass, hint)) {
            return this.componentManager.getInstance(storeClass, hint);
        } else {
            this.logger.warn("Can't find any implementation for the configured store with role [{}] and hint [{}]",
                storeClass, defaultHint);
        }

        // Fallback on the default
        if (!defaultHint.equals(hint) && this.componentManager.hasComponent(storeClass, defaultHint)) {
            return this.componentManager.getInstance(storeClass, defaultHint);
        }

        // Fallback on hibernate
        if (!defaultHint.equals(XWikiHibernateBaseStore.HINT)
            && this.componentManager.hasComponent(storeClass, XWikiHibernateBaseStore.HINT)) {
            return this.componentManager.getInstance(storeClass, XWikiHibernateBaseStore.HINT);
        }

        return null;
    }

    /**
     * @return the configured instance of {@link XWikiStoreInterface}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public XWikiStoreInterface getXWikiStore() throws ComponentLookupException
    {
        return getStore(XWikiStoreInterface.class, "xwiki.store.main.hint", XWikiHibernateBaseStore.HINT);
    }

    /**
     * @return the configured instance of {@link XWikiVersioningStoreInterface}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public XWikiVersioningStoreInterface getXWikiVersioningStore() throws ComponentLookupException
    {
        return getStore(XWikiVersioningStoreInterface.class, "xwiki.store.versioning.hint",
            XWikiHibernateBaseStore.HINT);
    }

    /**
     * @return the configured instance of {@link XWikiRecycleBinStoreInterface}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public XWikiRecycleBinStoreInterface getXWikiRecycleBinStore() throws ComponentLookupException
    {
        return isRecycleBinEnabled()
            ? getStore(XWikiRecycleBinStoreInterface.class, "xwiki.store.recyclebin.hint", XWikiHibernateBaseStore.HINT)
            : null;
    }

    /**
     * @return the configured instance of {@link AttachmentRecycleBinStore}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public AttachmentRecycleBinStore getAttachmentRecycleBinStore() throws ComponentLookupException
    {
        return isAttachmentRecycleBinEnabled() ? getStore(AttachmentRecycleBinStore.class,
            "xwiki.store.attachment.recyclebin.hint", XWikiHibernateBaseStore.HINT) : null;
    }

    // Support "file"

    /**
     * @return the configured instance of {@link XWikiAttachmentStoreInterface}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public XWikiAttachmentStoreInterface getXWikiAttachmentStore() throws ComponentLookupException
    {
        return getStore(XWikiAttachmentStoreInterface.class, "xwiki.store.attachment.hint",
            XWikiHibernateBaseStore.HINT);
    }

    /**
     * @return the configured instance of {@link AttachmentVersioningStore}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public AttachmentVersioningStore getAttachmentVersioningStore() throws ComponentLookupException
    {
        return getStore(AttachmentVersioningStore.class, "xwiki.store.attachment.versioning.hint",
            isAttachmentVersioningEnabled() ? FILE : "void");
    }

    /**
     * @return the configured instance of {@link XWikiRecycleBinContentStoreInterface}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public XWikiRecycleBinContentStoreInterface getXWikiRecycleBinContentStore() throws ComponentLookupException
    {
        return getStore(XWikiRecycleBinContentStoreInterface.class, "xwiki.store.recyclebin.content.hint", FILE);
    }

    /**
     * @return the configured instance of {@link AttachmentRecycleBinContentStore}
     * @throws ComponentLookupException when failing to lookup the component
     */
    public AttachmentRecycleBinContentStore getAttachmentRecycleBinContentStore() throws ComponentLookupException
    {
        return getStore(AttachmentRecycleBinContentStore.class, "xwiki.store.attachment.recyclebin.content.hint", FILE);
    }

    /**
     * @return true if the versioning store is enabled
     */
    public boolean isVersioningEnabled()
    {
        return !"0".equals(this.configuration.getProperty("xwiki.store.versioning", "1"));
    }

    /**
     * @return true if the attachment versioning tore is enabled
     */
    public boolean isAttachmentVersioningEnabled()
    {
        return !"0".equals(this.configuration.getProperty("xwiki.store.attachment.versioning", "1"));
    }

    /**
     * @return true if the deleted documents are stored in a recycle bin
     */
    public boolean isRecycleBinEnabled()
    {
        return !"0".equals(this.configuration.getProperty("xwiki.recyclebin", "1"));
    }

    /**
     * @return true if the deleted attachments are stored in a recycle bin
     */
    public boolean isAttachmentRecycleBinEnabled()
    {
        return !"0".equals(this.configuration.getProperty("storage.attachment.recyclebin", "1"));
    }

    /**
     * @return true if cache store is enabled
     */
    public boolean isStoreCacheEnabled()
    {
        return !"0".equals(this.configuration.getProperty("xwiki.store.cache", "1"));
    }
}
