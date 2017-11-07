package hudson.plugins.claim;

import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.model.*;
import hudson.security.ACL;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClaimGroovyTest {

    private static final String adminWithNoRunScriptRights = "adminWithNoRunScriptRights";
    private static final String adminWithAllRights = "adminWithAllRights";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
        realm.loadUserByUsername(adminWithNoRunScriptRights);
        j.jenkins.setSecurityRealm(realm);

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grantWithoutImplication(Jenkins.ADMINISTER, Jenkins.READ, Item.CREATE)
                .everywhere()
                .to(j.jenkins.getUser(adminWithNoRunScriptRights));
        strategy.grant(Jenkins.ADMINISTER, Jenkins.READ)
                .everywhere()
                .to(j.jenkins.getUser(adminWithAllRights));
        j.jenkins.setAuthorizationStrategy(strategy);

        ACL.impersonate(User.get(adminWithNoRunScriptRights).impersonate(), new Runnable() {
            @Override
            public void run() {
                assertTrue(j.jenkins.hasPermission(Jenkins.ADMINISTER));
                assertFalse(j.jenkins.hasPermission(Jenkins.RUN_SCRIPTS));
            }
        });
    }

    @Issue("JENKINS-43811")
    @Test
    public void userWithNoRunScriptsRightTest() throws Exception {
        doConfigureScriptWithUser(adminWithNoRunScriptRights);
        assertNull(j.jenkins.getSystemMessage());
    }

    @Issue("JENKINS-43811")
    @Test
    public void userWithRunScriptsRightTest() throws Exception {
        doConfigureScriptWithUser(adminWithAllRights);
        assertEquals("pwned", j.jenkins.getSystemMessage());
    }

    private void doConfigureScriptWithUser(String userName) throws InterruptedException, java.util.concurrent.ExecutionException {
        ACL.impersonate(User.get(userName).impersonate(), new Runnable() {
            @Override
            public void run() {
                try {
                    ClaimConfig config = (ClaimConfig) j.jenkins.getDescriptor(ClaimConfig.class);
                    config.setGroovyTrigger(new SecureGroovyScript("jenkins.model.Jenkins.instance.systemMessage = 'pwned'", false, null));
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });

        String configXml = "<project>  <builders>\n" +
                "    <hudson.tasks.Shell>\n" +
                "      <command>EXIT 1</command>\n" +
                "    </hudson.tasks.Shell>\n" +
                "  </builders>\n" +
                "  <publishers>\n" +
                "    <" + ClaimPublisher.class.getName() + " plugin=\"claim@1.2\"/>\n" +
                "  </publishers></project>";
        assertThat(new CLICommandInvoker(j, new CreateJobCommand()).asUser(adminWithNoRunScriptRights).withArgs("attack").withStdin(new ByteArrayInputStream(configXml.getBytes())).invoke(), CLICommandInvoker.Matcher.succeeded())
        ;
        final FreeStyleBuild build = j.jenkins.getItemByFullName("attack", FreeStyleProject.class).scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, build.getResult());

        ACL.impersonate(User.get(adminWithNoRunScriptRights).impersonate(), new Runnable() {
            @Override
            public void run() {
                try {
                    StaplerRequest req = mock(StaplerRequest.class);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.accumulate("assignee", adminWithNoRunScriptRights);
                    jsonObject.accumulate("reason", "none");
                    jsonObject.accumulate("errors", "");
                    jsonObject.accumulate("sticky", true);
                    when(req.getSubmittedForm()).thenReturn(jsonObject);

                    StaplerResponse res = mock(StaplerResponse.class);
                    ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
                    action.doClaim(req, res);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });
    }
}
