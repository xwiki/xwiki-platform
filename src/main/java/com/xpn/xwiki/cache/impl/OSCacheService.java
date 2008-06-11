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
 *
 */

package com.xpn.xwiki.cache.impl;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Allows initilization of OSCache in the proper way to use JGroups for
 * clustering. Basically this involves reading the oscache.properties file
 * located in WEB-INF/classes. This contains a listener setting for JGroups (and
 * other settings we could use if desired).
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
        // The JVM should be allowed to shutdown while this thread is running
        watcherThread.setDaemon(true);
        watcherThread.start();
        log.info("Initialized OSCacheService");
    }
    
    public XWikiCache newLocalCache() throws XWikiException
    {
        return new OSCacheCache(localCacheProperties);
    }

    public XWikiCache newLocalCache(int capacity) throws XWikiException
    {
        return new OSCacheCache(localCacheProperties, capacity);
    }

    public XWikiCache newCache(String cacheName, Properties props) throws XWikiException
    {
        OSCacheCache cc = new OSCacheCache(mergeProperties(props, cacheProperties));
        cc.setName(cacheName);
        initCache(cc);
        return cc;
    }

    public XWikiCache newLocalCache(Properties props) throws XWikiException
    {
        OSCacheCache cc = new OSCacheCache(mergeProperties(props, localCacheProperties));
        initCache(cc);
        return cc;
    }

    public XWikiCache newCache(String cacheName, Properties props, int capacity) throws XWikiException {
        OSCacheCache cc = new OSCacheCache(mergeProperties(props, cacheProperties), capacity);
        cc.setName(cacheName);
        initCache(cc);
        return cc;
    }

    public XWikiCache newLocalCache(Properties props, int capacity) throws XWikiException {
        OSCacheCache cc = new OSCacheCache(mergeProperties(props, localCacheProperties), capacity);
        initCache(cc);
        return cc;
    }

    private Properties mergeProperties(Properties props, Properties sourceProps) {
        Properties targetProps = new Properties();

        Enumeration en = sourceProps.keys();
                while (en.hasMoreElements()) {
                    Object key = en.nextElement();
                    Object value = sourceProps.get(key);
                    targetProps.put(key, value);
                }
        en = props.keys();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            Object value = props.get(key);
            targetProps.put(key, value);
        }
        return targetProps;
    }


    public XWikiCache newCache(String cacheName) throws XWikiException {
        OSCacheCache cc = new OSCacheCache(cacheProperties);
        cc.setName(cacheName);
        initCache(cc);
        return cc;
    }

    public XWikiCache newCache(String cacheName, int capacity) throws XWikiException {
        OSCacheCache cc = new OSCacheCache(cacheProperties, capacity);
        cc.setName(cacheName);
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
            log.warn("Could not load cache properties " + propertiesFilename + ": " + e.getMessage());
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