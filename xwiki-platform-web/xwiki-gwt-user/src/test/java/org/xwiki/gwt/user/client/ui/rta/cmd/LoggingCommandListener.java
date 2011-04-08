package org.xwiki.gwt.user.client.ui.rta.cmd;

import java.util.List;

/**
 * A {@link CommandListener} that has a name and logs it whenever it is notified.
 * 
 * @version $Id$
 */
public class LoggingCommandListener implements CommandListener
{
    /**
     * The name of this listener.
     */
    private final String name;

    /**
     * The list of log messages.
     */
    private final List<String> log;

    /**
     * Creates a new {@link CommandListener} that logs its name.
     * 
     * @param name the name of the log
     * @param log the list where to add log messages
     */
    public LoggingCommandListener(String name, List<String> log)
    {
        this.name = name;
        this.log = log;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        log.add(name + " before(" + command + ',' + param + ')');
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        log.add(name + " after(" + command + ',' + param + ')');
    }
}
