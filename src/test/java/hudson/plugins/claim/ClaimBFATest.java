package hudson.plugins.claim;

import org.htmlunit.html.*;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import hudson.model.Build;
import hudson.model.Project;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.HashSet;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class ClaimBFATest {

    private JenkinsRule j;
    private Build<?, ?> build;
    private Project<?, ?> project;

    private static final String CAUSE_NAME_1 = "Cause1";
    private static final String CAUSE_NAME_2 = "Cause2";
    private static final String CAUSE_NAME_3 = "Cause3";
    private static final String CAUSE_DESCRIPTION_1 = "DescriptionForCause1";
    private static final String CAUSE_DESCRIPTION_2 = "DescriptionForCause2";
    private static final String CAUSE_DESCRIPTION_WITH_SINGLE_QUOTE = "DescriptionWith'ForCause3";
    private static final String IDENTIFIED_PROBLEMS = "Identified problems";
    private static final String REASON = "Test Reason";


    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        this.j = j;
        java.util.logging.Logger.getLogger("org.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        createKnowledgeBase();
        project = j.createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();
    }

    @Test
    void canClaimFailureCause() throws Exception {
        try(JenkinsRule.WebClient webClient = j.createWebClient()) {
            ClaimBuildAction action = applyClaimWithFailureCauseSelected(webClient, "claim", CAUSE_NAME_2, REASON,
                    CAUSE_DESCRIPTION_2);

            assertThat(action.getClaimedBy(), is("user1"));
            assertThat(action.getReason(), is(REASON));
            assertThat(action.isClaimed(), is(true));
            assertThat(action.isBFAEnabled(), is(true));

            HtmlPage page = whenNavigatingToClaimPage(webClient);
            assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
            assertTrue(page.asXml().contains(CAUSE_NAME_2));
        }
    }

    @Test
    void canReclaimFailureCause() throws Exception {
        try(JenkinsRule.WebClient webClient = j.createWebClient()) {
            applyClaimWithFailureCauseSelected(webClient, "claim", CAUSE_NAME_2, REASON, CAUSE_DESCRIPTION_2);
            ClaimBuildAction action = applyClaimWithFailureCauseSelected(webClient, "reassign", CAUSE_NAME_1, REASON,
                    CAUSE_DESCRIPTION_1);

            assertThat(action.getClaimedBy(), is("user1"));
            assertThat(action.getReason(), is(REASON));
            assertThat(action.isClaimed(), is(true));
            assertThat(action.isBFAEnabled(), is(true));

            HtmlPage page = whenNavigatingToClaimPage(webClient);
            assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
            assertTrue(page.asXml().contains(CAUSE_NAME_1));
        }
    }

    @Test
    void canClaimFailureWithSingleQuoteInDescription() throws Exception {
        try(JenkinsRule.WebClient webClient = j.createWebClient()) {
            FailureCause cause3 = new FailureCause(CAUSE_NAME_3, CAUSE_DESCRIPTION_WITH_SINGLE_QUOTE);
            PluginImpl.getInstance().getKnowledgeBase().addCause(cause3);
            ClaimBuildAction action = applyClaimWithFailureCauseSelected(webClient, "claim", CAUSE_NAME_3, REASON,
                    CAUSE_DESCRIPTION_WITH_SINGLE_QUOTE);
            assertThat(action.isClaimed(), is(true));
        }
    }

    @Test
    void canDropFailureCause() throws Exception {
        try(JenkinsRule.WebClient webClient = j.createWebClient()) {
            ClaimBuildAction action = applyClaimWithFailureCauseSelected(webClient, "claim", CAUSE_NAME_2, REASON,
                    CAUSE_DESCRIPTION_2);

            HtmlPage page = whenNavigatingToClaimPage(webClient);
            page.getAnchorByHref("claim/unclaim").click();
            page = whenNavigatingToClaimPage(webClient);
            assertThat(action.isClaimed(), is(false));
            assertTrue(page.asXml().contains(IDENTIFIED_PROBLEMS));
            assertTrue(page.asXml().contains("No identified problem"));
        }
    }

    @Test
    void testCreateKnowledgeBase() throws Exception {
        createKnowledgeBase();
        assertNotNull(PluginImpl.getInstance().getKnowledgeBase().getCauses());
    }

    @Test
    void errorDropdownIsPresentAndIsNotEmpty() throws Exception {
        try(JenkinsRule.WebClient webClient = j.createWebClient()) {
            webClient.login("user1", "user1");
            HtmlPage page = webClient.goTo("job/x/" + build.getNumber());
            page.getElementById("claim").click();
            HtmlForm form = page.getFormByName("claim");
            HtmlSelect select = form.getSelectByName("_.errors");
            HashSet<String> set = new HashSet<>();
            for (HtmlOption option : select.getOptions()) {
                set.add(option.getValueAttribute());
            }
            assertTrue(set.contains("Default"));
            assertTrue(set.contains(CAUSE_NAME_2));
            assertTrue(set.contains(CAUSE_NAME_1));
        }
    }

    private void createKnowledgeBase() throws Exception {
        FailureCause cause1 = new FailureCause(CAUSE_NAME_1, CAUSE_DESCRIPTION_1);
        FailureCause cause2 = new FailureCause(CAUSE_NAME_2, CAUSE_DESCRIPTION_2);
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause1);
        PluginImpl.getInstance().getKnowledgeBase().addCause(cause2);
    }

    private ClaimBuildAction applyClaimWithFailureCauseSelected(JenkinsRule.WebClient webClient, String element,
                                                                String error, String reason,
                                                                String description) throws Exception {
        final int timeout = 1000;
        HtmlPage page = whenNavigatingToClaimPage(webClient);
        page.getElementById(element).click();
        HtmlForm form = page.getFormByName("claim");
        form.getTextAreaByName("reason").setText(reason);
        HtmlSelect select = form.getSelectByName("_.errors");
        HtmlOption option = select.getOptionByValue(error);
        select.setSelectedAttribute(option, true);
        // wait for async javascript callback to execute
        synchronized (page) {
            page.wait(timeout);
        }
        assertEquals(description, form.getTextAreaByName("errordesc").getTextContent());

        HtmlFormUtil.submit(form, j.last(form.getElementsByTagName("button")));

        return build.getAction(ClaimBuildAction.class);
    }

    private HtmlPage whenNavigatingToClaimPage(JenkinsRule.WebClient webClient) throws Exception {
        webClient.login("user1", "user1");
        return webClient.goTo("job/x/" + build.getNumber());
    }
}
