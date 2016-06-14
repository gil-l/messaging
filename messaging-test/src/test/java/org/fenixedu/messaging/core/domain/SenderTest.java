package org.fenixedu.messaging.core.domain;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

import java.util.Set;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.messaging.core.domain.Sender.SenderBuilder;
import org.fenixedu.messaging.core.exception.MessagingDomainException;
import org.fenixedu.messaging.test.util.TestHelpers;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fenixedu.messaging.test.util.TestConstants.ADDR_A;
import static org.fenixedu.messaging.test.util.TestConstants.ADDR_B;
import static org.fenixedu.messaging.test.util.TestConstants.GRP_A;
import static org.fenixedu.messaging.test.util.TestConstants.GRP_M;
import static org.fenixedu.messaging.test.util.TestConstants.PERIOD;
import static org.fenixedu.messaging.test.util.TestConstants.STR_A;
import static org.fenixedu.messaging.test.util.TestConstants.STR_B;
import static org.fenixedu.messaging.test.util.TestHelpers.testRecipientSetters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class SenderTest {

    /**
     * Builder throws when required address parameter is null
     */
    @Test(expected = MessagingDomainException.class)
    public void nullAddress() {
        Sender.from(null);
    }

    /**
     * Builder throws when required address parameter is not a valid email
     */
    @Test(expected = MessagingDomainException.class)
    public void invalidAddress() {
        Sender.from(STR_A);
    }

    /**
     * Builder provides default values enabling the straightforward creation of a valid default empty sender
     * Created Sender is not null
     * Automatic properties and relations are set
     * |- Sender address is provided address
     * Default properties are set
     * |- Name is empty string
     * |- Deletion policy is unlimited
     * |- Sender is not enabled to send html
     * |- Nobody has interface access to sender
     * |- Does not have interface allowed recipient groups
     * |- Does not have a reply to address
     * `- Message set is empty
     */
    @Test
    public void emptySender() {
        Sender sender = Sender.from(ADDR_A).build();
        assertNotNull(sender);

        assertEquals(sender.getAddress(), ADDR_A);

        assertNotNull(sender.getName());
        assertTrue(sender.getName().isEmpty());
        assertEquals(sender.getPolicy(), MessageStoragePolicy.keepAll());
        assertFalse(sender.getHtmlEnabled());
        assertEquals(sender.getMembers(), Group.nobody());
        assertTrue(sender.getRecipients().isEmpty());
        assertTrue(sender.getMessageSet().isEmpty());
        assertNull(sender.getReplyTo());
    }

    /**
     * Sets sender name to provided string
     * overrides previous values
     */
    @Test
    public void senderName() {
        SenderBuilder builder = Sender.from(ADDR_A);
        Sender sender = builder.as(STR_A).as(STR_B).build();
        assertEquals(sender.getName(), STR_B);
    }

    /**
     * Null sender name produces a Null Pointer Exception
     */
    @Test(expected = NullPointerException.class)
    public void nullSenderName() {
        Sender.from(ADDR_A).as(null);
    }

    /**
     * Sets sender policies as provided
     * Null constants.PERIOD sets unlimited, unless together with amount when it is ignored
     * Negative amount sets to 0
     */
    @Test
    public void senderPolicy() {
        SenderBuilder builder = Sender.from(ADDR_A);
        MessageStoragePolicy policy;

        policy = MessageStoragePolicy.keep(2, PERIOD);
        assertSame(builder.storagePolicy(policy).build().getPolicy(), policy);

        policy = builder.keepMessages(2).build().getPolicy();
        assertTrue(policy.getAmount() == 2);
        assertNull(policy.getPeriod());

        policy = builder.keepMessages(-1).build().getPolicy();
        assertTrue(policy.getAmount() == 0);
        assertNull(policy.getPeriod());

        policy = builder.keepMessages(PERIOD).build().getPolicy();
        assertEquals(policy.getPeriod(), PERIOD);
        assertNull(policy.getAmount());

        policy = builder.keepMessages(2, null).build().getPolicy();
        assertTrue(policy.getAmount() == 2);
        assertNull(policy.getPeriod());

        policy = builder.keepMessages(2, PERIOD).build().getPolicy();
        assertTrue(policy.getAmount() == 2);
        assertEquals(policy.getPeriod(), PERIOD);

        policy = builder.keepMessages(-1, PERIOD).build().getPolicy();
        assertTrue(policy.getAmount() == 0);
        assertEquals(policy.getPeriod(), PERIOD);
    }

    /**
     * Sets html enabled to the provided boolean
     */
    @Test
    public void senderHtmlEnabled() {
        SenderBuilder builder = Sender.from(ADDR_A);
        assertTrue(builder.htmlEnabled(true).build().getHtmlEnabled());

        assertFalse(builder.htmlEnabled(false).build().getHtmlEnabled());
    }

    /**
     * Sets reply to address to provided address
     * Reply to sender setter copies sender's default reply to address
     * Invalid addresses set null reply to
     */
    @Test
    public void senderReplyTo() {
        SenderBuilder builder = Sender.from(ADDR_A);

        builder.replyTo(null);
        assertNull(builder.build().getReplyTo());

        builder.replyTo(ADDR_A);
        assertEquals(builder.build().getReplyTo(), ADDR_A);

        builder.replyTo("not really a valid address...");
        assertNull(builder.build().getReplyTo());
    }

    /**
     * Sets interface access group
     * Null Group or PersistentGroup sets nobody group
     */
    @Test
    public void senderMembers() {
        SenderBuilder builder = Sender.from(ADDR_A);

        assertEquals(builder.members(GRP_M).build().getMembers(), GRP_M);

        assertEquals(builder.members(null).build().getMembers(), Group.nobody());

        assertEquals(builder.members(GRP_M).build().getMembers(), GRP_M);

        assertEquals(builder.members(null).build().getMembers(), Group.nobody());
    }

    /**
     * @see TestHelpers#testRecipientSetters
     */
    @Test
    public void senderRecipients() {
        SenderBuilder builder = Sender.from(ADDR_A);
        testRecipientSetters(builder::recipients,builder::recipients,builder::recipients,()->builder.build().getRecipients());
    }

    @Test
    public void regularSender() {
        Sender sender = Sender.from(ADDR_A).as(STR_A).keepMessages(2, PERIOD).replyTo(ADDR_B).htmlEnabled(true)
                .members(GRP_M).recipients(GRP_A).build();
        assertEquals(sender.getAddress(), ADDR_A);
        assertEquals(sender.getName(), STR_A);
        assertEquals(sender.getReplyTo(), ADDR_B);
        MessageStoragePolicy policy = sender.getPolicy();
        assertTrue(policy.getAmount() == 2);
        assertEquals(policy.getPeriod(), PERIOD);
        assertTrue(sender.getHtmlEnabled());
        assertEquals(sender.getMembers(), GRP_M);
        Set<Group> recipients = sender.getRecipients();
        assertTrue(recipients.size() == 1 && recipients.contains(GRP_A));
    }

    private static Sender testSender = null;
    static Sender testSender() {
        return testSender != null ? testSender : (testSender = Sender.from(ADDR_A).build()); //FIXME make it a more regular sender
    }

    //TODO format as Message Test
    //TODO test ordering
    //FIXME amount numbers are not a constant. all is ok when they are local to the test only but the regular sender test has
    // the check in separate
    //TODO more senders to cover more test cases. As other tests will require them, then might as well add them here and test their creation too.
}
