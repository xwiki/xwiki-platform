package org.xwiki.ircbot;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface IRCBotListener
{
    String getDescription();

    void onConnect();
    void onDisconnect();

    /**
     * @param channel the channel to which the message was sent
     * @param sender the nick of the person who sent the message
     * @param login the login of the person who sent the message
     * @param hostname the hostname of the person who sent the message
     * @param message the actual message sent to the channel
     */
    void onMessage(String channel, String sender, String login, String hostname, String message);

    void onJoin(String channel, String sender, String login, String hostname);
    void onPart(String channel, String sender, String login, String hostname);
    void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason);
    void onPrivateMessage(String sender, String login, String hostname, String message);
    void onNickChange(String oldNick, String login, String hostname, String newNick);
}
