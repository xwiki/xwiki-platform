package com.xpn.xwiki.cache.impl;

import com.opensymphony.oscache.base.*;
import com.opensymphony.oscache.base.events.*;
import com.opensymphony.oscache.plugins.clustersupport.ClusterNotification;
import com.opensymphony.oscache.plugins.clustersupport.JavaGroupsBroadcastingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.blocks.NotificationBus;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A concrete implementation of the {@link com.opensymphony.oscache.plugins.clustersupport.AbstractBroadcastingListener} based on
 * the JavaGroups library. This Class uses JavaGroups to broadcast cache flush
 * messages across a cluster.</p>
 *
 * <p>This variation of the JavaGroupsBroadcastingListerner uses a static NotificationBus which
 * is shared accross all instances of OSCache using this listerner. In order to work the cache entry keys have to
 * be unique accross all caches. 
 *
 * <p>One of the following properties should be configured in <code>oscache.properties</code> for
 * this listener:
 * <ul>
 * <li><b>cache.cluster.multicast.ip</b> - The multicast IP that JavaGroups should use for broadcasting</li>
 * <li><b>cache.cluster.properties</b> - The JavaGroups channel properties to use. Allows for precise
 * control over the behaviour of JavaGroups</li>
 * </ul>
 * Please refer to the clustering documentation for further details on the configuration of this listener.</p>
 *
 * @author <a href="&#109;a&#105;&#108;&#116;&#111;:chris&#64;swebtec.&#99;&#111;&#109;">Chris Miller</a>
 * @author Ludovic Dubost ludovic@xwiki.com
 */
public class SharedJavaGroupsBroadcastingListener implements NotificationBus.Consumer, CacheEntryEventListener, LifecycleAware {
    private final static Log log = LogFactory.getLog(JavaGroupsBroadcastingListener.class);
    private static final String BUS_NAME = "OSCacheBus";
    private static final String CHANNEL_PROPERTIES = "cache.cluster.properties";
    private static final String MULTICAST_IP_PROPERTY = "cache.cluster.multicast.ip";

    /**
     * The name to use for the origin of cluster events. Using this ensures
     * events are not fired recursively back over the cluster.
     */
    protected static final String CLUSTER_ORIGIN = "CLUSTER";
    protected Map cacheMap = new HashMap();

    /**
     * The first half of the default channel properties. They default channel properties are:
     * <pre>
     * UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
     * mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
     * PING(timeout=2000;num_initial_members=3):\
     * MERGE2(min_interval=5000;max_interval=10000):\
     * FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
     * pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
     * UNICAST(timeout=300,600,1200,2400):\
     * pbcast.STABLE(desired_avg_gossip=20000):\
     * FRAG(frag_size=8096;down_thread=false;up_thread=false):\
     * pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
     * </pre>
     *
     * Where <code>*.*.*.*</code> is the specified multicast IP, which defaults to <code>231.12.21.132</code>.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    /**
     * The second half of the default channel properties. They default channel properties are:
     * <pre>
     * UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
     * mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
     * PING(timeout=2000;num_initial_members=3):\
     * MERGE2(min_interval=5000;max_interval=10000):\
     * FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
     * pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
     * UNICAST(timeout=300,600,1200,2400):\
     * pbcast.STABLE(desired_avg_gossip=20000):\
     * FRAG(frag_size=8096;down_thread=false;up_thread=false):\
     * pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
     * </pre>
     *
     * Where <code>*.*.*.*</code> is the specified multicast IP, which defaults to <code>231.12.21.132</code>.
     */
    private static final String DEFAULT_CHANNEL_PROPERTIES_POST = ";mcast_port=45566;ip_ttl=32;mcast_send_buf_size=150000;mcast_recv_buf_size=80000):" + "PING(timeout=2000;num_initial_members=3):MERGE2(min_interval=5000;max_interval=10000):FD_SOCK:VERIFY_SUSPECT(timeout=1500):" + "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):UNICAST(timeout=300,600,1200,2400):pbcast.STABLE(desired_avg_gossip=20000):" + "FRAG(frag_size=8096;down_thread=false;up_thread=false):pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)";
    private static final String DEFAULT_MULTICAST_IP = "231.12.21.132";
    private static NotificationBus bus = null;
    private static int busCount = 0;

    /**
     * Initializes the broadcasting listener by starting up a JavaGroups notification
     * bus instance to handle incoming and outgoing messages.
     *
     * @param config An OSCache configuration object.
     * @throws com.opensymphony.oscache.base.InitializationException If this listener has
     * already been initialized.
     */
    public synchronized void initialize(Cache cache, Config config) throws InitializationException {
        String name = cache.getName();
        if (cacheMap.get(name)!=null) {
            if (log.isErrorEnabled())
                log.error("A cache with name " + name + " has already been registered");
        }
        cacheMap.put(name, cache);

        String properties = config.getProperty(CHANNEL_PROPERTIES);
        String multicastIP = config.getProperty(MULTICAST_IP_PROPERTY);

        if ((properties == null) && (multicastIP == null)) {
            multicastIP = DEFAULT_MULTICAST_IP;
        }

        if (properties == null) {
            properties = DEFAULT_CHANNEL_PROPERTIES_PRE + multicastIP.trim() + DEFAULT_CHANNEL_PROPERTIES_POST;
        } else {
            properties = properties.trim();
        }

        if (log.isInfoEnabled()) {
            log.info("Starting a new JavaGroups broadcasting listener with properties=" + properties);
        }

        try {
            if (bus==null) {
                bus = new NotificationBus(BUS_NAME, properties);
                bus.start();
                bus.getChannel().setOpt(Channel.LOCAL, new Boolean(false));
                bus.setConsumer(this);
            }
            busCount++;
            log.info("JavaGroups clustering support started successfully");
        } catch (Exception e) {
            throw new InitializationException("Initialization failed: " + e);
        }
    }


    public Cache getCache(String cacheName) {
        return (Cache) cacheMap.get(cacheName);
    }

    public void cacheEntryAdded(CacheEntryEvent cacheEntryEvent) {
    }

    public void cacheEntryRemoved(CacheEntryEvent cacheEntryEvent) {
    }

    public void cacheEntryUpdated(CacheEntryEvent cacheEntryEvent) {
    }

    /**
     * Event fired when an entry is flushed from the cache. This broadcasts
     * the flush message to any listening nodes on the network.
     */
    public void cacheEntryFlushed(CacheEntryEvent event) {
        if (!Cache.NESTED_EVENT.equals(event.getOrigin()) && !CLUSTER_ORIGIN.equals(event.getOrigin())) {
            if (log.isDebugEnabled()) {
                log.debug("cacheEntryFlushed called (" + event + ")");
            }

            sendNotification(new ClusterSharedNotification(event.getMap().getName(), ClusterNotification.FLUSH_KEY, event.getKey()));
        }
    }

    /**
     * Event fired when an entry is removed from the cache. This broadcasts
     * the remove method to any listening nodes on the network, as long as
     * this event wasn't from a broadcast in the first place.
     */
    public void cacheGroupFlushed(CacheGroupEvent event) {
        if (!Cache.NESTED_EVENT.equals(event.getOrigin()) && !CLUSTER_ORIGIN.equals(event.getOrigin())) {
            if (log.isDebugEnabled()) {
                log.debug("cacheGroupFushed called (" + event + ")");
            }

            sendNotification(new ClusterSharedNotification(event.getMap().getName(), ClusterNotification.FLUSH_GROUP, event.getGroup()));
        }
    }

    public void cachePatternFlushed(CachePatternEvent event) {
        if (!Cache.NESTED_EVENT.equals(event.getOrigin()) && !CLUSTER_ORIGIN.equals(event.getOrigin())) {
            if (log.isDebugEnabled()) {
                log.debug("cachePatternFushed called (" + event + ")");
            }

            sendNotification(new ClusterSharedNotification(event.getMap().getName(), ClusterNotification.FLUSH_PATTERN, event.getPattern()));
        }
    }

    public void cacheFlushed(CachewideEvent event) {
        if (!Cache.NESTED_EVENT.equals(event.getOrigin()) && !CLUSTER_ORIGIN.equals(event.getOrigin())) {
            if (log.isDebugEnabled()) {
                log.debug("cacheFushed called (" + event + ")");
            }

            sendNotification(new ClusterSharedNotification(event.getCache().getName(), ClusterNotification.FLUSH_CACHE, event.getDate()));
        }
    }

    /**
     * Handles incoming notification messages. This method should be called by the
     * underlying broadcasting implementation when a message is received from another
     * node in the cluster.
     *
     * @param message The incoming cluster notification message object.
     */
    public void handleClusterNotification(ClusterSharedNotification message) {
        Cache cache = getCache(message.getCacheName());
        if (cache == null) {
            log.warn("A cluster notification (" + message + ") was received, but the cache named " + message.getCacheName() + " is not registered on this machine. Notification ignored.");

            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Cluster notification (" + message + ") was received.");
        }

        switch (message.getType()) {
            case ClusterNotification.FLUSH_KEY:
                cache.flushEntry((String) message.getData(), CLUSTER_ORIGIN);
                break;
            case ClusterNotification.FLUSH_GROUP:
                cache.flushGroup((String) message.getData(), CLUSTER_ORIGIN);
                break;
            case ClusterNotification.FLUSH_PATTERN:
                cache.flushPattern((String) message.getData(), CLUSTER_ORIGIN);
                break;
            case ClusterNotification.FLUSH_CACHE:
                cache.flushAll((Date) message.getData(), CLUSTER_ORIGIN);
                break;
            default:
                log.error("The cluster notification (" + message + ") is of an unknown type. Notification ignored.");
        }
    }

    /**
     * Shuts down the JavaGroups being managed by this listener. This
     * occurs once the cache is shut down and this listener is no longer
     * in use.
     *
     * @throws com.opensymphony.oscache.base.FinalizationException
     */
    public synchronized void finialize() throws FinalizationException {
        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutting down...");
        }

        // It's possible that the notification bus is null (CACHE-154)
        if (bus != null) {
            busCount--;
            if (busCount==0)
                bus.stop();
            bus = null;
        } else {
            log.warn("Notification bus wasn't initialized or finialize was invoked before!");
        }

        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutdown complete.");
        }
    }

    /**
     * Uses JavaGroups to broadcast the supplied notification message across the cluster.
     *
     * @param message The cluster nofication message to broadcast.
     */
    protected void sendNotification(ClusterSharedNotification message) {
        bus.sendNotification(message);
    }

    /**
     * Handles incoming notification messages from JavaGroups. This method should
     * never be called directly.
     *
     * @param serializable The incoming message object. This must be a {@link ClusterNotification}.
     */
    public void handleNotification(Serializable serializable) {
        if (!(serializable instanceof ClusterNotification)) {
            log.error("An unknown cluster notification message received (class=" + serializable.getClass().getName() + "). Notification ignored.");

            return;
        }

        handleClusterNotification((ClusterSharedNotification) serializable);
    }

    /**
     * We are not using the caching, so we just return something that identifies
     * us. This method should never be called directly.
     */
    public Serializable getCache() {
        return "JavaGroupsBroadcastingListener: " + bus.getLocalAddress();
    }

    /**
     * A callback that is fired when a new member joins the cluster. This
     * method should never be called directly.
     *
     * @param address The address of the member who just joined.
     */
    public void memberJoined(Address address) {
        if (log.isInfoEnabled()) {
            log.info("A new member at address '" + address + "' has joined the cluster");
        }
    }

    /**
     * A callback that is fired when an existing member leaves the cluster.
     * This method should never be called directly.
     *
     * @param address The address of the member who left.
     */
    public void memberLeft(Address address) {
        if (log.isInfoEnabled()) {
            log.info("Member at address '" + address + "' left the cluster");
        }
    }
}

