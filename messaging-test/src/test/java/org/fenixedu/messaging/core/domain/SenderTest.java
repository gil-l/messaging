package org.fenixedu.messaging.core.domain;

import jersey.repackaged.com.google.common.collect.Lists;
import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.messaging.core.domain.Sender.SenderBuilder;
import org.fenixedu.messaging.core.exception.MessagingDomainException;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class SenderTest {
    private static final String NAME = "name";
    private static final Period PERIOD = Period.parse("P1Y");
    private static final String REPLY_TO = "no.reply@senders.com";
    private static final int AMOUNT = 3;

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
        Sender.from(TestConstants.STR_A);
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
        Sender sender = Sender.from(TestConstants.ADDR_A).build();
        assertNotNull(sender);

        assertEquals(sender.getAddress(), TestConstants.ADDR_A);

        assertNotNull(sender.getName());
        assertTrue(sender.getName().isEmpty());
        assertEquals(sender.getPolicy(), MessageDeletionPolicy.unlimited());
        assertFalse(sender.getHtmlEnabled());
        assertEquals(sender.getMembers(), Group.nobody());
        assertTrue(sender.getRecipients().isEmpty());
        assertTrue(sender.getMessageSet().isEmpty());
        assertNull(sender.getReplyTo());
    }

    /**
     * Sets sender name to provided string
     * Null name sets empty string
     */
    @Test
    public void senderName() {
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        Sender sender = builder.as(null).build();
        assertNotNull(sender.getName());
        
        sender = builder.as(TestConstants.STR_A).as(TestConstants.STR_A).build();
        assertEquals(sender.getName(), TestConstants.STR_A);
    }


    //TODO format as Message Test
    
    @Test
    public void senderPolicy() { // Null safe with a default and reusable
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        int amount = 3;
        MessageDeletionPolicy policy = MessageDeletionPolicy.keepAmountForDuration(amount, PERIOD);
        Sender sender = builder.keepMessages(2).keepMessages(amount).build();
        assertTrue(sender.getPolicy().getAmount().intValue() == amount);
        assertNull(sender.getPolicy().getPeriod());
        sender = builder.keepMessages(null).build();
        assertNotNull(sender.getPolicy());
        sender = builder.keepMessages(Period.parse("P2Y")).keepMessages(PERIOD).build();
        assertEquals(sender.getPolicy().getPeriod(), PERIOD);
        assertNull(sender.getPolicy().getAmount());
        sender = builder.keepMessages(2, null).build();
        assertNotNull(sender.getPolicy());
        sender = builder.keepMessages(2, Period.parse("P2Y")).keepMessages(amount, PERIOD).build();
        assertTrue(sender.getPolicy().getAmount().intValue() == amount);
        assertEquals(sender.getPolicy().getPeriod(), PERIOD);
        sender = builder.deletionPolicy(null).build();
        assertNotNull(sender.getPolicy());
        sender = builder.deletionPolicy(MessageDeletionPolicy.unlimited()).deletionPolicy(policy).build();
        assertSame(sender.getPolicy(), policy);
    }

    @Test
    public void senderHtmlEnabled() {
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        Sender sender = builder.htmlEnabled(true).build();
        assertTrue(sender.getHtmlEnabled());
        sender = builder.htmlEnabled(true).htmlEnabled(false).build();
        assertFalse(sender.getHtmlEnabled());
    }

    @Test
    public void senderReplyTo() { // Null safe with no default and reusable
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        Sender sender = builder.replyTo(null).build();
        assertNull(sender.getReplyTo());
        sender = builder.replyTo(null).replyTo(REPLY_TO).build();
        assertEquals(sender.getReplyTo(), REPLY_TO);
    }

    @Test
    public void senderMembers() { // Null safe with NobodyGroup as default and reusable
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        Group anyone = Group.anyone(), nobody = Group.nobody();
        Sender sender = builder.members((Group) null).build();
        assertEquals(sender.getMembers(), nobody);
        sender = builder.members((PersistentGroup) null).build();
        assertSame(sender.getMembers(), nobody);
        sender = builder.members(nobody).members(anyone).build();
        assertSame(sender.getMembers(), anyone);
    }

    @Test
    public void senderRecipients() { // Null safe with empty by default and reusable but as additive, not replacing (and no repeats, it's a set)
        SenderBuilder builder = Sender.from(TestConstants.ADDR_A);
        Group anyone = Group.anyone(), nobody = Group.nobody();
        Sender sender =
                builder.recipients((Group) null).recipients((Stream<Group>) null).recipients((Collection<Group>) null).build();
        Set<Group> recipients = sender.getRecipients();
        assertTrue(recipients.isEmpty());
        sender = builder.recipients(nobody).build();
        recipients = sender.getRecipients();
        assertTrue(recipients.size() == 1);
        assertEquals(recipients.iterator().next(), nobody);
        sender = builder.recipients(anyone).recipients(Lists.newArrayList(anyone, anyone, anyone))
                .recipients(Stream.of(nobody, nobody)).build();
        recipients = sender.getRecipients();
        assertTrue(recipients.size() == 2);
        assertTrue(recipients.contains(nobody));
        assertTrue(recipients.contains(anyone));
    }

    @Test
    public void regularSender() {
        Sender sender = newRegularSender();
        assertEquals(sender.getAddress(), TestConstants.ADDR_A);
        assertEquals(sender.getName(), NAME);
        assertEquals(sender.getReplyTo(), REPLY_TO);
        MessageDeletionPolicy policy = sender.getPolicy();
        assertTrue(policy.getAmount() == AMOUNT);
        assertEquals(policy.getPeriod(), PERIOD);
        assertTrue(sender.getHtmlEnabled());
        assertEquals(sender.getMembers(), Group.nobody());
        assertEquals(sender.getRecipients().iterator().next(), Group.anyone());
    }

    public static Sender newEmptySender() {
        return Sender.from(TestConstants.ADDR_A).build();
    }

    ;

    public static final Sender newRegularSender() {
        return Sender.from(TestConstants.ADDR_A).as(NAME).keepMessages(AMOUNT, PERIOD).replyTo(REPLY_TO).htmlEnabled(true)
                .members(Group.nobody()).recipients(Group.anyone()).build();
    }
    //TODO more senders to cover more test cases. As other tests will require them, then might as well add them here and test their creation too.
}
