package rssreader.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.History;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import api.client.Document;
import api.client.XObject;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 2 déc. 2006
 * Time: 16:28:30
 * To change this template use File | Settings | File Templates.
 */

public class AsyncLoadFeedCallback implements AsyncCallback {
    private RSSReader reader;

    public AsyncLoadFeedCallback(RSSReader reader) {
       this.reader = reader;
    }

    public void onSuccess(Object object) {
       reader.loadFeeds((Document) object);
    }

    public void onFailure(Throwable caught) {
       reader.showError(caught);
    }
}
