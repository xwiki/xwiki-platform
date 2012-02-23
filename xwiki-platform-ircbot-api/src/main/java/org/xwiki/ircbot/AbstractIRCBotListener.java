package org.xwiki.ircbot;

public abstract class AbstractIRCBotListener implements IRCBotListener
{
    @Override
    public void onConnect()
    {
        // Do nothing
    }

    @Override
    public void onDisconnect()
    {
        // Do nothing
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname)
    {
        // Do nothing
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        // Do nothing
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        // Do nothing
    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname)
    {
        // Do nothing
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        // Do nothing
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        // Do nothing
    }
}
