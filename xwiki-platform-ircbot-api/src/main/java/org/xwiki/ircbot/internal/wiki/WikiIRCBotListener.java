package org.xwiki.ircbot.internal.wiki;

import java.util.Map;

import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;

public class WikiIRCBotListener implements IRCBotListener, WikiIRCBotListenerConstants
{
    private String description;

    private Map<String, XDOM> events;

    private Syntax syntax;

    private Transformation macroTransformation;

    private BlockRenderer plainTextBlockRenderer;

    private IRCBot bot;

    public WikiIRCBotListener(String description, Map<String, XDOM> events, Syntax syntax,
        Transformation macroTransformation, BlockRenderer plainTextBlockRenderer, IRCBot bot)
    {
        this.description = description;
        this.events = events;
        this.syntax = syntax;
        this.macroTransformation = macroTransformation;
        this.plainTextBlockRenderer = plainTextBlockRenderer;
        this.bot = bot;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public void onConnect()
    {
        XDOM xdom = this.events.get(ON_CONNECT_EVENT_NAME);
        if (xdom != null) {
/*
            // Execute Macro Transformations on XDOM and send the result to the IRC server
            TransformationContext txContext = new TransformationContext(xdom, this.syntax);
            this.macroTransformation.transform(xdom, this.syntax);
            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            this.plainTextBlockRenderer.render(xdom, printer);

            // TODO: Split content  by new line
            this.bot.sendMessage(printer.toString());
*/
        }
    }

    @Override
    public void onDisconnect()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onJoin(String channel, String sender, String login, String hostname)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onPart(String channel, String sender, String login, String hostname)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
