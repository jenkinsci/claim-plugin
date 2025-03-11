package hudson.plugins.claim;

import hudson.model.User;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class ClaimEmailPreferenceTest {

    @Test
    void testDefaultEmailPreferences(JenkinsRule jenkins) {
        User user = User.getOrCreateByIdOrFullName("testuser");

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull(preference, "User property should not be null");
        assertTrue(preference.isReceiveInitialBuildClaimEmail(), "Default initial build claim email should be true");
        assertTrue(preference.isReceiveInitialTestClaimEmail(), "Default initial test claim email should be true");
        assertTrue(preference.isReceiveRepeatedBuildClaimEmail(), "Default repeated build claim email should be true");
        assertTrue(preference.isReceiveRepeatedTestClaimEmail(), "Default repeated test claim email should be true");
    }

    @Test
    void testCustomEmailPreferences(JenkinsRule jenkins) throws Exception {
        User user = User.getOrCreateByIdOrFullName("testuser");

        ClaimEmailPreference customPreference = new ClaimEmailPreference(true, false, true, false);
        user.addProperty(customPreference);

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull(preference, "ClaimEmailPreference should be set");
        assertTrue(preference.isReceiveInitialBuildClaimEmail(), "User should receive initial build claim emails");
        assertFalse(preference.isReceiveInitialTestClaimEmail(), "User should NOT receive initial test claim emails");
        assertTrue(preference.isReceiveRepeatedBuildClaimEmail(), "User should receive repeated build claim emails");
        assertFalse(preference.isReceiveRepeatedTestClaimEmail(), "User should NOT receive repeated test claim emails");
    }

    @Test
    void testUpdateEmailPreferences(JenkinsRule jenkins) throws Exception {
        User user = User.getOrCreateByIdOrFullName("testuser");

        user.addProperty(new ClaimEmailPreference(false, false, false, false));
        ClaimEmailPreference pref1 = user.getProperty(ClaimEmailPreference.class);
        assertFalse(pref1.isReceiveInitialBuildClaimEmail());
        assertFalse(pref1.isReceiveInitialTestClaimEmail());
        assertFalse(pref1.isReceiveRepeatedBuildClaimEmail());
        assertFalse(pref1.isReceiveRepeatedTestClaimEmail());

        user.addProperty(new ClaimEmailPreference(true, true, true, true));
        ClaimEmailPreference pref2 = user.getProperty(ClaimEmailPreference.class);
        assertTrue(pref2.isReceiveInitialBuildClaimEmail());
        assertTrue(pref2.isReceiveInitialTestClaimEmail());
        assertTrue(pref2.isReceiveRepeatedBuildClaimEmail());
        assertTrue(pref2.isReceiveRepeatedTestClaimEmail());
    }

    @Test
    void testUserWithoutPreferences(JenkinsRule jenkins) {
        User user = User.getOrCreateByIdOrFullName("newuser");

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull(preference, "Preference should not be null");
        assertTrue(preference.isReceiveInitialBuildClaimEmail(), "Default initial build claim email should be true");
        assertTrue(preference.isReceiveInitialTestClaimEmail(), "Default initial test claim email should be true");
        assertTrue(preference.isReceiveRepeatedBuildClaimEmail(), "Default repeated build claim email should be true");
        assertTrue(preference.isReceiveRepeatedTestClaimEmail(), "Default repeated test claim email should be true");
    }

    @Test
    void testNullUserPreferences(JenkinsRule jenkins) {
        User user = User.getOrCreateByIdOrFullName("noPreferencesUser");

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull(preference, "Preference should not be null");
        assertTrue(preference.isReceiveInitialBuildClaimEmail(), "Default initial build claim email should be true");
        assertTrue(preference.isReceiveInitialTestClaimEmail(), "Default initial test claim email should be true");
        assertTrue(preference.isReceiveRepeatedBuildClaimEmail(), "Default repeated build claim email should be true");
        assertTrue(preference.isReceiveRepeatedTestClaimEmail(), "Default repeated test claim email should be true");
    }
}
