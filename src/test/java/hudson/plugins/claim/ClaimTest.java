/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Tom Huybrechts
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.claim;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import hudson.model.Build;
import hudson.model.Project;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.FailureBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

public class ClaimTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private Build<?, ?> build;
    private Project<?, ?> project;
    private String claimText = "claimReason";

    @Before
    public void setUp() throws Exception {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("user2", "user2");
        wc.closeAllWindows();

        project = j.createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();
    }

    @Test
    public void failed_build_with_claim_publisher_has_claim_action() {
        assertThat(build.getAction(ClaimBuildAction.class), is(notNullValue()));
    }

    @Test
    public void failed_build_can_be_claimed_by_you() throws Exception {
        // When:
        ClaimBuildAction action = whenClaimingBuildByClicking("claim");
        // Then:
        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
    }

    @Test
    public void failed_build_can_be_assigned() throws Exception {
        // When:
        ClaimBuildAction action = whenAssigningBuildByClicking("claim");
        // Then:
        assertThat(action.getClaimedBy(), is("user2"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
    }

    @Test
    public void claimed_build_can_be_reclaimed_by_you() throws Exception {
        // Given:
        givenBuildClaimedByOtherUser();
        // When:
        ClaimBuildAction action = whenClaimingBuildByClicking("reassign");
        // Then:
        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
    }

    @Test
    public void claim_can_be_dropped() throws Exception {
        // Given:
        givenBuildClaimedByCurrentUser();
        // When:
        whenNavigatingToClaimPageAndClicking("dropClaim");
        // Then:
        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        assertThat(action.isClaimed(), is(false));
    }

    @Test
    public void claim_can_be_reassigned() throws Exception {
        // Given:
        givenBuildClaimedByCurrentUser();
        // When:
        ClaimBuildAction action = whenAssigningBuildByClicking("reassign");
        // Then:
        assertThat(action.getClaimedBy(), is("user2"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
    }

    @Test
    public void sticky_claim_propagates_to_next_build() throws Exception {
        // Given:
        givenBuildClaimedByCurrentUser();
        // When:
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(true));
        assertThat(action2.getClaimedBy(), is("user1"));
        assertThat(action2.getReason(), is("reason"));
        assertThat(action2.isSticky(), is(true));
    }

    @Test
    public void non_sticky_claim_does_not_propagate_to_next_build() throws Exception {
        // Given:
        ClaimBuildAction action1 = givenBuildClaimedByCurrentUser();
        action1.setSticky(false);
        // When:
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(false));
    }

    private ClaimBuildAction givenBuildClaimedByOtherUser() {
        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        action.claim("user2", "reason", true);
        return action;
    }

    private ClaimBuildAction givenBuildClaimedByCurrentUser() {
        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        action.claim("user1", "reason", true);
        return action;
    }

    private ClaimBuildAction whenClaimingBuildByClicking(String claimElement) throws Exception {
        return applyClaim(claimElement, "", claimText);
    }

    private ClaimBuildAction whenAssigningBuildByClicking(String claimElement) throws Exception {
        return applyClaim(claimElement, "user2", claimText);
    }

    private ClaimBuildAction applyClaim(String claimElement, String assignee, String reason) throws Exception {
        HtmlPage page = whenNavigatingToClaimPageAndClicking(claimElement);
        HtmlForm form = page.getFormByName("claim");

        form.getTextAreaByName("reason").setText(reason);
        if ( assignee!=null ) {
        	HtmlSelect select = form.getSelectByName("_.assignee");
        	if (! assignee.isEmpty()) {
        		HtmlOption option = select.getOptionByValue(assignee);
        		select.setSelectedAttribute(option, true);
        	}
        }

        form.submit((HtmlButton) j.last(form.getHtmlElementsByTagName("button")));

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        return action;
    }

    private HtmlPage whenNavigatingToClaimPageAndClicking(String claimElement) throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("job/x/" + build.getNumber());
        page.getElementById(claimElement).click();
        return page;
    }
}
