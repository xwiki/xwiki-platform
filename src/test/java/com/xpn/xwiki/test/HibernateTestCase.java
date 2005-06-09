/**
 * ===================================================================
 *
 * Copyright (c) 2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 */
package com.xpn.xwiki.test;

import org.apache.velocity.app.Velocity;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;

import junit.framework.TestCase;

public class HibernateTestCase extends TestCase
{
    private XWiki xwiki;
    private XWikiConfig config;
    private XWikiContext context;

    protected void setUp() throws Exception
    {
        this.config = new XWikiConfig();

        // TODO: Should probably be modified to use a memory store for testing or a mock store
        // TODO: StoreHibernateTest should be refactored with this class in mind 
        this.config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
        this.config.put("xwiki.store.hibernate.path", getClass().getResource(StoreHibernateTest.HIB_LOCATION).getFile());

        this.context = new XWikiContext();
        this.context.setDatabase("xwikitest");

        this.xwiki = new XWiki(this.config, this.context);
        this.xwiki.setDatabase("xwikitest");

        this.context.setWiki(this.xwiki);
        
        StoreHibernateTest.cleanUp(this.xwiki.getHibernateStore(), this.context);
        this.xwiki.flushCache();

        Velocity.init("velocity.properties");
    }

    protected void tearDown()
    {
        this.xwiki.getHibernateStore().shutdownHibernate(this.context);
        this.xwiki = null;
        this.context = null;
        this.config = null;
        System.gc();
    }
    
    public XWikiContext getXWikiContext()
    {
        return this.context;
    }
    
    public XWikiConfig getXWikiConfig()
    {
        return this.config;
    }
    
    public XWiki getXWiki()
    {
        return this.xwiki;
    }
}
