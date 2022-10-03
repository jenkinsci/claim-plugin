package hudson.plugins.claim;

import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClaimGroovyTest {

    private static final String ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS = "ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS";
    private static final String ADMIN_WITH_ALL_RIGHTS = "ADMIN_WITH_ALL_RIGHTS";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        realm.loadUserByUsername(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS);
        j.jenkins.setSecurityRealm(realm);

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        try {
            setUserCreationViaUrl(true);
            strategy.grantWithoutImplication(Jenkins.MANAGE, Jenkins.READ, Item.CREATE)
                    .everywhere()
                    .to(j.jenkins.getUser(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS));
            strategy.grant(Jenkins.ADMINISTER, Jenkins.READ)
                    .everywhere()
                    .to(j.jenkins.getUser(ADMIN_WITH_ALL_RIGHTS));
        } finally {
            setUserCreationViaUrl(false);
        }

        j.jenkins.setAuthorizationStrategy(strategy);

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS))) {
            assertTrue(j.jenkins.hasPermission(Jenkins.MANAGE));
            assertFalse(j.jenkins.hasPermission(Jenkins.ADMINISTER));
        };
    }

    /**
     * Allows setting the ALLOW_USER_CREATION_VIA_URL value on User object if the version of Jenkins requires it
     * To remove once base version for the plugin will have this property in favor of classic code.
     * @param value the value to set
     */
    private void setUserCreationViaUrl(boolean value) {
        try {
            Field field = User.class.getDeclaredField("ALLOW_USER_CREATION_VIA_URL");
            field.setBoolean(null, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // do nothing
        }
    }

    @Issue("JENKINS-43811")
    @Test
    public void userWithNoRunScriptsRightTest() throws Exception {
        doConfigureScriptWithUser(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS);
        assertNull(j.jenkins.getSystemMessage());
    }

    @Issue("JENKINS-43811")
    @Test
    public void userWithRunScriptsRightTest() throws Exception {
        doConfigureScriptWithUser(ADMIN_WITH_ALL_RIGHTS);
        assertEquals("pwned", j.jenkins.getSystemMessage());
    }

    private void doConfigureScriptWithUser(String userName)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(userName))) {
            try {
                ClaimConfig config = (ClaimConfig) j.jenkins.getDescriptor(ClaimConfig.class);
                config.setGroovyTrigger(new SecureGroovyScript(
                        "jenkins.model.Jenkins.instance.systemMessage = 'pwned'", false, null));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        String configXml = "<project>  <builders>\n"
                + "    <hudson.tasks.Shell>\n"
                + "      <command>EXIT 1</command>\n"
                + "    </hudson.tasks.Shell>\n"
                + "  </builders>\n"
                + "  <publishers>\n"
                + "    <" + ClaimPublisher.class.getName() + " plugin=\"claim@1.2\"/>\n"
                + "  </publishers></project>";
        assertThat(new CLICommandInvoker(j, new CreateJobCommand())
                .asUser(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS)
                .withArgs("attack")
                .withStdin(new ByteArrayInputStream(configXml.getBytes()))
                .invoke(),
                CLICommandInvoker.Matcher.succeeded());

        final FreeStyleBuild build = j.jenkins.getItemByFullName("attack", FreeStyleProject.class)
                .scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, build.getResult());

        try (ACLContext ctx = ACL.as(User.getOrCreateByIdOrFullName(ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS))) {
            try {
                StaplerRequest req = mock(StaplerRequest.class);
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("assignee", ADMIN_WITH_NO_RUN_SCRIPT_RIGHTS);
                jsonObject.accumulate("reason", "none");
                jsonObject.accumulate("errors", "");
                jsonObject.accumulate("sticky", true);
                jsonObject.accumulate("propagateToFollowingBuilds", false);
                when(req.getSubmittedForm()).thenReturn(jsonObject);

                StaplerResponse res = mock(StaplerResponse.class);
                ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
                action.doClaim(req, res);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }
}
