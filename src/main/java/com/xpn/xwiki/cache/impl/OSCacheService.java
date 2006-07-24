package com.xpn.xwiki.cache.impl;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheService;

/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * Allows initilization of OSCache in the proper way to use JGroups for
 * clustering. Basically this involves reading the oscache.properties file
 * located in WEB-INF/classes. This contains a listener setting for JGroups (and
 * other settings we could use if desired).
 * 

 * @author sdumitriu
 * @author markj
 * @author wr0ngway
 * 
 */
public class OSCacheService implements XWikiCacheService, Runnable
{
    private static final Log    log                  = LogFactory.getLog(OSCacheService.class);
    private static final String PROPS_FILENAME       = "oscache.properties";
    private static final String LOCAL_PROPS_FILENAME = "oscache-local.properties";
    private static final String PROPS_PATH       = "/WEB-INF/";

    private XWiki               xwiki;
    private Properties          cacheProperties;
    private Properties          localCacheProperties;
    private static int          cacheCount           = 0;
    // used by watcher thread to keep an eye on caches
    private List                cacheList            = new LinkedList();
    
    public OSCacheService()
    {
        super();
    }

    public void init(XWiki xwiki)
    {
        log.info("Initializing OSCacheService");
        this.xwiki = xwiki;
        cacheProperties = loadProps(PROPS_FILENAME);
        localCacheProperties = loadProps(LOCAL_PROPS_FILENAME);
        Thread watcherThread = new Thread(this, "OSCacheService Cache Monitor");
        watcherThread.start();
        log.info("Initialized OSCacheService");
    }
    
    public XWikiCache newLocalCache()
    {
        return new OSCacheCache(localCacheProperties);
    }

    public XWikiCache newLocalCache(int capacity)
    {
        return new OSCacheCache(localCacheProperties, capacity);
    }

    public XWikiCache newCache(Properties props)
    {
        OSCacheCache cc = new OSCacheCache(props);
        initCache(cc);
        return cc;
    }

    public XWikiCache newCache()
    {
        OSCacheCache cc = new OSCacheCache(cacheProperties);
        initCache(cc);
        return cc;
    }

    public XWikiCache newCache(int capacity)
    {
        OSCacheCache cc = new OSCacheCache(cacheProperties, capacity);
        initCache(cc);
        return cc;
    }

    private void initCache(OSCacheCache cc)
    {
        cacheCount++;
        cc.setName("Cache # " + cacheCount);

        if (log.isInfoEnabled())
        {
            DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            log.info("Created " + cc.getName() + " of size " + cc.getCapacity() + " at " + df.format(new Date())
                    + ", current count is " + cacheCount);
        }

        synchronized (cacheList)
        {
            cacheList.add(new WeakReference(cc));
        }
    }

    private Properties loadProps(String propertiesFilename)
    {
        log.info("Loading cache properties: " + propertiesFilename);
        Properties props = new Properties();
        InputStream is = null;

        try
        {
            File f = new File(propertiesFilename);
            if (f.exists())
                is = new FileInputStream(f);
            else
                is = xwiki.getResourceAsStream(PROPS_PATH + propertiesFilename);
            props.load(is);
            log.info("Properties loaded: " + propertiesFilename);
        }
        catch (Exception e)
        {
            log.error("Could not load cache properties " + propertiesFilename, e);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (Exception e)
            {
            }
        }
        return props;
    }

    /**
     * The cache watcher thread
     */
    public void run()
    {
        while (true)
        {
            if (log.isInfoEnabled())
            {
                synchronized (cacheList)
                {
                    Iterator i = cacheList.iterator();
                    while (i.hasNext())
                    {
                        WeakReference wr = (WeakReference) i.next();
                        if (wr != null)
                        {
                            OSCacheCache cc = (OSCacheCache) wr.get();
                            if (cc != null)
                            {
                                log.info("OSCacheCache item count for " + cc.getName() + " = "
                                        + cc.getNumberEntries() + " capacity is " + cc.getCapacity());
                            }
                        }
                    }
                }
            }
            try
            {
                Thread.sleep(1000 * 60 * 5); // 5 min
            }
            catch (InterruptedException e)
            {
            }

        }
    }
}