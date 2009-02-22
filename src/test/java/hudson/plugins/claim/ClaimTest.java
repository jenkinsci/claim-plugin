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
import hudson.model.Project;
import hudson.model.Build;
import hudson.model.User;
import hudson.model.Hudson;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;

import java.io.IOException;

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);
        hudson.setSecurityRealm(realm);

        User user1 = realm.createAccount("user1", "user1");
        User user2 = realm.createAccount("user2", "user2");


        project = createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();

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
        HtmlCheckBoxInput checkBox = (HtmlCheckBoxInput) last(form.selectNodes(".//input[@type='checkbox']"));
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
}
