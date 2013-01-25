package org.jockerd.opensocial.feeds;

import java.util.ArrayList;
import java.util.List;

import org.jokerd.opensocial.api.model.ActivityEntry;
import org.jokerd.opensocial.cursors.ActivityListCursor;
import org.jokerd.opensocial.cursors.IActivityCursor;
import org.jokerd.opensocial.cursors.StreamException;

/**
 * @author kotelnikov
 */
public class FeedActivitiesCursor extends ActivityListCursor {

    public static List<IActivityCursor> getFeedCursors(String... urls)
        throws StreamException {
        List<IActivityCursor> cursors = new ArrayList<IActivityCursor>();
        for (String uri : urls) {
            FeedActivitiesCursor cursor = new FeedActivitiesCursor(uri);
            cursors.add(cursor);
        }
        return cursors;
    }

    public FeedActivitiesCursor(String uri) throws StreamException {
        List<ActivityEntry> list = FeedActivityBuilder.readActivities(uri);
        setList(list);
    }

}