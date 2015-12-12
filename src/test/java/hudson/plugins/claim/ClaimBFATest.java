package hudson.plugins.claim;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import hudson.model.Build;
import hudson.model.Project;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.HashSet;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;

public class ClaimBFATest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private Build<?, ?> build;
    private Project<?, ?> project;

    private static final String CAUSE_NAME_1 = "Cause1";
    private static final String CAUSE_NAME_2 = "Cause2";
    private static final String CAUSE_DESCRIPTION_1 = "DescriptionForCause1";
    private static final String CAUSE_DESCRIPTION_2 = "DescriptionForCause2";
    private static final String IDENTIFIED_PROBLEMS = "Identified problems";
    private static final String REASON = "Test Reason";


    @Before
    public void setUp() throws Exception {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        createKnowledgeBase();
        project = j.createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();
    }

    @Test
    public void canClaimFailureCause() throws Exception {
        ClaimBuildAction action = applyClaimWithFailureCauseSelected("claim",CAUSE_NAME_2, REASON, CAUSE_DESCRIPTION_2);

        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(REASON));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.isBFAEnabled(), is(true));

        HtmlPage page = whenNavigatingtoClaimPage();
        assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
        assertTrue(page.asXml().contains(CAUSE_NAME_2));
    }

    @Test
    public void canReclaimFailureCause() throws Exception{
        applyClaimWithFailureCauseSelected("claim", CAUSE_NAME_2, REASON, CAUSE_DESCRIPTION_2);
        ClaimBuildAction action = applyClaimWithFailureCauseSelected("reassign", CAUSE_NAME_1, REASON, CAUSE_DESCRIPTION_1);

        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(REASON));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.isBFAEnabled(), is(true));

        HtmlPage page = whenNavigatingtoClaimPage();
        assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
        assertTrue(page.asXml().contains(CAUSE_NAME_1));
    }

    @Test
    public void canDropFailureCause() throws Exception{
        ClaimBuildAction action = applyClaimWithFailureCauseSelected("claim", CAUSE_NAME_2, REASON, CAUSE_DESCRIPTION_2);

        HtmlPage page = whenNavigatingtoClaimPage();
        page.getElementById("dropClaim").click();
        page = whenNavigatingtoClaimPage();
        assertThat(action.isClaimed(), is(false));
        assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
        assertTrue(page.asXml().contains("No identified problem"));
    }

    @Test
    public void testCreateKnowledgeBase() throws Exception {
        createKnowledgeBase();
        assertNotNull(PluginImpl.getInstance().getKnowledgeBase().getCauses());
    }

    @Test
    public void errorDropdownIsPresentAndIsNotEmpty() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("job/x/" + build.getNumber());
        page.getElementById("claim").click();
        HtmlForm form = page.getFormByName("claim");
        HtmlSelect select = form.getSelectByName("_.errors");
        HashSet<String> set = new HashSet<String>();
        for(HtmlOption option:select.getOptions()){
            set.add(option.getValueAttribute());
        }
        assertTrue(set.contains("Default"));
        assertTrue(set.contains(CAUSE_NAME_2));
        assertTrue(set.contains(CAUSE_NAME_1));
    }

    private void createKnowledgeBase() throws Exception {
        FailureCause cause1 = new FailureCause(CAUSE_NAME_1, CAUSE_DESCRIPTION_1);
        FailureCause cause2 = new FailureCause(CAUSE_NAME_2, CAUSE_DESCRIPTION_2);
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause1);
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause2);
    }

    private ClaimBuildAction applyClaimWithFailureCauseSelected(String element, String error, String reason, String description) throws Exception {
        HtmlPage page = whenNavigatingtoClaimPage();
        page.getElementById(element).click();
        HtmlForm form = page.getFormByName("claim");
        form.getTextAreaByName("reason").setText(reason);
        HtmlSelect select = form.getSelectByName("_.errors");
        HtmlOption option = select.getOptionByValue(error);
        select.setSelectedAttribute(option, true);

        assertEquals(description, form.getTextAreaByName("errordesc").getTextContent());

        form.submit((HtmlButton) j.last(form.getHtmlElementsByTagName("button")));

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        return action;
    }

    private HtmlPage whenNavigatingtoClaimPage() throws Exception{
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("job/x/" + build.getNumber());
        return page;
    }

}
