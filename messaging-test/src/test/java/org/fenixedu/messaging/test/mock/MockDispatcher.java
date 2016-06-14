package org.fenixedu.messaging.test.mock;

import org.fenixedu.messaging.core.dispatch.MessageDispatcher;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.test.domain.MockAsyncEmailMessageDispatchReport;
import org.fenixedu.messaging.test.domain.MockEmailMessageDispatchReport;

import com.google.common.collect.Sets;

public class MockDispatcher {

    public static MessageDispatcher async = (Message message) -> {
        int mailCount = Sets.union(Sets.union(message.getTos(), message.getCcs()), message.getBccs()).size();
        return new MockAsyncEmailMessageDispatchReport(mailCount);
    };

    public static MessageDispatcher skipper = (Message message) -> null;


    public static MessageDispatcher sync = (Message message) -> {
        int mailCount = Sets.union(Sets.union(message.getTos(), message.getCcs()), message.getBccs()).size();
        return new MockEmailMessageDispatchReport(mailCount);
    };

}
