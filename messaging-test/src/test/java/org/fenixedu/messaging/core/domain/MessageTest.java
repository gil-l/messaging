package org.fenixedu.messaging.core.domain;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message.MessageBuilder;
import org.fenixedu.messaging.core.domain.Message.TemplateMessageBuilder;
import org.fenixedu.messaging.core.exception.MessagingDomainException;
import org.fenixedu.messaging.test.mock.MockDispatcher;
import org.fenixedu.messaging.test.util.TestConstants;
import org.fenixedu.messaging.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fenixedu.messaging.test.util.TestConstants.ADDR_A;
import static org.fenixedu.messaging.test.util.TestConstants.ADDR_INVALID;
import static org.fenixedu.messaging.test.util.TestConstants.LOC_A;
import static org.fenixedu.messaging.test.util.TestConstants.LOC_B;
import static org.fenixedu.messaging.test.util.TestConstants.LOC_DEF;
import static org.fenixedu.messaging.test.util.TestConstants.LS_A;
import static org.fenixedu.messaging.test.util.TestConstants.LS_AA;
import static org.fenixedu.messaging.test.util.TestConstants.LS_AB;
import static org.fenixedu.messaging.test.util.TestConstants.LS_B;
import static org.fenixedu.messaging.test.util.TestConstants.LS_BB;
import static org.fenixedu.messaging.test.util.TestConstants.LS_BC;
import static org.fenixedu.messaging.test.util.TestConstants.LS_C;
import static org.fenixedu.messaging.test.util.TestConstants.LS_CA;
import static org.fenixedu.messaging.test.util.TestConstants.LS_CC;
import static org.fenixedu.messaging.test.util.TestConstants.LS_EMPTY;
import static org.fenixedu.messaging.test.util.TestConstants.STR_A;
import static org.fenixedu.messaging.test.util.TestConstants.STR_B;
import static org.fenixedu.messaging.test.util.TestConstants.STR_C;
import static org.fenixedu.messaging.test.util.TestHelpers.expectNullPointerException;
import static org.fenixedu.messaging.test.util.TestHelpers.shortWait;
import static org.fenixedu.messaging.test.util.TestHelpers.testAddressSetters;
import static org.fenixedu.messaging.test.util.TestHelpers.testContentSetters;
import static org.fenixedu.messaging.test.util.TestHelpers.testRecipientSetters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(FenixFrameworkRunner.class)
public class MessageTest {

    //FIXME cannot test templates as ServletContainerInitializer is not run. Find alternative way.
    //XXX Message deletion cannot be tested with current no repository backend
    //XXX user mocking and dispatcher setting cannot be done in class startup/cleanup as they access domain objects
    //TODO ensure Message.fromSystem produces regular builder (we use it in most tests) [PROBLEM: can't inspect builder state]
    //TODO separate message dispatch tests and test message skipping


    @Before
    public void startup() {
        Authenticate.mock(TestConstants.get().USER);
    }

    @After
    public void cleanup() {
        Authenticate.unmock();
    }

    /**
     * Builder supplier throws when required sender parameter is null
     */
    @Test(expected = MessagingDomainException.class)
    public void nullSender() {
        Message.from(null).send();
    }

    /**
     * Builder provides default values enabling the straightforward creation of a valid default empty message:
     * Created Message is not null
     * Automatic properties and relations are set
     * |- Message Sender is provided sender
     * |- Message user is previously mocked logged user
     * |- Creation date is set to 'now'
     * |- Dispatch data is null
     * `- Message is pending dispatch
     * Default properties are set
     * |- Empty Recipient sets
     * |- Empty Content
     * |- Default locale as preferred locale
     * `- Null reply to address
     * Message can be dispatched normally
     * |- Message is linked to the dispatch report produced by dispatch method
     * |- Message is removed from the pending queue
     * `- Message sent date is updated once report's delivery finishes
     * XXX Delivery and report handling should be tested further within dispatcher modules
     * FIXME Date testing is for now reduced to a null check. How can I mock/test the 'now' aspect?
     */
    @Test
    public void emptyMessage() {
        Sender sender = SenderTest.testSender();
        Message message = Message.from(sender).send();
        assertNotNull(message);

        assertSame(message.getSender(), sender);
        assertSame(message.getCreator(), TestConstants.get().USER);
        assertNotNull(message.getCreated());
        assertNull(message.getSent());
        assertNull(message.getDispatchReport());
        assertSame(message.getMessagingSystemFromPendingDispatch(), MessagingSystem.getInstance());

        assertTrue(message.getBccGroups().isEmpty());
        assertTrue(message.getCcGroups().isEmpty());
        assertTrue(message.getToGroups().isEmpty());
        assertNull(message.getSingleBccs());
        LocalizedString ls = message.getSubject();
        assertNotNull(ls);
        assertTrue(ls.isEmpty());
        ls = message.getHtmlBody();
        assertNotNull(ls);
        assertTrue(ls.isEmpty());
        ls = message.getTextBody();
        assertNotNull(ls);
        assertTrue(ls.isEmpty());
        assertSame(message.getPreferredLocale(), LOC_DEF);
        assertNull(message.getReplyTo());

        MessagingSystem.setMessageDispatcher(MockDispatcher.sync);
        MessageDispatchReport report = MessagingSystem.dispatch(message);
        assertNotNull(report);
        assertFalse(MessagingSystem.getPendingMessages().stream().anyMatch(m->m.equals(message)));
        assertSame(report, message.getDispatchReport());
        assertNotNull(message.getSent());
        MessagingSystem.setMessageDispatcher(null);
    }

    /**
     * MessageBuilder.from overwrites initially supplied sender
     */
    @Test
    public void messageFrom() {
        MessageBuilder builder = Message.fromSystem();
        Sender s = SenderTest.testSender();
        assertSame(builder.from(s).send().getSender(), s);
    }

    /**
     *  MessageBuilder.from throws MessageDomainException on null
     */
    @Test(expected = MessagingDomainException.class)
    public void messageFromNull() {
        MessageBuilder builder = Message.fromSystem();
        builder.from(null);
    }

    //FIXME not very atomic... succint though.
    /**
     * @see TestHelpers#testRecipientSetters
     */
    @Test
    public void messageBcc() {
        MessageBuilder builder = Message.fromSystem();
        TestHelpers.testRecipientSetters(builder::bcc, builder::bcc, builder::bcc, () -> builder.send().getBccGroups());
    }

    /**
     * @see TestHelpers#testRecipientSetters
     */
    @Test
    public void messageCc() {
        MessageBuilder builder = Message.fromSystem();
        testRecipientSetters(builder::cc, builder::cc, builder::cc, () -> builder.send().getCcGroups());
    }

    /**
     * @see TestHelpers#testRecipientSetters
     */
    @Test
    public void messageTo() {
        MessageBuilder builder = Message.fromSystem();
        testRecipientSetters(builder::to, builder::to, builder::to, () -> builder.send().getToGroups());
    }

    /**
     * @see TestHelpers#testAddressSetters
     */
    @Test
    public void messageSingleBcc() {
        MessageBuilder builder = Message.fromSystem();
        testAddressSetters(builder::singleBcc, builder::singleBcc, builder::singleBcc, () -> builder.send().getSingleBccsSet());
    }

    /**
     * Allow setting subject, text body and html body simultaneously
     * Follows the same restrictions as individual setters
     * The content order is always subject, text body and html body
     */
    @Test
    public void messageContent() {
        MessageBuilder builder = Message.fromSystem();
        Message message;

        builder.content(LS_A, LS_B, LS_C);
        message = builder.send();
        assertSame(message.getSubject(), LS_A);
        assertSame(message.getTextBody(), LS_B);
        assertSame(message.getHtmlBody(), LS_C);

        builder.content(STR_A, STR_B, STR_C);
        message = builder.send();
        assertEquals(message.getSubject(), LS_AA);
        assertEquals(message.getTextBody(), LS_BB);
        assertEquals(message.getHtmlBody(), LS_CC);

        builder.content(STR_B, STR_C, STR_A);
        message = builder.send();
        assertEquals(message.getSubject(), LS_AB);
        assertEquals(message.getTextBody(), LS_BC);
        assertEquals(message.getHtmlBody(), LS_CA);

        builder.content((String) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), LS_A);
        assertEquals(message.getTextBody(), LS_B);
        assertEquals(message.getHtmlBody(), LS_C);

        builder.content((String) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), LS_A);
        assertEquals(message.getTextBody(), LS_B);
        assertEquals(message.getHtmlBody(), LS_C);

        builder.content(null, null, null, LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), LS_EMPTY);
        assertEquals(message.getTextBody(), LS_EMPTY);
        assertEquals(message.getHtmlBody(), LS_EMPTY);

        builder.content(null, null, null, LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), LS_EMPTY);
        assertEquals(message.getTextBody(), LS_EMPTY);
        assertEquals(message.getHtmlBody(), LS_EMPTY);

        builder.content(STR_C, STR_A, STR_B, LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), LS_C);
        assertEquals(message.getTextBody(), LS_A);
        assertEquals(message.getHtmlBody(), LS_B);

        builder.content(STR_A, STR_B, STR_C, LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), LS_A);
        assertEquals(message.getTextBody(), LS_B);
        assertEquals(message.getHtmlBody(), LS_C);

        expectNullPointerException(() -> builder.content(null, LS_A, LS_B));
        expectNullPointerException(() -> builder.content(LS_A, null, LS_B));
        expectNullPointerException(() -> builder.content(LS_A, LS_B, null));
        expectNullPointerException(() -> builder.content(STR_A, STR_A, STR_A, null));
    }

    /**
     * returns locales for which there is message content
     */
    @Test
    public void messageContentLocales() {
        MessageBuilder builder = Message.fromSystem();
        assertTrue(builder.send().getContentLocales().isEmpty());

        builder.content(STR_A, STR_B, STR_C);
        Set<Locale> locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(LOC_DEF));

        builder.textBody(STR_B, LOC_A);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(LOC_DEF) && locales.contains(LOC_A));

        builder.htmlBody(STR_C, LOC_B);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 3 && locales.contains(LOC_DEF) && locales.contains(LOC_A) && locales.contains(LOC_B));

        builder.htmlBody(null, LOC_B);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(LOC_DEF) && locales.contains(LOC_A));

        builder.textBody(null, LOC_A);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(LOC_DEF));

        builder.subject((String) null);
        builder.textBody((String) null);
        builder.htmlBody((String) null);
        assertTrue(builder.send().getContentLocales().isEmpty());
    }

    /**
     * @see TestHelpers#testContentSetters
     */
    @Test
    public void messageSubject() {
        MessageBuilder builder = Message.fromSystem();
        testContentSetters(builder::subject, builder::subject, builder::subject, () -> builder.send().getSubject());
    }

    /**
     * @see TestHelpers#testContentSetters
     */
    @Test
    public void messageTextBody() {
        MessageBuilder builder = Message.fromSystem();
        testContentSetters(builder::textBody, builder::textBody, builder::textBody, () -> builder.send().getTextBody());
    }

    /**
     * @see TestHelpers#testContentSetters
     */
    @Test
    public void messageHtmlBody() {
        MessageBuilder builder = Message.fromSystem();
        testContentSetters(builder::htmlBody, builder::htmlBody, builder::htmlBody, () -> builder.send().getHtmlBody());
    }


    /**
     * Sets reply to address to provided address
     * Reply to sender setter copies sender's default reply to address
     * Invalid addresses set null reply to
     */
    @Test
    public void messageReplyTo() {
        Sender sender = SenderTest.testSender();
        MessageBuilder builder = Message.from(sender);

        builder.replyTo(null);
        assertNull(builder.send().getReplyTo());

        builder.replyTo(ADDR_A);
        assertEquals(builder.send().getReplyTo(), ADDR_A);

        builder.replyToSender();
        assertEquals(builder.send().getReplyTo(), sender.getReplyTo());

        builder.replyTo(ADDR_INVALID);
        assertNull(builder.send().getReplyTo());

    }

    /**
     * Sets preferred locale to provided locale
     * Null Locale throws NullPointerException
     */
    @Test
    public void messagePreferredLocale() {
        MessageBuilder builder = Message.fromSystem();

        builder.preferredLocale(LOC_A);
        assertEquals(builder.send().getPreferredLocale(), LOC_A);

        builder.preferredLocale(LOC_B);
        assertEquals(builder.send().getPreferredLocale(), LOC_B);

        expectNullPointerException(() -> builder.preferredLocale(LOC_B));
    }

    /**
     * Messages are ordered by descending creation date
     */
    @Test
    public void messageOrder() {
        Message mC = Message.fromSystem().send();
        shortWait();
        Message mB = Message.fromSystem().send();
        shortWait();
        Message mA = Message.fromSystem().send();
        List<Message> ordered = Stream.of(mC, mB, mA).sorted().collect(Collectors.toList());
        assertSame(ordered.get(0), mA);
        assertSame(ordered.get(1), mB);
        assertSame(ordered.get(2), mC);
    }



    @Test
    public void messageTemplate() {
        // Template Builder compiles provided template, setting message content appropriately
        // The process can be repeated overriding the current compiled contents
        // Standard content setters also override template compiled content
        // Template methods throw if template does not exist
        // TODO throws have to be tested in other methods or without resorting to expected as this can mask other errors
        // TODO Further tests of Template functionality in Template Tests

        MessageBuilder builder = Message.fromSystem();
        TemplateMessageBuilder tBuilder = builder.template("TEST_TEMPLATE");

        //TODO
    }

    // TODO test message setters? I think they should be set to protected just like the recipient setters.
    // set created, html body, text body, subject, sender, preferred locale, single bccs, reply to
    // TODO test regular filled message, including dispatch. a standard use test.
}
