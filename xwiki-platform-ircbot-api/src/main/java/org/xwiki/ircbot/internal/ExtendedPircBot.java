package org.xwiki.ircbot.internal;

import org.jibble.pircbot.PircBot;
import org.xwiki.ircbot.IRCBotListener;

public class ExtendedPircBot extends PircBot implements PircBotInterface
{
    private IRCBotListener ircBotListener;

    public ExtendedPircBot(IRCBotListener ircBotListener)
    {
        this.ircBotListener = ircBotListener;
    }

    public void setBotName(String botName)
    {
        super.setName(botName);
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        this.ircBotListener.onMessage(channel, sender, login, hostname, message);
    }

    @Override
    protected void onDisconnect()
    {
        this.ircBotListener.onDisconnect();
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname)
    {
        this.ircBotListener.onJoin(channel, sender, login, hostname);
    }

    @Override
    protected void onPart(String channel, String sender, String login, String hostname)
    {
        this.ircBotListener.onPart(channel, sender, login, hostname);
    }

    @Override
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        this.ircBotListener.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        this.ircBotListener.onPrivateMessage(sender, login, hostname, message);
    }

    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        this.ircBotListener.onNickChange(oldNick, login, hostname, newNick);
    }
}
