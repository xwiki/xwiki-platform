package rssreader.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.Window;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 21 janv. 2007
 * Time: 21:16:07
 * To change this template use File | Settings | File Templates.
 */
public class PostTagsAsyncCallback implements AsyncCallback {
    RSSReader reader;
    HTML tagshtml;
    String tags;

    public PostTagsAsyncCallback(RSSReader rssReader, HTML tagshtml, String tags) {
        this.reader = rssReader;
        this.tagshtml = tagshtml;
        this.tags = tags;
    }

    public void onFailure(Throwable throwable) {
        reader.showError((Throwable) throwable);
    }
    public void onSuccess(Object object) {
        tagshtml.setHTML(tags);
    }

}
