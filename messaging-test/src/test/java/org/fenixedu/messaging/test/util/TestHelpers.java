package org.fenixedu.messaging.test.util;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;

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

    public static <T> void testNullResistantSetAdder(Consumer<T[]> adder, T[] feed, Supplier<Set<T>> getter) {
        requireNonNull(adder);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> adder.accept(null));
        } else {
            Set<T> expectation = Stream.of(feed).filter(Objects::nonNull).collect(Collectors.toSet());
            expectation.addAll(getter.get());
            adder.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
    }

    public static <T> void testNullResistantSetAdder(Consumer<Stream<T>> adder, Stream<T> feed, Supplier<Set<T>> getter) {
        requireNonNull(adder);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> adder.accept(null));
        } else {
            Set<T> expectation = feed.filter(Objects::nonNull).collect(Collectors.toSet());
            expectation.addAll(getter.get());
            adder.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
    }

    public static <T> void testNullResistantSetAdder(Consumer<Collection<T>> adder, Collection<T> feed, Supplier<Set<T>> getter) {
        requireNonNull(adder);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> adder.accept(null));
        } else {
            Set<T> expectation = feed.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            expectation.addAll(getter.get());
            adder.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
    }

    public static <T> void testNullResistantSetSetter(Consumer<T[]> setter, T[] feed, Supplier<Set<T>> getter) {
        requireNonNull(setter);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> setter.accept(null));
        } else {
            Set<T> expectation = Stream.of(feed).filter(Objects::nonNull).collect(Collectors.toSet());
            setter.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
    }

    public static <T> void testNullResistantSetSetter(Consumer<Stream<T>> setter, Stream<T> feed, Supplier<Set<T>> getter) {
        requireNonNull(setter);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> setter.accept(null));
        } else {
            Set<T> expectation = feed.filter(Objects::nonNull).collect(Collectors.toSet());
            setter.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
    }

    public static <T> void testNullResistantSetSetter(Consumer<Collection<T>> setter, Collection<T> feed,
            Supplier<Set<T>> getter) {
        requireNonNull(setter);
        requireNonNull(getter);
        if (feed == null) {
            expectNullPointerException(() -> setter.accept(null));
        } else {
            Set<T> expectation = feed.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            setter.accept(feed);
            Set<T> result = getter.get();
            assertEquals(result, expectation);
        }
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
        localizedStringSetter.accept(TestConstants.get().LS_A);
        assertSame(getter.get(), TestConstants.get().LS_A);

        stringSetter.accept(TestConstants.get().LAST_NAME);
        assertEquals(getter.get(), TestConstants.get().LS_AB);

        stringSetter.accept(TestConstants.get().FIRST_NAME);
        assertEquals(getter.get(), TestConstants.get().LS_AA);

        stringSetter.accept(null);
        assertEquals(getter.get(), TestConstants.get().LS_A);

        stringSetter.accept(null);
        assertEquals(getter.get(), TestConstants.get().LS_A);

        stringLocaleSetter.accept(null, TestConstants.get().ITALIAN);
        assertEquals(getter.get(), TestConstants.get().LS_EMPTY);

        stringLocaleSetter.accept(null, TestConstants.get().ITALIAN);
        assertEquals(getter.get(), TestConstants.get().LS_EMPTY);

        stringLocaleSetter.accept(TestConstants.get().LAST_NAME, TestConstants.get().ITALIAN);
        assertEquals(getter.get(), TestConstants.get().LS_B);

        stringLocaleSetter.accept(TestConstants.get().FIRST_NAME, TestConstants.get().ITALIAN);
        assertEquals(getter.get(), TestConstants.get().LS_A);

        expectNullPointerException(() -> localizedStringSetter.accept(null));
        expectNullPointerException(() -> stringLocaleSetter.accept(TestConstants.get().FIRST_NAME, null));
    }

    public static void shortWait() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }
}
