package hudson.plugins.claim;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.model.*;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClaimGroovyTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Before
  public void setUp() throws Exception {
    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

    JenkinsRule.DummySecurityRealm realm = j.createDummySecurityRealm();
    realm.loadUserByUsername("user0");
    j.jenkins.setSecurityRealm(realm);

    MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
    strategy.grantWithoutImplication(Jenkins.ADMINISTER, Jenkins.READ)
        .everywhere()
        .to(j.jenkins.getUser("user0"));
    j.jenkins.setAuthorizationStrategy(strategy);

    ACL.impersonate(User.get("user0").impersonate(), new Runnable() {
      @Override
      public void run() {
        assertTrue(j.jenkins.hasPermission(Jenkins.ADMINISTER));
        assertFalse(j.jenkins.hasPermission(Jenkins.RUN_SCRIPTS));
      }
    });
  }

  @Issue("JENKINS-43811")
  @Test
  public void scriptSecurity() throws Exception {
    ACL.impersonate(User.get("user0").impersonate(), new Runnable() {
      @Override
      public void run() {
        ClaimConfig config = (ClaimConfig) j.jenkins.getDescriptor(ClaimConfig.class);
        config.setGroovyScript("jenkins.model.Jenkins.instance.systemMessage = 'pwned'");
      }
    });

    String configXml = "<project>  <builders>\n" +
        "    <hudson.tasks.Shell>\n" +
        "      <command>EXIT 1</command>\n" +
        "    </hudson.tasks.Shell>\n" +
        "  </builders>\n" +
        "  <publishers>\n" +
        "    <"+ClaimPublisher.class.getName() + " plugin=\"claim@1.2\"/>\n" +
        "  </publishers></project>";
    assertThat(new CLICommandInvoker(j, new CreateJobCommand()).authorizedTo(Jenkins.READ, Item.CREATE).withArgs("attack").withStdin(new ByteArrayInputStream(configXml.getBytes())).invoke(), CLICommandInvoker.Matcher.succeeded());
    final FreeStyleBuild build = j.jenkins.getItemByFullName("attack", FreeStyleProject.class).scheduleBuild2(0).get();
    assertEquals(Result.FAILURE, build.getResult());

    ACL.impersonate(User.get("user0").impersonate(), new Runnable() {
      @Override
      public void run() {
        try {
          StaplerRequest req = mock(StaplerRequest.class);
          JSONObject jsonObject = new JSONObject();
          jsonObject.accumulate("assignee", "user0");
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



    assertNull(j.jenkins.getSystemMessage());
  }
}
