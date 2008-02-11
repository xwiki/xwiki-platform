package org.xwiki.plugin.activitystream.plugin;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 févr. 2008
 * Time: 23:58:14
 * To change this template use File | Settings | File Templates.
 */
public class ActivityEvent extends Api {

    protected org.xwiki.plugin.activitystream.api.ActivityEvent event;

    public ActivityEvent(org.xwiki.plugin.activitystream.api.ActivityEvent event, XWikiContext context) {
        super(context);
        this.event = event;
    }

    /**
     * @return The unique ID of the event
     */
    public String getEventId() {
        return event.getEventId();
    }


    /**
     * @return The request ID
     */
    public String getRequestId() {
        return event.getRequestId();
    }

    /**
     * @return The priority of the event
     */
    public int getPriority() {
        return event.getPriority();
    }

    /**
     * @return The type of the event
     */
    public String getType() {
        return event.getType();
    }


    /**
     * @return The application name
     */
    public String getApplication() {
        return event.getApplication();
    }

    /**
     * @return The stream name
     */
    public String getStream() {
        return event.getStream();
    }


    /**
     * @return The stream name
     */
    public Date getDate() {
        return event.getDate();
    }



    /**
     * @return The wiki name of the user creating the event
     */
    public String getUser() {
        return event.getUser();
    }


    /**
     * @return The wiki name in which the event was created
     */
    public String getWiki() {
        return event.getWiki();
    }


    /**
     * @return The space name in which the event was created
     */
    public String getSpace() {
        return event.getSpace();
    }


    /**
     * @return The page of the event
     */
    public String getPage() {
        return event.getPage();
    }


    /**
     * @return The target url
     */
    public String getUrl() {
        return event.getUrl();
    }


    /**
     * @return The title of the event
     */
    public String getTitle() {
        return event.getTitle();
    }


    /**
     * @return The Body of the event
     */
    public String getBody() {
        return event.getBody();
    }

    /**
     * @return The first param of the event
     */
    public String getParam1() {
        return event.getParam1();
    }

    /**
     * @return The second param of the event
     */
    public String getParam2() {
        return event.getParam2();
    }

    /**
     * @return The third param of the event
     */
    public String getParam3() {
        return event.getParam3();
    }
    /**
     * @return The fourth param of the event
     */
    public String getParam4() {
        return event.getParam4();
    }

    /**
     * @return The fifth param of the event
     */
    public String getParam5() {
        return event.getParam5();
    }


    /**
     * Retrieves the event in displayable format
     *
     * @return
     */
    public String getDisplayTitle() {
        return event.getDisplayTitle(context);
    }

    /**
     * Retrieves the event body in displayable format
     *
     * @return
     */
    public String getDisplayBody() {
        return event.getDisplayBody(context);
    }

    /**
     * Retrieves the event date in displayable format
     *
     * @return
     */
    public String getDisplayDate() {
        return event.getDisplayDate(context);
    }

    /**
     * Retrieves the event user in displayable format
     *
     * @return
     */
    public String getDisplayUser() {
        return event.getDisplayUser(context);
    }

}
