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

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.FailureBuilder;
import org.mockito.Mockito;

import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Build;
import hudson.model.Result;
import hudson.model.User;
import hudson.model.Hudson;
import hudson.plugins.claim.ClaimBuildAction;
import hudson.plugins.claim.ClaimBuildActionWrapper;
import hudson.plugins.claim.ClaimPublisher;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;

public class ClaimTest extends HudsonTestCase {
	private Build<?,?> build;
    private Project<?, ?> project;
    private Set<User> culprits;
    private User user1;
    private User user2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);
        hudson.setSecurityRealm(realm);

        user1 = realm.createAccount("user1", "user1");
        user2 = realm.createAccount("user2", "user2");


        project = createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();

        /*Setting culprtis*/
        culprits = new HashSet<User>();
        culprits.add(user1);
        culprits.add(user2);

    }

    public void testHasClaimAction() {
        assertNotNull(build.getAction(ClaimBuildAction.class));
    }

    public void testFirstClaim() throws Exception {
        WebClient wc = new WebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("/job/x/" + build.getNumber());
        ((HtmlAnchor) page.getElementById("claim")).click();

        HtmlForm form = page.getFormByName("claim");
        HtmlTextArea textArea = (HtmlTextArea) last(form.selectNodes(".//textarea"));
        HtmlCheckBoxInput checkBox = (HtmlCheckBoxInput) last(form.selectNodes(".//input[@name='sticky']"));
        checkBox.setChecked(false);
        String claimText = "claimReason";
        textArea.setText(claimText);

        form.submit((HtmlButton) last(form.selectNodes(".//button")));

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        assertEquals("claiming user incorrect", "user1", action.getClaimedBy());
        assertEquals("claim text incorrect", claimText, action.getReason());
        assertTrue("build is not claimed", action.isClaimed());
        assertFalse("build should not be sticky", action.isSticky());
    }

    public void testClaimForYourself() throws Exception {
        build.getAction(ClaimBuildAction.class).claim("user2", "reason", true);

        WebClient wc = new WebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("/job/x/" + build.getNumber());
        ((HtmlAnchor) page.getElementById("claimForYourself")).click();

        HtmlForm form = page.getFormByName("claim");
        HtmlTextArea textArea = (HtmlTextArea) last(form.selectNodes(".//textarea"));
        String claimText = "claimReason";
        textArea.setText(claimText);

        form.submit((HtmlButton) form.getElementsByTagName("button").item(0));

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        assertEquals("claiming user incorrect", "user1", action.getClaimedBy());
        assertEquals("claim text incorrect", claimText, action.getReason());
        assertTrue(action.isClaimed());
    }

    public void testDropClaim() throws Exception {
        build.getAction(ClaimBuildAction.class).claim("user1", "reason", true);

        WebClient wc = new WebClient();
        wc.login("user1", "user1");
        HtmlPage page = wc.goTo("/job/x/" + build.getNumber());
        ((HtmlAnchor) page.getElementById("dropClaim")).click();

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        assertFalse(action.isClaimed());
    }

    public void testStickyBuild() throws Exception {
        ClaimBuildAction action1 = build.getAction(ClaimBuildAction.class);
        action1.claim("user1", "reason", true);

        Build<?,?> nextBuild = project.scheduleBuild2(0).get();

        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertTrue("build is not claimed", action2.isClaimed());
        assertEquals("claiming user not equal", action1.getClaimedBy(), action2.getClaimedBy());
        assertEquals("claim reason not equal", action1.getReason(), action2.getReason());
        assertTrue("build should be sticky", action2.isSticky());
    }

    public void testNotStickyBuild() throws Exception {
        ClaimBuildAction action1 = build.getAction(ClaimBuildAction.class);
        action1.claim("user1", "reason", false);

        Build<?,?> nextBuild = project.scheduleBuild2(0).get();

        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertFalse("build is claimed", action2.isClaimed());
    }

    public void testIsSameCulprit_CulpritIsNull() throws Exception {
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	assertFalse(testClaim.isSameCulprit(null));
    }

    public void testIsSameCulprit_CulpritIsEmpty() throws Exception {
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	assertFalse(testClaim.isSameCulprit(""));
    }

    public void testIsSameCulprit_NoCulprits() throws Exception {
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(new HashSet<User>());
    	assertFalse(testClaim.isSameCulprit(user1.getId()));
    }

    public void testIsSameCulprit_NotSameCulprit() throws Exception {
    	final String invalidCulprit = "invalidCulprit";
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	assertFalse(testClaim.isSameCulprit(invalidCulprit));
    }

    public void testIsSameCulprit_SameCulprit() throws Exception {
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	assertTrue(testClaim.isSameCulprit(user1.getId()));
    }

    public void testClaimGivenBuild_BuildIsNotClaimedAndCulpritIsNotSame() throws Exception {
    	Build<?, ?> otherBuild = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(otherBuild);
    	testClaim.setCulprits(culprits);

    	Mockito.when(otherBuild.getAction(ClaimBuildAction.class)).thenReturn(testClaim);

    	/*try claiming for the build*/
    	testClaim.claimGivenBuild("user2", "", true, "user3", otherBuild);

    	ClaimBuildAction otherClaim = otherBuild.getAction(ClaimBuildAction.class);
    	assertFalse(otherClaim.isClaimed());
    }

    public void testClaimGivenBuild_BuildIsNotClaimedAndCulpritIsSame() throws Exception {
    	Build<?, ?> otherBuild = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(otherBuild);
    	testClaim.setCulprits(culprits);

    	Mockito.when(otherBuild.getAction(ClaimBuildAction.class)).thenReturn(testClaim);

    	/*try claiming for the build*/
    	testClaim.claimGivenBuild("user2", "", true, "user1", otherBuild);

    	ClaimBuildAction otherClaim = otherBuild.getAction(ClaimBuildAction.class);
    	assertTrue(otherClaim.isClaimed());
    	assertSame("user2", otherClaim.getClaimedBy());
    }

    public void testClaimGivenBuild_BuildIsClaimedByOtherUser() throws Exception {
    	Build<?, ?> otherBuild = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(otherBuild);
    	testClaim.setCulprits(culprits);
    	testClaim.claim("user1", "", true);

    	Mockito.when(otherBuild.getAction(ClaimBuildAction.class)).thenReturn(testClaim);

    	/*try claiming for the build*/
    	testClaim.claimGivenBuild("user2", "", true, "user1", otherBuild);

    	ClaimBuildAction otherClaim = otherBuild.getAction(ClaimBuildAction.class);
    	assertNotSame("user2", otherClaim.getClaimedBy());
    }

    public void testClaimAllBrokenBuilds_NoBuildsClaimed() {

    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	List<Job> items = createMockUnstableItems();
    	testClaim.setItems(items);

    	try {
			testClaim.claimAllBrokenBuilds("user1", "", true, "user1");
		} catch (IOException e) {
			e.printStackTrace();
		}

    	for(Job item : items) {
    		ClaimBuildAction build0Claim = item.getLastBuild().getAction(ClaimBuildAction.class);
    		assertTrue(build0Claim.isClaimed());
    		assertEquals("user1", build0Claim.getClaimedBy());
    	}
    }

    public void testClaimAllBrokenBuilds_SomeBuildsClaimed() {

    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	List<Job> items = createMockUnstableItems();
    	/*Mock the first build being claimed already by some other user*/
    	items.get(0).getLastBuild().getAction(ClaimBuildAction.class).claim("user1", "", true);
    	testClaim.setItems(items);

    	try {
			testClaim.claimAllBrokenBuilds("user2", "", true, "user1");
		} catch (IOException e) {
			e.printStackTrace();
		}

    	ClaimBuildAction build0Claim = items.get(0).getLastBuild().getAction(ClaimBuildAction.class);
    	assertTrue(build0Claim.isClaimed());
    	assertNotSame("user2", build0Claim.getClaimedBy());

    	ClaimBuildAction build1Claim = items.get(1).getLastBuild().getAction(ClaimBuildAction.class);
    	assertTrue(build1Claim.isClaimed());
    	assertSame("user2", build1Claim.getClaimedBy());
    }

    public void testClaimAllBrokenBuilds_SomeBuildsAreSuccess() {

    	ClaimBuildActionWrapper testClaim = new ClaimBuildActionWrapper(build);
    	testClaim.setCulprits(culprits);
    	List<Job> items = createMockUnstableItems();
    	items.addAll(createMockStableItems());

    	testClaim.setItems(items);

    	try {
			testClaim.claimAllBrokenBuilds("user2", "", true, "user1");
		} catch (IOException e) {
			e.printStackTrace();
		}

    	ClaimBuildAction build0Claim = items.get(0).getLastBuild().getAction(ClaimBuildAction.class);
    	assertTrue(build0Claim.isClaimed());
    	assertSame("user2", build0Claim.getClaimedBy());

    	ClaimBuildAction build1Claim = items.get(1).getLastBuild().getAction(ClaimBuildAction.class);
    	assertTrue(build1Claim.isClaimed());
    	assertSame("user2", build1Claim.getClaimedBy());

    	ClaimBuildAction build2Claim = items.get(2).getLastBuild().getAction(ClaimBuildAction.class);
    	assertFalse(build2Claim.isClaimed());

    	ClaimBuildAction build3Claim = items.get(3).getLastBuild().getAction(ClaimBuildAction.class);
    	assertFalse(build3Claim.isClaimed());
    }

    private List<Job> createMockUnstableItems() {
    	Build<?, ?> build0 = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper build0Claim = new ClaimBuildActionWrapper(build0);
    	build0Claim.setCulprits(culprits);
    	Mockito.when(build0.getAction(ClaimBuildAction.class)).thenReturn(build0Claim);
    	Mockito.when(build0.getResult()).thenReturn(Result.UNSTABLE);

    	Build<?, ?> build1 = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper build1Claim = new ClaimBuildActionWrapper(build1);
    	build1Claim.setCulprits(culprits);
    	Mockito.when(build1.getAction(ClaimBuildAction.class)).thenReturn(build1Claim);
    	Mockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);

    	Job job1 = Mockito.mock(Job.class);
    	Mockito.when(job1.getLastBuild()).thenReturn(build0);

    	Job job2 = Mockito.mock(Job.class);
    	Mockito.when(job2.getLastBuild()).thenReturn(build1);

    	List<Job> items = new ArrayList<Job>();
    	items.add(job1);
    	items.add(job2);

    	return items;
    }

    private List<Job> createMockStableItems() {
    	Build<?, ?> build0 = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper build0Claim = new ClaimBuildActionWrapper(build0);
    	build0Claim.setCulprits(new HashSet<User>());
    	Mockito.when(build0.getAction(ClaimBuildAction.class)).thenReturn(build0Claim);
    	Mockito.when(build0.getResult()).thenReturn(Result.SUCCESS);

    	Build<?, ?> build1 = Mockito.mock(Build.class);
    	ClaimBuildActionWrapper build1Claim = new ClaimBuildActionWrapper(build1);
    	build1Claim.setCulprits(new HashSet<User>());
    	Mockito.when(build1.getAction(ClaimBuildAction.class)).thenReturn(build1Claim);
    	Mockito.when(build1.getResult()).thenReturn(Result.SUCCESS);

    	Job job1 = Mockito.mock(Job.class);
    	Mockito.when(job1.getLastBuild()).thenReturn(build0);

    	Job job2 = Mockito.mock(Job.class);
    	Mockito.when(job2.getLastBuild()).thenReturn(build1);

    	List<Job> items = new ArrayList<Job>();
    	items.add(job1);
    	items.add(job2);

    	return items;
    }
}
