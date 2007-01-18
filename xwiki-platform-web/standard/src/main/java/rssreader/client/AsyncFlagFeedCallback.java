package rssreader.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import api.client.XObject;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 9 janv. 2007
 * Time: 22:49:27
 * To change this template use File | Settings | File Templates.
 */
public class AsyncFlagFeedCallback implements AsyncCallback {
    private RSSReader reader;
    private XObject feedentry;
    private int newflagstatus;
    private Image link;

    public AsyncFlagFeedCallback(RSSReader reader, XObject feedentry, int newflagstatus, Image link) {
        this.reader = reader;
        this.feedentry = feedentry;
        this.newflagstatus = newflagstatus;
        this.link = link;
    }

    public void onFailure(Throwable caught) {
        reader.showError(caught);
    }

    public void onSuccess(Object result) {
         reader.flagFeedCallback(feedentry, newflagstatus, link,((Boolean)result).booleanValue());
    }
}
