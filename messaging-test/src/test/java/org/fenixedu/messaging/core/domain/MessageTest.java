package org.fenixedu.messaging.core.domain;

import jersey.repackaged.com.google.common.collect.Lists;
import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message.MessageBuilder;
import org.fenixedu.messaging.core.domain.Message.TemplateMessageBuilder;
import org.fenixedu.messaging.core.exception.MessagingDomainException;
import org.fenixedu.messaging.core.template.DeclareMessageTemplate;
import org.fenixedu.messaging.test.mock.MockDispatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@DeclareMessageTemplate(id = "TEST_TEMPLATE",
        subject = "Test template {{id}}",
        text = "This is test template {{id}}.\n{{message}}",
        html = "<h1>Test Template {{id}}</h1><p>{{message}}</p>")
@RunWith(FenixFrameworkRunner.class)
public class MessageTest {

    //TODO user mocking could be BeforeClass and AfterClass - unfortunately dispatcher setting cannot (?)
    //FIXME cannot test templates as ServletContainerInitializer is not run. Find alternative way.
    //FIXME Message deletion cannot be tested with current no repository backend

    @Before
    public void startup() {
        Authenticate.mock(TestConstants.USER);
        MessagingSystem.setMessageDispatcher(new MockDispatcher());
    }

    @After
    public void cleanup() {
        Authenticate.unmock();
        MessagingSystem.setMessageDispatcher(null);
    }

    /**
     * Builder throws when required sender parameter is null
     */
    @Test(expected = MessagingDomainException.class)
    public void nullSender() {
        Message.from(null).send();
    }

    /**
     * Builder provides default values enabling the straightforward creation of a valid default empty message
     */
    @Test
    public void emptyMessage() {
        Message message = Message.fromSystem().send();
        testEmptyMessage(message, MessagingSystem.systemSender());

        Sender sender = SenderTest.newRegularSender();
        message = Message.from(sender).send();
        testEmptyMessage(message, sender);
    }

    /**
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
     * `- Message sent date is updated once report's delivery finishes
     * XXX Delivery and report handling should be tested further within dispatcher modules
     * FIXME Date testing is now reduced to a null check. How can I mock/test the 'now' aspect?
     */
    private void testEmptyMessage(Message message, Sender sender) {
        assertNotNull(message);

        assertSame(message.getSender(), sender);
        assertSame(message.getUser(), TestConstants.USER);
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
        assertSame(message.getPreferredLocale(), TestConstants.LOC_DEF);
        assertNull(message.getReplyTo());

        MessageDispatchReport report = MessagingSystem.getInstance().dispatch(message);
        assertNotNull(report);
        assertSame(report, message.getDispatchReport());
        assertNotNull(message.getSent());
    }

    /**
     * Array and Stream setters are additive and ignore null
     * Collection setter always clears previous groups even on null
     * Null groups are filtered and repeated (handled by Set) groups are ignored
     */
    @Test
    public void messageBcc() {
        MessageBuilder builder = Message.fromSystem();
        Set<Group> groups;

        builder.bcc(TestConstants.GRP_A, null, TestConstants.GRP_B);
        groups = builder.send().getBccGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B));

        builder.bcc(Stream.of(null, TestConstants.GRP_B, TestConstants.GRP_B, TestConstants.GRP_C));
        groups = builder.send().getBccGroups();
        assertTrue(groups.size() == 3 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B) && groups
                .contains(TestConstants.GRP_C));

        builder.bcc(Lists.newArrayList(TestConstants.GRP_B, TestConstants.GRP_C, TestConstants.GRP_B, null));
        groups = builder.send().getBccGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.bcc((Group[]) null);
        groups = builder.send().getBccGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.bcc((Stream<Group>) null);
        groups = builder.send().getBccGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.bcc((Collection<Group>) null);
        groups = builder.send().getBccGroups();
        assertTrue(groups.isEmpty());
    }

    /**
     * Array and Stream setters are additive and ignore null
     * Collection setter always clears previous groups even on null
     * Null groups are filtered and repeated (handled by Set) groups are ignored
     */
    @Test
    public void messageCc() {
        MessageBuilder builder = Message.fromSystem();
        Set<Group> groups;

        builder.cc(TestConstants.GRP_A, null, TestConstants.GRP_B);
        groups = builder.send().getCcGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B));

        builder.cc(Stream.of(null, TestConstants.GRP_B, TestConstants.GRP_B, TestConstants.GRP_C));
        groups = builder.send().getCcGroups();
        assertTrue(groups.size() == 3 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B) && groups
                .contains(TestConstants.GRP_C));

        builder.cc(Lists.newArrayList(TestConstants.GRP_B, TestConstants.GRP_C, TestConstants.GRP_B, null));
        groups = builder.send().getCcGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.cc((Group[]) null);
        groups = builder.send().getCcGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.cc((Stream<Group>) null);
        groups = builder.send().getCcGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.cc((Collection<Group>) null);
        groups = builder.send().getCcGroups();
        assertTrue(groups.isEmpty());
    }

    /**
     * Array and Stream setters are additive and ignore null
     * Collection setter always clears previous groups even on null
     * Null groups are filtered and repeated (handled by Set) groups are ignored
     */
    @Test
    public void messageTo() {
        MessageBuilder builder = Message.fromSystem();
        Set<Group> groups;

        builder.to(TestConstants.GRP_A, null, TestConstants.GRP_B);
        groups = builder.send().getToGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B));

        builder.to(Stream.of(null, TestConstants.GRP_B, TestConstants.GRP_B, TestConstants.GRP_C));
        groups = builder.send().getToGroups();
        assertTrue(groups.size() == 3 && groups.contains(TestConstants.GRP_A) && groups.contains(TestConstants.GRP_B) && groups
                .contains(TestConstants.GRP_C));

        builder.to(Lists.newArrayList(TestConstants.GRP_B, TestConstants.GRP_C, TestConstants.GRP_B, null));
        groups = builder.send().getToGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.to((Group[]) null);
        groups = builder.send().getToGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.to((Stream<Group>) null);
        groups = builder.send().getToGroups();
        assertTrue(groups.size() == 2 && groups.contains(TestConstants.GRP_B) && groups.contains(TestConstants.GRP_C));

        builder.to((Collection<Group>) null);
        groups = builder.send().getToGroups();
        assertTrue(groups.isEmpty());
    }

    /**
     * Array and Stream setters are additive and ignore null
     * Collection setter always clears previous addresses even on null
     * Null addresses are filtered, invalid and repeated (handled by Set) addresses are ignored
     */
    @Test
    public void messageSingleBcc() {
        MessageBuilder builder = Message.fromSystem();
        Set<String> emails;

        builder.singleBcc(TestConstants.ADDR_A, null, TestConstants.ADDR_B);
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.size() == 2 && emails.contains(TestConstants.ADDR_A) && emails.contains(TestConstants.ADDR_B));

        builder.singleBcc(Stream.of(null, TestConstants.ADDR_B, TestConstants.ADDR_B, TestConstants.ADDR_C));
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.size() == 3 && emails.contains(TestConstants.ADDR_A) && emails.contains(TestConstants.ADDR_B) && emails
                .contains(TestConstants.ADDR_C));

        builder.singleBcc(Lists.newArrayList(TestConstants.ADDR_B, TestConstants.ADDR_C, TestConstants.ADDR_B, null));
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.size() == 2 && emails.contains(TestConstants.ADDR_B) && emails.contains(TestConstants.ADDR_C));

        builder.singleBcc((String[]) null);
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.size() == 2 && emails.contains(TestConstants.ADDR_B) && emails.contains(TestConstants.ADDR_C));

        builder.singleBcc((Stream<String>) null);
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.size() == 2 && emails.contains(TestConstants.ADDR_B) && emails.contains(TestConstants.ADDR_C));

        builder.singleBcc((Collection<String>) null);
        emails = builder.send().getSingleBccsSet();
        assertTrue(emails.isEmpty());
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

        builder.content(TestConstants.LS_A, TestConstants.LS_B, TestConstants.LS_C);
        message = builder.send();
        assertSame(message.getSubject(), TestConstants.LS_A);
        assertSame(message.getTextBody(), TestConstants.LS_B);
        assertSame(message.getHtmlBody(), TestConstants.LS_C);

        builder.content(TestConstants.STR_A, TestConstants.STR_B, TestConstants.STR_C);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_AA);
        assertEquals(message.getTextBody(), TestConstants.LS_BB);
        assertEquals(message.getHtmlBody(), TestConstants.LS_CC);

        builder.content(TestConstants.STR_B, TestConstants.STR_C, TestConstants.STR_A);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_AB);
        assertEquals(message.getTextBody(), TestConstants.LS_BC);
        assertEquals(message.getHtmlBody(), TestConstants.LS_CA);

        builder.content((LocalizedString) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_EMPTY);
        assertEquals(message.getTextBody(), TestConstants.LS_EMPTY);
        assertEquals(message.getHtmlBody(), TestConstants.LS_EMPTY);

        builder.content(TestConstants.STR_C, TestConstants.STR_A, TestConstants.STR_B, TestConstants.LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_C);
        assertEquals(message.getTextBody(), TestConstants.LS_A);
        assertEquals(message.getHtmlBody(), TestConstants.LS_B);

        builder.content(TestConstants.STR_A, TestConstants.STR_B, TestConstants.STR_C, TestConstants.LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_A);
        assertEquals(message.getTextBody(), TestConstants.LS_B);
        assertEquals(message.getHtmlBody(), TestConstants.LS_C);

        builder.content(TestConstants.LS_AA, TestConstants.LS_BB, TestConstants.LS_CC);
        message = builder.send();
        assertSame(message.getSubject(), TestConstants.LS_AA);
        assertSame(message.getTextBody(), TestConstants.LS_BB);
        assertSame(message.getHtmlBody(), TestConstants.LS_CC);

        builder.content((String) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_A);
        assertEquals(message.getTextBody(), TestConstants.LS_B);
        assertEquals(message.getHtmlBody(), TestConstants.LS_C);

        builder.content(TestConstants.STR_A, TestConstants.STR_A, TestConstants.STR_A, null);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_A);
        assertEquals(message.getTextBody(), TestConstants.LS_B);
        assertEquals(message.getHtmlBody(), TestConstants.LS_C);

        builder.content(null, null, null, TestConstants.LOC_A);
        message = builder.send();
        assertEquals(message.getSubject(), TestConstants.LS_EMPTY);
        assertEquals(message.getTextBody(), TestConstants.LS_EMPTY);
        assertEquals(message.getHtmlBody(), TestConstants.LS_EMPTY);

    }

    /**
     * LocalizedString setter overrides current value
     * String setter is a shorthand for using the I18N default locale
     * String and locale setter overrides the locale content only
     * Null locale is ignored
     * Null content erases the content of the locale
     * Null LocalizedString sets and empty LocalizedString
     */
    @Test
    public void messageSubject() {
        MessageBuilder builder = Message.fromSystem();

        builder.subject(TestConstants.LS_A);
        assertSame(builder.send().getSubject(), TestConstants.LS_A);

        builder.subject(TestConstants.STR_A);
        assertEquals(builder.send().getSubject(), TestConstants.LS_AA);

        builder.subject(TestConstants.STR_B);
        assertEquals(builder.send().getSubject(), TestConstants.LS_AB);

        builder.subject((LocalizedString) null);
        assertEquals(builder.send().getSubject(), TestConstants.LS_EMPTY);

        builder.subject(TestConstants.STR_B, TestConstants.LOC_A);
        assertEquals(builder.send().getSubject(), TestConstants.LS_B);

        builder.subject(TestConstants.STR_A, TestConstants.LOC_A);
        assertEquals(builder.send().getSubject(), TestConstants.LS_A);

        builder.subject(TestConstants.LS_AA);
        assertSame(builder.send().getSubject(), TestConstants.LS_AA);

        builder.subject((String) null);
        assertEquals(builder.send().getSubject(), TestConstants.LS_A);

        builder.subject(TestConstants.STR_A, null);
        assertEquals(builder.send().getSubject(), TestConstants.LS_A);

        builder.subject(null, TestConstants.LOC_A);
        assertEquals(builder.send().getSubject(), TestConstants.LS_EMPTY);
    }

    /**
     * LocalizedString setter overrides current value
     * String setter is a shorthand for using the I18N default locale
     * String and locale setter overrides the locale content only
     * Null locale is ignored
     * Null content erases the content of the locale
     * Null LocalizedString sets and empty LocalizedString
     */
    @Test
    public void messageTextBody() {
        MessageBuilder builder = Message.fromSystem();

        builder.textBody(TestConstants.LS_A);
        assertSame(builder.send().getTextBody(), TestConstants.LS_A);

        builder.textBody(TestConstants.STR_A);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_AA);

        builder.textBody(TestConstants.STR_B);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_AB);

        builder.textBody((LocalizedString) null);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_EMPTY);

        builder.textBody(TestConstants.STR_B, TestConstants.LOC_A);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_B);

        builder.textBody(TestConstants.STR_A, TestConstants.LOC_A);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_A);

        builder.textBody(TestConstants.LS_AA);
        assertSame(builder.send().getTextBody(), TestConstants.LS_AA);

        builder.textBody((String) null);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_A);

        builder.textBody(TestConstants.STR_A, null);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_A);

        builder.textBody(null, TestConstants.LOC_A);
        assertEquals(builder.send().getTextBody(), TestConstants.LS_EMPTY);
    }

    /**
     * LocalizedString setter overrides current value
     * String setter is a shorthand for using the I18N default locale
     * String and locale setter overrides the locale content only
     * Null locale is ignored
     * Null content erases the content of the locale
     * Null LocalizedString sets and empty LocalizedString
     */
    @Test
    public void messageHtmlBody() {
        MessageBuilder builder = Message.fromSystem();

        builder.htmlBody(TestConstants.LS_A);
        assertSame(builder.send().getHtmlBody(), TestConstants.LS_A);

        builder.htmlBody(TestConstants.STR_A);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_AA);

        builder.htmlBody(TestConstants.STR_B);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_AB);

        builder.htmlBody((LocalizedString) null);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_EMPTY);

        builder.htmlBody(TestConstants.STR_B, TestConstants.LOC_A);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_B);

        builder.htmlBody(TestConstants.STR_A, TestConstants.LOC_A);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_A);

        builder.htmlBody(TestConstants.LS_AA);
        assertSame(builder.send().getHtmlBody(), TestConstants.LS_AA);

        builder.htmlBody((String) null);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_A);

        builder.htmlBody(TestConstants.STR_A, null);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_A);

        builder.htmlBody(null, TestConstants.LOC_A);
        assertEquals(builder.send().getHtmlBody(), TestConstants.LS_EMPTY);
    }

    /**
     * Sets reply to address to provided address
     * Reply to sender setter copies sender's default reply to address
     * Invalid addresses set null reply to
     */
    @Test
    public void messageReplyTo() {
        Sender sender = SenderTest.newRegularSender();
        MessageBuilder builder = Message.from(sender);

        builder.replyTo(null);
        assertNull(builder.send().getReplyTo());

        builder.replyTo(TestConstants.ADDR_A);
        assertEquals(builder.send().getReplyTo(), TestConstants.ADDR_A);

        builder.replyToSender();
        assertEquals(builder.send().getReplyTo(), sender.getReplyTo());

        builder.replyTo("not really a valid address...");
        assertNull(builder.send().getReplyTo());

    }

    /**
     * Sets preferred locale to provided locale
     * Null locale sets I18N default locale
     */
    @Test
    public void messagePreferredLocale() {
        MessageBuilder builder = Message.fromSystem();

        builder.preferredLocale(TestConstants.LOC_A);
        assertEquals(builder.send().getPreferredLocale(), TestConstants.LOC_A);

        builder.preferredLocale(null);
        assertEquals(builder.send().getPreferredLocale(), TestConstants.LOC_DEF);

        builder.preferredLocale(TestConstants.LOC_A);
        assertEquals(builder.send().getPreferredLocale(), TestConstants.LOC_A);
    }

    /**
     * returns locales for which there is message content
     */
    @Test
    public void messageContentLocales() {
        MessageBuilder builder = Message.fromSystem();
        assertTrue(builder.send().getContentLocales().isEmpty());

        builder.content(TestConstants.STR_A, TestConstants.STR_B, TestConstants.STR_C);
        Set<Locale> locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(TestConstants.LOC_DEF));

        builder.textBody(TestConstants.STR_B, TestConstants.LOC_A);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(TestConstants.LOC_DEF) && locales.contains(TestConstants.LOC_A));

        builder.htmlBody(TestConstants.STR_C, Locale.CHINA);
        locales = builder.send().getContentLocales();
        assertTrue(
                locales.size() == 3 && locales.contains(TestConstants.LOC_DEF) && locales.contains(TestConstants.LOC_A) && locales
                        .contains(Locale.CHINA));

        builder.htmlBody(null, Locale.CHINA);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(TestConstants.LOC_DEF) && locales.contains(TestConstants.LOC_A));

        builder.textBody(null, TestConstants.LOC_A);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(TestConstants.LOC_DEF));

        builder.subject((String) null);
        builder.textBody((String) null);
        builder.htmlBody((String) null);
        assertTrue(builder.send().getContentLocales().isEmpty());
    }

    /**
     * Messages are ordered by descending creation date
     */
    @Test
    public void messageOrder() {
        Message mC = Message.fromSystem().send();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        Message mB = Message.fromSystem().send();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        Message mA = Message.fromSystem().send();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
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
