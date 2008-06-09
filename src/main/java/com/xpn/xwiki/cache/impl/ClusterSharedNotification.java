package com.xpn.xwiki.cache.impl;

import com.opensymphony.oscache.plugins.clustersupport.ClusterNotification;

import java.io.Serializable;

/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */

/**
 * A notification message that holds information about a cache event. This
 * class is <code>Serializable</code> to allow it to be sent across the
 * network to other machines running in a cluster.
 *
 * @author <a href="&#109;a&#105;&#108;&#116;&#111;:chris&#64;swebtec.&#99;&#111;&#109;">Chris Miller</a>
 * @author $Author: dres $
 * @version $Revision: 1.1 $
 */
public class ClusterSharedNotification extends ClusterNotification {

    protected String cacheName;

    /**
     * Creates a new notification message object to broadcast to other
     * listening nodes in the cluster.
     *
     * @param type       The type of notification message. Valid types are
     *                   {@link #FLUSH_KEY} and {@link #FLUSH_GROUP}.
     * @param data       Specifies the object key or group name to flush.
     */
    public ClusterSharedNotification(String cacheName, int type, Serializable data) {
        super(type, data);
        this.cacheName = cacheName;
    }


    public String getCacheName() {
        return cacheName;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("cacheName=").append(cacheName).append(super.toString());
        return buf.toString();
    }
}
