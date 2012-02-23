package org.xwiki.ircbot.internal;

import java.io.IOException;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public interface PircBotInterface
{
    void connect(String hostname) throws IOException, IrcException, NickAlreadyInUseException;
    void reconnect() throws IOException, IrcException, NickAlreadyInUseException;
    void joinChannel(String channel);
    void disconnect();
    void identify(String password);
    void sendMessage(String target, String message);

    boolean isConnected();

    void setBotName(String botName);
}
