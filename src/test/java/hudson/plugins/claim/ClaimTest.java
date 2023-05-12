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

import com.gargoylesoftware.htmlunit.html.*;
import static org.junit.Assert.assertEquals;
import hudson.model.Build;
import hudson.model.User;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Action;
import hudson.plugins.claim.utils.TestBuilder;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ClaimTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private Build<?, ?> firstBuild;
    private Project<?, ?> project;
    private TestBuilder builder;
    private String claimText = "claimReason";

    @Before
    public void setUp() throws Exception {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        JenkinsRule.WebClient wc = j.createWebClient();
        // Three users exist, we will be user1 so ensure the others have logged on
        wc.login("user0", "user0");
        wc.login("user2", "user2");
        wc.close();

        project = j.createFreeStyleProject("x");
        builder = new TestBuilder();
        project.getBuildersList().add(builder);
        project.getPublishersList().add(new ClaimPublisher());
        firstBuild = project.scheduleBuild2(0).get();
    }

    @Test
    public void failedBuildWithClaimPublisherHasClaimAction() {
        assertThat(firstBuild.getAction(ClaimBuildAction.class), is(notNullValue()));
    }

    @Test
    public void failedBuildCanBeClaimedByYou() throws Exception {
        // When:
        ClaimBuildAction action = whenClaimingBuild(firstBuild);
        // Then:
        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.getAssignedBy(), is("user1"));
    }

    @Test
    public void failedBuildCanBeAssigned() throws Exception {
        // When:
        ClaimBuildAction action = whenAssigningBuildByClicking(firstBuild, "claim");
        // Then:
        assertThat(action.getClaimedBy(), is("user2"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.getAssignedBy(), is("user1"));
    }

    @Test
    public void claimedBuildCanBeReclaimedByYou() throws Exception {
        // Given:
        givenBuildClaimedByOtherUser(firstBuild);
        // When:
        ClaimBuildAction action = whenClaimingBuildByClicking(firstBuild, "reassign");
        // Then:
        assertThat(action.getClaimedBy(), is("user1"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.getAssignedBy(), is("user1"));
    }

    @Test
    public void claimCanBeDropped() throws Exception {
        // Given:
        givenBuildClaimedByCurrentUser(firstBuild);
        // When:
        whenNavigatingToClaimPageAndClicking(firstBuild, "claim/unclaim");
        // Then:
        ClaimBuildAction action = firstBuild.getAction(ClaimBuildAction.class);
        assertThat(action.isClaimed(), is(false));
    }

    @Test
    public void claimCanBeReassigned() throws Exception {
        // Given:
        givenBuildClaimedByCurrentUser(firstBuild);
        // When:
        ClaimBuildAction action = whenAssigningBuildByClicking(firstBuild, "reassign");
        // Then:
        assertThat(action.getClaimedBy(), is("user2"));
        assertThat(action.getReason(), is(claimText));
        assertThat(action.isClaimed(), is(true));
        assertThat(action.getAssignedBy(), is("user1"));
    }

    @Test
    public void stickyClaimPropagatesToNextBuild() throws Exception {
        final int waitTime = 2_000;
        // Given:
        givenBuildClaimedByCurrentUser(firstBuild);
        // When:
        Thread.sleep(waitTime);
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action = firstBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(true));
        assertThat(action2.getClaimedBy(), is("user1"));
        assertThat(action2.getReason(), is("reason"));
        assertThat(action2.isSticky(), is(true));
        assertThat(action2.getAssignedBy(), is("user1"));
        assertThat(action2.getClaimDate(), is(action.getClaimDate()));
    }

    @Test
    public void stickyClaimDoesPropagatesToNextBuildWithUnknownAssignedByWhenAssignedByUserHasBeenDeleted() throws Exception {
        final int waitTime = 2_000;
        // Given:
        givenBuildClaimedByOtherUser(firstBuild);
        User user = User.getById("user1", false);
        user.delete();

        // When:
        Thread.sleep(waitTime);
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action = firstBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(true));
        assertThat(action2.getClaimedBy(), is("user2"));
        assertThat(action2.getReason(), is("reason"));
        assertThat(action2.isSticky(), is(true));
        assertThat(action2.getAssignedBy(), is(User.getUnknown().getId()));
        assertThat(action2.getClaimDate(), is(action.getClaimDate()));
    }

    @Test
    public void stickyClaimDoesNotPropagatesToNextBuildWhenClaimedByUserHasBeenDeleted() throws Exception {
        final int waitTime = 2_000;
        // Given:
        givenBuildClaimedByCurrentUser(firstBuild);
        User user = User.getById("user1", false);
        user.delete();

        // When:
        Thread.sleep(waitTime);
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action = firstBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(false));
    }

    @Test
    public void stickyClaimOnPreviousBuildPropagatesToFollowingFailedBuilds()  throws Exception {
        // Given:
        Build<?, ?> secondBuild = project.scheduleBuild2(0).get();
        Build<?, ?> thirdBuild = project.scheduleBuild2(0).get();
        Build<?, ?> fourthBuild = project.scheduleBuild2(0).get();
        // When:
        ClaimBuildAction firstAction = whenAssigningBuildByClicking(firstBuild, "claim", true);
        // Then:
        ClaimBuildAction[] actions = new ClaimBuildAction[] {
                secondBuild.getAction(ClaimBuildAction.class),
                thirdBuild.getAction(ClaimBuildAction.class),
                fourthBuild.getAction(ClaimBuildAction.class),
        };
        for (ClaimBuildAction action : actions) {
            assertThat(action.isClaimed(), is(true));
            assertThat(action.getClaimedBy(), is("user2"));
            assertThat(action.getReason(), is(claimText));
            assertThat(action.isSticky(), is(true));
            assertThat(action.getAssignedBy(), is("user1"));
            assertThat(action.getClaimDate(), is(firstAction.getClaimDate()));
        }
    }

    @Test
    public void stickyClaimOnPreviousBuildPropagatesToFollowingFailedBuildsUntilBuildIsPassing()  throws Exception {
        // Given:
        Build<?, ?> secondBuild = project.scheduleBuild2(0).get();
        givenProjectIsSucceeding();
        Build<?, ?> thirdBuild = project.scheduleBuild2(0).get();
        givenProjectIsFailing();
        Build<?, ?> fourthBuild = project.scheduleBuild2(0).get();
        // When:
        ClaimBuildAction action1 = whenAssigningBuildByClicking(firstBuild, "claim", true);
        // Then:
        ClaimBuildAction action2 = secondBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action3 = thirdBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action4 = fourthBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(true));
        assertThat(action2.getClaimedBy(), is("user2"));
        assertThat(action2.getReason(), is(claimText));
        assertThat(action2.isSticky(), is(true));
        assertThat(action2.getAssignedBy(), is("user1"));
        assertThat(action2.getClaimDate(), is(action1.getClaimDate()));
        assertThat(action3, nullValue());
        assertThat(action4.isClaimed(), is(false));
    }

    @Test
    public void stickyClaimOnPreviousBuildPropagatesToFollowingFailedBuildsUntilBuildIsClaimed()  throws Exception {
        final int waitTime = 2_000;
        // Given:
        Build<?, ?> secondBuild = project.scheduleBuild2(0).get();
        Build<?, ?> thirdBuild = project.scheduleBuild2(0).get();
        Thread.sleep(waitTime);
        whenClaimingBuildByClicking(thirdBuild, "claim");
        // When:
        Thread.sleep(waitTime);
        ClaimBuildAction action1 = whenAssigningBuildByClicking(firstBuild, "claim", true);
        // Then:
        ClaimBuildAction action2 = secondBuild.getAction(ClaimBuildAction.class);
        ClaimBuildAction action3 = thirdBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(true));
        assertThat(action2.getClaimedBy(), is("user2"));
        assertThat(action2.getReason(), is(claimText));
        assertThat(action2.isSticky(), is(true));
        assertThat(action2.getAssignedBy(), is("user1"));
        assertThat(action2.getClaimDate(), is(action1.getClaimDate()));
        assertThat(action3.isClaimed(), is(true));
        assertThat(action3.getClaimedBy(), is("user1"));
        assertThat(action3.getReason(), is(claimText));
        assertThat(action3.isSticky(), is(true));
        assertThat(action3.getAssignedBy(), is("user1"));
    }
    @Test
    public void claimTestShouldGiveProperURL() {
        ClaimTestDataPublisher.Data data = new ClaimTestDataPublisher.Data(firstBuild);
        //testObjectId now contains junit/ since hudson 1.347
        ClaimTestAction acti = new ClaimTestAction(data, "junit/assembly/classTest/unitTest");
        assertEquals("wrong url who would not link to test",
                "job/x/1/testReport/junit/assembly/classTest/unitTest", acti.getUrl());
    }
    @Test
    public void claimTestShouldGiveProperURL() {
        ClaimTestDataPublisher.Data data = new ClaimTestDataPublisher.Data(firstBuild);
        //testObjectId now contains junit/ since hudson 1.347
        ClaimTestAction acti = new ClaimTestAction(data, "junit/assembly/classTest/unitTest");
        assertEquals("wrong url who would not link to test",
                "job/x/1/testReport/junit/assembly/classTest/unitTest", acti.getUrl());
    }

    private void givenProjectIsSucceeding() {
        builder.setResult(Result.SUCCESS);
    }

    private void givenProjectIsFailing() {
        builder.setResult(Result.FAILURE);
    }


    @Test
    public void nonStickyClaimDoesNotPropagateToNextBuild() throws Exception {
        // Given:
        ClaimBuildAction action1 = givenBuildClaimedByCurrentUser(firstBuild);
        action1.setSticky(false);
        // When:
        Build<?, ?> nextBuild = project.scheduleBuild2(0).get();
        // Then:
        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertThat(action2.isClaimed(), is(false));
    }

    private ClaimBuildAction givenBuildClaimedByOtherUser(Build<?, ?> build) {
        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        User user1 = User.getById("user1", true);
        User user2 = User.getById("user2", true);
        action.claim(user2, "reason", user1, new Date(), true,
                false, false);
        return action;
    }

    private ClaimBuildAction givenBuildClaimedByCurrentUser(Build<?, ?> build) {
        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        User user1 = User.getById("user1", true);
        action.claim(user1, "reason", user1, new Date(), true,
                false, false);
        return action;
    }

    private ClaimBuildAction whenClaimingBuild(Build<?, ?> build) throws Exception {
        return applyClaim(build, "claim", null, claimText, false);
    }

    private ClaimBuildAction whenClaimingBuildByClicking(Build<?, ?> build, String claimElement) throws Exception {
        return applyClaim(build, claimElement, "user1", claimText, false);
    }

    private ClaimBuildAction whenAssigningBuildByClicking(Build<?, ?> build, String claimElement)
            throws Exception {
        return whenAssigningBuildByClicking(build, claimElement, false);
    }

    private ClaimBuildAction whenAssigningBuildByClicking(Build<?, ?> build, String claimElement, boolean propagate)
            throws Exception {
        return applyClaim(build, claimElement, "user2", claimText, propagate);
    }

    private ClaimBuildAction applyClaim(Build<?, ?> build, String claimElement, String assignee, String reason,
                                        boolean propagate)
            throws Exception {
        HtmlPage page = whenNavigatingToClaimPageAndClicking(build, claimElement);
        HtmlForm form = page.getFormByName("claim");

        form.getTextAreaByName("reason").setText(reason);
        if (assignee != null) {
            HtmlSelect select = form.getSelectByName("_.assignee");
            if (!assignee.isEmpty()) {
                HtmlOption option = select.getOptionByValue(assignee);
                select.setSelectedAttribute(option, true);
            }
        }
        HtmlCheckBoxInput propagateInput = form.getInputByName("propagateToFollowingBuilds");
        propagateInput.setChecked(propagate);
        HtmlFormUtil.submit(form, j.last(form.getElementsByTagName("button")));

        return build.getAction(ClaimBuildAction.class);
    }

    private HtmlPage whenNavigatingToClaimPageAndClicking(Build<?, ?> build, String idOrHref) throws Exception {
        try(JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.login("user1", "user1");
            HtmlPage page = wc.goTo("job/x/" + build.getNumber());
            // expand claim HTML box
            var element = page.getElementById(idOrHref);
            if (element == null) {
                element = page.getAnchorByHref(idOrHref);
            }
            element.click();
            return page;
        }
    }
}
