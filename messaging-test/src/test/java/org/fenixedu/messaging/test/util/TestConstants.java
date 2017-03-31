package org.fenixedu.messaging.test.util;

import java.util.Locale;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.ui.access.SendersGroup;
import org.joda.time.Period;

public class TestConstants {

    private static TestConstants CONSTANTS;

    public final Group ANYBODY = Group.anyone(), NOBODY = Group.nobody(), MANAGERS = Group.managers(), SENDERS =
            SendersGroup.get();
    public final String FIRST_NAME = "John", LAST_NAME = "Doe", DISPLAY_NAME = "Dude", USER_EMAIL = "johndoe@gmail.com",
            MANAGER_EMAIL = MANAGERS.getMembers().findFirst().get().getEmail(), VALID_EMAIL = "tester@test.com", INVALID_EMAIL =
            "Ceci n'est pas une adresse";
    public final Locale ITALIAN = Locale.ITALIAN, CHINESE = Locale.CHINESE, DEFAULT_LOCALE = I18N.getLocale();
    public final LocalizedString LS_A = new LocalizedString(ITALIAN, FIRST_NAME), LS_AA = LS_A.with(DEFAULT_LOCALE, FIRST_NAME), LS_AB =
            LS_A.with(DEFAULT_LOCALE, LAST_NAME), LS_B = new LocalizedString(ITALIAN, LAST_NAME), LS_BB = LS_B.with(DEFAULT_LOCALE, LAST_NAME),
            LS_BC = LS_B.with(DEFAULT_LOCALE, DISPLAY_NAME), LS_C = new LocalizedString(ITALIAN, DISPLAY_NAME), LS_CA =
            LS_C.with(DEFAULT_LOCALE, FIRST_NAME), LS_CC = LS_C.with(DEFAULT_LOCALE, DISPLAY_NAME), LS_EMPTY = new LocalizedString();
    public static final Period PERIOD = Period.parse("P1Y");

    public final User USER = new User(new UserProfile(FIRST_NAME, LAST_NAME, DISPLAY_NAME, USER_EMAIL, null));

    private TestConstants() {
    }

    public static TestConstants get() {
        return CONSTANTS == null ? (CONSTANTS = new TestConstants()) : CONSTANTS;
    }

}
