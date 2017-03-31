package org.fenixedu.messaging.core.domain;

import static org.fenixedu.messaging.test.util.TestHelpers.expectNullPointerException;
import static org.fenixedu.messaging.test.util.TestHelpers.shortWait;
import static org.fenixedu.messaging.test.util.TestHelpers.testContentSetters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message.MessageBuilder;
import org.fenixedu.messaging.core.domain.Message.TemplateMessageBuilder;
import org.fenixedu.messaging.core.exception.MessagingDomainException;
import org.fenixedu.messaging.test.mock.MockDispatcher;
import org.fenixedu.messaging.test.util.TestConstants;
import org.fenixedu.messaging.test.util.TestHelpers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class MessageTest {

    private final TestConstants constants;

    public MessageTest() {
        constants = TestConstants.get();
    }

    //FIXME cannot test templates as ServletContainerInitializer is not run. Find alternative way.
    //XXX Message deletion cannot be tested with current no repository backend
    //XXX user mocking and dispatcher setting cannot be done in class startup/cleanup as they access domain objects
    //TODO ensure Message#fromSystem produces regular builder (we use it in most tests) [PROBLEM: can't inspect builder state]
    //TODO separate message dispatch tests
    //TODO test message skipping
    //TODO test fromsystem is same as from system sender
    //XXX all setter tests test builder reusability too
    //TODO test relation/priority between recipient types in message sending (independent lists in message object, defined
    // order in proper email)

    @Before
    public void startup() {
        Authenticate.mock(constants.USER);
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().getMillis());
    }

    @After
    public void cleanup() {
        Authenticate.unmock();
        DateTimeUtils.setCurrentMillisSystem();
    }

    /**
     * {@link Message#from } throws NullPointerException on null
     */
    @Test(expected = MessagingDomainException.class)
    public void nullSender() {
        Message.from(null).send();
    }

    /**
     * {@link MessageBuilder } provides default values enabling the straightforward creation of a valid default empty message:
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
     */
    @Test
    public void emptyMessage() {
        Sender sender = SenderTest.testSender();
        Message message = Message.from(sender).send();
        assertNotNull(message);

        assertSame(message.getSender(), sender);
        assertSame(message.getCreator(), TestConstants.get().USER);
        assertEquals(message.getCreated(), DateTime.now());
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
        assertSame(message.getPreferredLocale(), constants.DEFAULT_LOCALE);
        assertNull(message.getReplyTo());

        MessagingSystem.setMessageDispatcher(MockDispatcher.sync);
        MessageDispatchReport report = MessagingSystem.dispatch(message);
        assertNotNull(report);
        assertFalse(MessagingSystem.getPendingMessages().contains(message));
        assertSame(report, message.getDispatchReport());
        assertNotNull(message.getSent());
        MessagingSystem.setMessageDispatcher(null);
    }

    /**
     * {@link MessageBuilder#from } overwrites initially supplied sender
     */
    @Test
    public void messageFrom() {
        MessageBuilder builder = Message.fromSystem();
        Sender s = SenderTest.testSender();
        assertSame(builder.from(s).send().getSender(), s);
    }

    /**
     * {@link MessageBuilder#from } throws NullPointerException on null
     */
    @Test(expected = NullPointerException.class)
    public void messageFromNull() {
        MessageBuilder builder = Message.fromSystem();
        builder.from(null);
    }

    /**
     * {@link MessageBuilder}'s {@link MessageBuilder#bcc(Stream) Stream} and {@link MessageBuilder#bcc(Group...) Array} bcc
     * methods are adders and ignore nulls among provided Groups.
     * {@link MessageBuilder}'s {@link MessageBuilder#bcc(Collection) Collection} bcc method is a setter and ignores nulls
     * among provided Groups.
     * Recipient and address lists in {@link Message} and {@link MessageBuilder} objects are independent of each other and
     * indifferent to group members and address validity.
     */
    @Test
    public void messageRecipients() {
        MessageBuilder builder = Message.fromSystem();

        Supplier<Set<Group>> bccGetter = () -> builder.send().getBccGroups();
        Supplier<Set<Group>> toGetter = () -> builder.send().getToGroups();
        Supplier<Set<Group>> ccGetter = () -> builder.send().getCcGroups();
        Supplier<Set<String>> singleBccGetter = () -> builder.send().getSingleBccsSet();

        Group[] recipientArray = { null, constants.ANYBODY, constants.NOBODY };
        Collection<Group> recipientCollection = Arrays.asList(constants.NOBODY, constants.MANAGERS, constants.NOBODY, null);
        Stream<Group> recipientStream = recipientCollection.stream();
        String[] addressArray = { null, constants.INVALID_EMAIL, constants.MANAGER_EMAIL };
        Collection<String> addressCollection =
                Arrays.asList(constants.USER_EMAIL, constants.USER_EMAIL, constants.MANAGER_EMAIL, null);
        Stream<String> addressStream = addressCollection.stream();

        TestHelpers.testNullResistantSetAdder(builder::to, recipientArray, toGetter);
        TestHelpers.testNullResistantSetAdder(builder::to, recipientStream, toGetter);
        TestHelpers.testNullResistantSetSetter(builder::to, recipientCollection, toGetter);

        TestHelpers.testNullResistantSetAdder(builder::cc, recipientArray, ccGetter);
        TestHelpers.testNullResistantSetAdder(builder::cc, recipientStream, ccGetter);
        TestHelpers.testNullResistantSetSetter(builder::cc, recipientCollection, ccGetter);

        TestHelpers.testNullResistantSetAdder(builder::bcc, recipientArray, bccGetter);
        TestHelpers.testNullResistantSetAdder(builder::bcc, recipientStream, bccGetter);
        TestHelpers.testNullResistantSetSetter(builder::bcc, recipientCollection, bccGetter);

        TestHelpers.testNullResistantSetAdder(builder::singleBcc, addressArray, singleBccGetter);
        TestHelpers.testNullResistantSetAdder(builder::singleBcc, addressStream, singleBccGetter);
        TestHelpers.testNullResistantSetSetter(builder::singleBcc, addressCollection, singleBccGetter);

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

        builder.content(constants.LS_A, constants.LS_B, constants.LS_C);
        message = builder.send();
        assertSame(message.getSubject(), constants.LS_A);
        assertSame(message.getTextBody(), constants.LS_B);
        assertSame(message.getHtmlBody(), constants.LS_C);

        builder.content(constants.FIRST_NAME, constants.LAST_NAME, constants.DISPLAY_NAME);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_AA);
        assertEquals(message.getTextBody(), constants.LS_BB);
        assertEquals(message.getHtmlBody(), constants.LS_CC);

        builder.content(constants.LAST_NAME, constants.DISPLAY_NAME, constants.FIRST_NAME);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_AB);
        assertEquals(message.getTextBody(), constants.LS_BC);
        assertEquals(message.getHtmlBody(), constants.LS_CA);

        builder.content((String) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_A);
        assertEquals(message.getTextBody(), constants.LS_B);
        assertEquals(message.getHtmlBody(), constants.LS_C);

        builder.content((String) null, null, null);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_A);
        assertEquals(message.getTextBody(), constants.LS_B);
        assertEquals(message.getHtmlBody(), constants.LS_C);

        builder.content(null, null, null, constants.ITALIAN);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_EMPTY);
        assertEquals(message.getTextBody(), constants.LS_EMPTY);
        assertEquals(message.getHtmlBody(), constants.LS_EMPTY);

        builder.content(null, null, null, constants.ITALIAN);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_EMPTY);
        assertEquals(message.getTextBody(), constants.LS_EMPTY);
        assertEquals(message.getHtmlBody(), constants.LS_EMPTY);

        builder.content(constants.DISPLAY_NAME, constants.FIRST_NAME, constants.LAST_NAME, constants.ITALIAN);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_C);
        assertEquals(message.getTextBody(), constants.LS_A);
        assertEquals(message.getHtmlBody(), constants.LS_B);

        builder.content(constants.FIRST_NAME, constants.LAST_NAME, constants.DISPLAY_NAME, constants.ITALIAN);
        message = builder.send();
        assertEquals(message.getSubject(), constants.LS_A);
        assertEquals(message.getTextBody(), constants.LS_B);
        assertEquals(message.getHtmlBody(), constants.LS_C);

        expectNullPointerException(() -> builder.content(null, constants.LS_A, constants.LS_B));
        expectNullPointerException(() -> builder.content(constants.LS_A, null, constants.LS_B));
        expectNullPointerException(() -> builder.content(constants.LS_A, constants.LS_B, null));
        expectNullPointerException(() -> builder.content(constants.FIRST_NAME, constants.FIRST_NAME, constants.FIRST_NAME, null));
    }

    /**
     * returns locales for which there is message content
     */
    @Test
    public void messageContentLocales() {
        MessageBuilder builder = Message.fromSystem();
        assertTrue(builder.send().getContentLocales().isEmpty());

        builder.content(constants.FIRST_NAME, constants.LAST_NAME, constants.DISPLAY_NAME);
        Set<Locale> locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(constants.DEFAULT_LOCALE));

        builder.textBody(constants.LAST_NAME, constants.ITALIAN);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(constants.DEFAULT_LOCALE) && locales.contains(constants.ITALIAN));

        builder.htmlBody(constants.DISPLAY_NAME, constants.CHINESE);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 3 && locales.contains(constants.DEFAULT_LOCALE) && locales.contains(constants.ITALIAN) && locales.contains(constants.CHINESE));

        builder.htmlBody(null, constants.CHINESE);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 2 && locales.contains(constants.DEFAULT_LOCALE) && locales.contains(constants.ITALIAN));

        builder.textBody(null, constants.ITALIAN);
        locales = builder.send().getContentLocales();
        assertTrue(locales.size() == 1 && locales.contains(constants.DEFAULT_LOCALE));

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

        builder.replyTo(constants.USER_EMAIL);
        assertEquals(builder.send().getReplyTo(), constants.USER_EMAIL);

        builder.replyToSender();
        assertEquals(builder.send().getReplyTo(), sender.getReplyTo());

        builder.replyTo(constants.INVALID_EMAIL);
        assertNull(builder.send().getReplyTo());

    }

    /**
     * Sets preferred locale to provided locale
     * Null Locale throws NullPointerException
     */
    @Test
    public void messagePreferredLocale() {
        MessageBuilder builder = Message.fromSystem();

        builder.preferredLocale(constants.ITALIAN);
        assertEquals(builder.send().getPreferredLocale(), constants.ITALIAN);

        builder.preferredLocale(constants.CHINESE);
        assertEquals(builder.send().getPreferredLocale(), constants.CHINESE);

        expectNullPointerException(() -> builder.preferredLocale(constants.CHINESE));
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
