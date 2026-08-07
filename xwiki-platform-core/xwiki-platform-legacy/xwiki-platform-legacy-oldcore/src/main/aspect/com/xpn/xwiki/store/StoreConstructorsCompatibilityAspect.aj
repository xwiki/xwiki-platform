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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentRecycleBinStore;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentVersioningStore;

/**
 * Add a backward compatibility layer for the deprecated public constructors of the store classes.
 * <p>
 * All of these constructors are declared in a single aspect because they form one hierarchy: the subclass constructors
 * need the initialization performed by {@link XWikiHibernateBaseStore}'s, which an inter-type constructor cannot reach
 * through a {@code super(...)} call.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public privileged aspect StoreConstructorsCompatibilityAspect
{
    private static final String HIBERNATE_PATH_PROPERTY = "xwiki.store.hibernate.path";

    private static final String DEFAULT_HIBERNATE_PATH = "/WEB-INF/hibernate.cfg.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiHibernateBaseStore.class);

    /**
     * Set the Hibernate configuration path from the {@code xwiki.store.hibernate.path} configuration property.
     *
     * @param store the store to initialize
     * @param xwiki the XWiki object holding the configuration
     */
    private static void initializeHibernatePath(XWikiHibernateBaseStore store, XWiki xwiki)
    {
        String path = xwiki.Param(HIBERNATE_PATH_PROPERTY, DEFAULT_HIBERNATE_PATH);
        LOGGER.debug("Hibernate configuration file: [{}]", path);

        initializeHibernatePath(store, path);
    }

    /**
     * Set the Hibernate configuration path explicitly.
     *
     * @param store the store to initialize
     * @param hibpath the path to the Hibernate configuration file
     */
    private static void initializeHibernatePath(XWikiHibernateBaseStore store, String hibpath)
    {
        store.hibernateConfiguration = new HibernateConfiguration();

        store.setPath(hibpath);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param xwiki the XWiki object holding the configuration
     * @param context the current context, unused
     * @deprecated 1.6M1. Use the concrete store component instead, for example {@link XWikiStoreInterface} with
     *             hint {@code hibernate}.
     */
    @Deprecated
    public XWikiHibernateBaseStore.new(XWiki xwiki, XWikiContext context)
    {
        this();

        initializeHibernatePath(this, xwiki);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param hibpath the path to the Hibernate configuration file
     * @deprecated 1.6M1. Use the concrete store component instead, for example {@link XWikiStoreInterface} with
     *             hint {@code hibernate}.
     */
    @Deprecated
    public XWikiHibernateBaseStore.new(String hibpath)
    {
        this();

        initializeHibernatePath(this, hibpath);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param xwiki the XWiki object holding the configuration
     * @param context the current context, unused
     * @deprecated 1.6M1. Use the {@link XWikiStoreInterface} component with hint {@code hibernate} instead.
     */
    @Deprecated
    public XWikiHibernateStore.new(XWiki xwiki, XWikiContext context)
    {
        this();

        initializeHibernatePath(this, xwiki);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param hibpath the path to the Hibernate configuration file
     * @deprecated 1.6M1. Use the {@link XWikiStoreInterface} component with hint {@code hibernate} instead.
     */
    @Deprecated
    public XWikiHibernateStore.new(String hibpath)
    {
        this();

        initializeHibernatePath(this, hibpath);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link XWikiStoreInterface} component with hint {@code hibernate} instead.
     */
    @Deprecated
    public XWikiHibernateStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param xwiki the XWiki object holding the configuration
     * @param context the current context, unused
     * @deprecated 1.6M1. Use the {@link XWikiAttachmentStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore.new(XWiki xwiki, XWikiContext context)
    {
        this();

        initializeHibernatePath(this, xwiki);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link XWikiAttachmentStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param hibpath the path to the Hibernate configuration file
     * @deprecated 1.6M1. Use the {@link XWikiAttachmentStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore.new(String hibpath)
    {
        this();

        initializeHibernatePath(this, hibpath);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param xwiki the XWiki object holding the configuration
     * @param context the current context, unused
     * @deprecated 1.6M1. Use the {@link XWikiVersioningStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore.new(XWiki xwiki, XWikiContext context)
    {
        this();

        initializeHibernatePath(this, xwiki);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param hibpath the path to the Hibernate configuration file
     * @deprecated 1.6M1. Use the {@link XWikiVersioningStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore.new(String hibpath)
    {
        this();

        initializeHibernatePath(this, hibpath);
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link XWikiVersioningStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link XWikiRecycleBinStoreInterface} component with hint {@code hibernate}
     *             instead.
     */
    @Deprecated
    public XWikiHibernateRecycleBinStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link AttachmentRecycleBinStore} component with hint {@code hibernate} instead.
     */
    @Deprecated
    public HibernateAttachmentRecycleBinStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * The resulting store is not usable: this class obtains all its collaborators through component injection, and an
     * instance built with this constructor leaves every one of them {@code null}, so the first store operation fails.
     * Look up the component instead.
     *
     * @param context the current context, used to reach the XWiki object
     * @deprecated 1.6M1. Use the {@link AttachmentVersioningStore} component with hint {@code hibernate} instead.
     */
    @Deprecated
    public HibernateAttachmentVersioningStore.new(XWikiContext context)
    {
        this();

        initializeHibernatePath(this, context.getWiki());
    }

    /**
     * Build a store directly instead of looking it up as a component.
     * <p>
     * Unlike the other stores, this one holds no state and no injected collaborator, so the instance it returns behaves
     * exactly like one built with the no-argument constructor. The context is ignored.
     *
     * @param context the current context, unused
     * @deprecated 1.6M1. Use the {@link AttachmentVersioningStore} component with hint {@code void} instead.
     */
    @Deprecated
    public VoidAttachmentVersioningStore.new(XWikiContext context)
    {
        this();
    }
}
