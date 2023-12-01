package org.mengyun.commons.collapser.queue;

import com.lmax.disruptor.EventFactory;

/**
 * Created by changmingxie on 12/2/15.
 */
public class GroupEventFactory implements EventFactory<GroupEvent> {
    @Override
    public GroupEvent newInstance() {
        return new GroupEvent();
    }
}
