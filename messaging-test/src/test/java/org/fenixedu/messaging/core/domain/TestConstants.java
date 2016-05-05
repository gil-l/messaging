package org.fenixedu.messaging.core.domain;

import java.util.Locale;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;

public class TestConstants {

    public static final User USER = new User(new UserProfile("John", "Doe", null, "johndoe@gmail.com", null));

    public static final Group GRP_A = Group.anyone(), GRP_B = Group.nobody(), GRP_C = Group.managers();

    public static final String STR_A = "test-a", STR_B = "test-b", STR_C = "test-c", ADDR_A = "testerA@test.com", ADDR_B =
            "testerB@test.com", ADDR_C = "testerC@test.com";

    public static final Locale LOC_A = new Locale("aa", "bb", "cc"), LOC_DEF = I18N.getLocale();

    public static final LocalizedString LS_A = new LocalizedString(LOC_A, STR_A), LS_AA = LS_A.with(LOC_DEF, STR_A), LS_AB =
            LS_A.with(LOC_DEF, STR_B), LS_B = new LocalizedString(LOC_A, STR_B), LS_BB = LS_B.with(LOC_DEF, STR_B), LS_BC =
            LS_B.with(LOC_DEF, STR_C), LS_C = new LocalizedString(LOC_A, STR_C), LS_CA = LS_C.with(LOC_DEF, STR_A), LS_CC =
            LS_C.with(LOC_DEF, STR_C), LS_EMPTY = new LocalizedString();
}
