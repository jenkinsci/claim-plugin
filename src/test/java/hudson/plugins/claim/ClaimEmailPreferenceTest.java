package hudson.plugins.claim;

import hudson.model.User;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class ClaimEmailPreferenceTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testDefaultEmailPreferences() {
        User user = User.getOrCreateByIdOrFullName("testuser");

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull("User property should not be null", preference);
        assertTrue("Default initial build claim email should be true", preference.isReceiveInitialBuildClaimEmail());
        assertTrue("Default initial test claim email should be true", preference.isReceiveInitialTestClaimEmail());
        assertTrue("Default repeated build claim email should be true", preference.isReceiveRepeatedBuildClaimEmail());
        assertTrue("Default repeated test claim email should be true", preference.isReceiveRepeatedTestClaimEmail());
    }

    @Test
    public void testCustomEmailPreferences() throws Exception {
        User user = User.getOrCreateByIdOrFullName("testuser");

        ClaimEmailPreference customPreference = new ClaimEmailPreference(true, false, true, false);
        user.addProperty(customPreference);

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull("ClaimEmailPreference should be set", preference);
        assertTrue("User should receive initial build claim emails", preference.isReceiveInitialBuildClaimEmail());
        assertFalse("User should NOT receive initial test claim emails", preference.isReceiveInitialTestClaimEmail());
        assertTrue("User should receive repeated build claim emails", preference.isReceiveRepeatedBuildClaimEmail());
        assertFalse("User should NOT receive repeated test claim emails", preference.isReceiveRepeatedTestClaimEmail());
    }

    @Test
    public void testUpdateEmailPreferences() throws Exception {
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
    public void testUserWithoutPreferences() {
        User user = User.getOrCreateByIdOrFullName("newuser");

        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);

        assertNotNull("Preference should not be null", preference);
        assertTrue("Default initial build claim email should be true", preference.isReceiveInitialBuildClaimEmail());
        assertTrue("Default initial test claim email should be true", preference.isReceiveInitialTestClaimEmail());
        assertTrue("Default repeated build claim email should be true", preference.isReceiveRepeatedBuildClaimEmail());
        assertTrue("Default repeated test claim email should be true", preference.isReceiveRepeatedTestClaimEmail());
    }
}
