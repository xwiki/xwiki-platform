package org.xwiki.observation.remote.internal.jgroups;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.conf.ConfiguratorFactory;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
import org.xwiki.observation.remote.jgroups.JGroupsReceiver;

/**
 * JGroups based implementation of {@link NetworkAdapter}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Component("jgroups")
public class JGroupsNetworkAdapter extends AbstractLogEnabled implements NetworkAdapter
{
    /**
     * Relative path where to find jgroups channels configurations.
     */
    public static final String CONFIGURATION_PATH = "observation/remote/jgroups/";

    /**
     * The container used to access configuration files.
     */
    @Requirement
    private Container container;

    /**
     * Used to lookup the receiver corresponding to the channel identifier.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The network channels.
     */
    private Map<String, JChannel> channels = new ConcurrentHashMap<String, JChannel>();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.NetworkAdapter#send(org.xwiki.observation.remote.RemoteEventData)
     */
    public void send(RemoteEventData remoteEvent)
    {
        getLogger().debug("Send JGroups remote event [" + remoteEvent + "]");

        Message message = new Message(null, null, remoteEvent);

        // Send message to jgroups channels
        for (Map.Entry<String, JChannel> entry : this.channels.entrySet()) {
            try {
                entry.getValue().send(message);
            } catch (Exception e) {
                getLogger().error("Fail to send message to the channel [" + entry.getKey() + "]", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.NetworkAdapter#startChannel(java.lang.String)
     */
    public void startChannel(String channelId) throws RemoteEventException
    {
        if (this.channels.containsKey(channelId)) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] already started", channelId));
        }

        JChannel channel;
        try {
            channel = createChannel(channelId);
            channel.connect("event");

            this.channels.put(channelId, channel);
        } catch (Exception e) {
            throw new RemoteEventException("Failed to create channel [" + channelId + "]", e);
        }

        getLogger().info(MessageFormat.format("Channel [{0}] started", channelId));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.NetworkAdapter#stopChannel(java.lang.String)
     */
    public void stopChannel(String channelId) throws RemoteEventException
    {
        JChannel channel = this.channels.get(channelId);

        if (channel == null) {
            throw new RemoteEventException(MessageFormat.format("Channel [{0}] is not started", channelId));
        }

        channel.close();

        this.channels.remove(channelId);

        getLogger().info(MessageFormat.format("Channel [{0}] stoped", channelId));
    }

    /**
     * Create a new channel.
     * 
     * @param channelId the identifier of the channel to create
     * @return the new channel
     * @throws ComponentLookupException failed to get default {@link JGroupsReceiver}
     * @throws ChannelException failed to create channel
     */
    private JChannel createChannel(String channelId) throws ComponentLookupException, ChannelException
    {
        // load configuration
        ProtocolStackConfigurator channelConf;
        try {
            channelConf = loadChannelConfiguration(channelId);
        } catch (IOException e) {
            throw new ChannelException("Failed to load configuration for the channel [" + channelId + "]", e);
        }

        // get Receiver
        JGroupsReceiver channelReceiver;
        try {
            channelReceiver = this.componentManager.lookup(JGroupsReceiver.class, channelId);
        } catch (ComponentLookupException e) {
            channelReceiver = this.componentManager.lookup(JGroupsReceiver.class);
        }

        // create channel
        JChannel channel = new JChannel(channelConf);

        channel.setReceiver(channelReceiver);
        channel.setOpt(JChannel.LOCAL, false);

        return channel;
    }

    /**
     * Load channel configuration.
     * 
     * @param channelId the identifier of the channel
     * @return the channel configuration
     * @throws IOException failed to load configuration file
     * @throws ChannelException failed to creation channel configuration
     */
    private ProtocolStackConfigurator loadChannelConfiguration(String channelId) throws IOException, ChannelException
    {
        ProtocolStackConfigurator configurator = null;

        String path = "/WEB-INF/" + CONFIGURATION_PATH + channelId + ".xml";

        InputStream is = this.container.getApplicationContext().getResourceAsStream(path);

        if (is != null) {
            configurator = XmlConfigurator.getInstance(is);
        } else {
            getLogger().warn(
                "Can't find a configuration for channel [" + channelId + "] at [" + path + "]. Using "
                    + JChannel.DEFAULT_PROTOCOL_STACK + " JGRoups default configuration.");

            configurator = ConfiguratorFactory.getStackConfigurator(JChannel.DEFAULT_PROTOCOL_STACK);
        }

        return configurator;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.NetworkAdapter#stopAllChannels()
     */
    public void stopAllChannels() throws RemoteEventException
    {
        for (Map.Entry<String, JChannel> channelEntry : this.channels.entrySet()) {
            channelEntry.getValue().close();
        }

        this.channels.clear();

        getLogger().info("All channels stoped");
    }
}
