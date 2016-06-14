package org.fenixedu.messaging.core.domain;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Strings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class MessagingSystemTest {

    /**
     * System Sender is a usable valid sender even without bootstrap configuration
     */
    @Test
    public void systemSender() {
        Sender system = MessagingSystem.systemSender();
        assertNotNull(system);
        assertTrue(MessagingSystem.Util.isValidEmail(system.getAddress()));
        assertFalse(Strings.isNullOrEmpty(system.getName()));
        assertNotNull(system.getMembers());
        assertNotNull(system.getPolicy());
    }

    // TODO test pruneMessages
    // TODO test getMessageDispatchSet
    // TODO test getUnfinishedReportsSet

    //FIXME add/remove UnfinishedReports is public? getSenderSet, getMessageSet, getTemplateSet as well... protect them all
    //TODO have a Blacklist.get() method and protect MS.getBlacklist()
    //TODO actually blacklist has no need to be linked to MessagingSystem... connection is not used really. they're both
    // singletons, why connect them?
}
