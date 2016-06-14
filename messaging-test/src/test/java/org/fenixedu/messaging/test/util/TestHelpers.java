package org.fenixedu.messaging.test.util;

import jersey.repackaged.com.google.common.base.Supplier;
import jersey.repackaged.com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.Assert;

import static org.fenixedu.messaging.test.util.TestConstants.ADDR_A;
import static org.fenixedu.messaging.test.util.TestConstants.ADDR_B;
import static org.fenixedu.messaging.test.util.TestConstants.ADDR_C;
import static org.fenixedu.messaging.test.util.TestConstants.ADDR_INVALID;
import static org.fenixedu.messaging.test.util.TestConstants.GRP_A;
import static org.fenixedu.messaging.test.util.TestConstants.GRP_M;
import static org.fenixedu.messaging.test.util.TestConstants.GRP_N;
import static org.fenixedu.messaging.test.util.TestConstants.LOC_A;
import static org.fenixedu.messaging.test.util.TestConstants.LS_A;
import static org.fenixedu.messaging.test.util.TestConstants.LS_AA;
import static org.fenixedu.messaging.test.util.TestConstants.LS_AB;
import static org.fenixedu.messaging.test.util.TestConstants.LS_B;
import static org.fenixedu.messaging.test.util.TestConstants.LS_EMPTY;
import static org.fenixedu.messaging.test.util.TestConstants.STR_A;
import static org.fenixedu.messaging.test.util.TestConstants.STR_B;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestHelpers {

    private TestHelpers() {
    }

    ;

    public static void expectNullPointerException(Runnable r) {
        try {
            r.run();
            fail();
        } catch (NullPointerException e) {
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Array and Stream setters are additive
     * Collection setter always clears previous groups
     * Null and repeated (handled by Set) groups are ignored
     * Null containers throw NullPointerException
     */
    public static void testRecipientSetters(Consumer<Group[]> arraySetter, Consumer<Stream<Group>> streamSetter,
            Consumer<Collection<Group>> collectionSetter, Supplier<Set<Group>> getter) {
        Set<Group> groups;

        Group[] groupArray = { GRP_A, null, GRP_N };
        arraySetter.accept(groupArray);
        groups = getter.get();
        assertTrue(groups.size() == 2 && groups.contains(GRP_A) && groups.contains(GRP_N));

        streamSetter.accept(Stream.of(null, GRP_N, GRP_N, GRP_M));
        groups = getter.get();
        assertTrue(groups.size() == 3 && groups.contains(GRP_A) && groups.contains(GRP_N) && groups.contains(GRP_M));

        collectionSetter.accept(Lists.newArrayList(GRP_N, GRP_M, GRP_N, null));
        groups = getter.get();
        assertTrue(groups.size() == 2 && groups.contains(GRP_N) && groups.contains(GRP_M));

        expectNullPointerException(() -> arraySetter.accept(null));
        expectNullPointerException(() -> streamSetter.accept(null));
        expectNullPointerException(() -> collectionSetter.accept(null));
    }

    /**
     * Array and Stream setters are additive
     * Collection setter always clears previous addresses
     * Null, invalid and repeated (handled by Set) addresses are ignored
     * Null containers throw NullPointerException
     */
    public static void testAddressSetters(Consumer<String[]> arraySetter, Consumer<Stream<String>> streamSetter,
            Consumer<Collection<String>> collectionSetter, Supplier<Set<String>> getter) {
        Set<String> addresses;

        String[] groupArray = { ADDR_A, null, ADDR_B, ADDR_INVALID };
        arraySetter.accept(groupArray);
        addresses = getter.get();
        assertTrue(addresses.size() == 2 && addresses.contains(ADDR_A) && addresses.contains(ADDR_B));

        streamSetter.accept(Stream.of(null, ADDR_B, ADDR_INVALID, ADDR_B, ADDR_C));
        addresses = getter.get();
        assertTrue(
                addresses.size() == 3 && addresses.contains(ADDR_A) && addresses.contains(ADDR_B) && addresses.contains(ADDR_C));

        collectionSetter.accept(Lists.newArrayList(ADDR_B, ADDR_INVALID, ADDR_C, ADDR_B, null));
        addresses = getter.get();
        assertTrue(addresses.size() == 2 && addresses.contains(ADDR_B) && addresses.contains(ADDR_C));

        expectNullPointerException(() -> arraySetter.accept(null));
        expectNullPointerException(() -> streamSetter.accept(null));
        expectNullPointerException(() -> collectionSetter.accept(null));
    }

    /**
     * LocalizedString setter overrides current value
     * String setter is a shorthand for using the I18N default locale
     * String and locale setter overrides the locale content only
     * Null locale or Null LocalizedString throw NullPointerException
     * Null content erases the content of the locale
     */
    public static void testContentSetters(Consumer<String> stringSetter, BiConsumer<String, Locale> stringLocaleSetter,
            Consumer<LocalizedString> localizedStringSetter, Supplier<LocalizedString> getter) {
        localizedStringSetter.accept(LS_A);
        assertSame(getter.get(), LS_A);

        stringSetter.accept(STR_B);
        Assert.assertEquals(getter.get(), LS_AB);

        stringSetter.accept(STR_A);
        Assert.assertEquals(getter.get(), LS_AA);

        stringSetter.accept(null);
        Assert.assertEquals(getter.get(), LS_A);

        stringSetter.accept(null);
        Assert.assertEquals(getter.get(), LS_A);

        stringLocaleSetter.accept(null, LOC_A);
        Assert.assertEquals(getter.get(), LS_EMPTY);

        stringLocaleSetter.accept(null, LOC_A);
        Assert.assertEquals(getter.get(), LS_EMPTY);

        stringLocaleSetter.accept(STR_B, LOC_A);
        Assert.assertEquals(getter.get(), LS_B);

        stringLocaleSetter.accept(STR_A, LOC_A);
        Assert.assertEquals(getter.get(), LS_A);

        expectNullPointerException(() -> localizedStringSetter.accept(null));
        expectNullPointerException(() -> stringLocaleSetter.accept(STR_A, null));
    }

    public static void shortWait() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }
}
