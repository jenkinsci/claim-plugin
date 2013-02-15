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

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.FailureBuilder;
import org.xml.sax.SAXException;

import hudson.model.Project;
import hudson.model.Build;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;


public class ClaimTest extends HudsonTestCase {
    private Build<?,?> build;
    private Project<?, ?> project;
	private String claimText = "claimReason";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

        project = createFreeStyleProject("x");
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();

        hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        hudson.setSecurityRealm(createDummySecurityRealm());
    }

    public void testHasClaimAction() {
        assertNotNull(build.getAction(ClaimBuildAction.class));
    }

    public void testFirstClaim() throws Exception {
        ClaimBuildAction action = whenClaimingBuildByClicking("claim");
       
        assertEquals("user1", action.getClaimedBy());
        assertEquals(claimText, action.getReason());
        assertTrue(action.isClaimed());
    }

	public void testClaimForYourself() throws Exception {
		givenBuildClaimedByOtherUser();
        ClaimBuildAction action = whenClaimingBuildByClicking("claimForYourself");
        assertEquals("user1", action.getClaimedBy());
        assertEquals(claimText, action.getReason());
        assertTrue(action.isClaimed());
    }

	public void testDropClaim() throws Exception {
		givenBuildClaimedByCurrentUser();
        
        whenNavigatingToClaimPageAndClicking("dropClaim");

        ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
        assertFalse(action.isClaimed());
    }

    public void testStickyBuild() throws Exception {
        ClaimBuildAction action1 = build.getAction(ClaimBuildAction.class);
        action1.claim("user1", "reason", true);

        Build<?,?> nextBuild = project.scheduleBuild2(0).get();

        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertTrue(action2.isClaimed());
        assertEquals(action1.getClaimedBy(), action2.getClaimedBy());
        assertEquals(action1.getReason(), action2.getReason());
        assertTrue(action2.isSticky());
    }

    public void testNotStickyBuild() throws Exception {
        ClaimBuildAction action1 = build.getAction(ClaimBuildAction.class);
        action1.claim("user1", "reason", false);

        Build<?,?> nextBuild = project.scheduleBuild2(0).get();

        ClaimBuildAction action2 = nextBuild.getAction(ClaimBuildAction.class);
        assertFalse(action2.isClaimed());
    }

	private void givenBuildClaimedByOtherUser() {
		build.getAction(ClaimBuildAction.class).claim("user2", "reason", true);
	}

	private void givenBuildClaimedByCurrentUser() {
		build.getAction(ClaimBuildAction.class).claim("user1", "reason", true);
	}
	
	private ClaimBuildAction whenClaimingBuildByClicking(String claimElement) throws Exception, IOException,
			SAXException {
		HtmlPage page = whenNavigatingToClaimPageAndClicking(claimElement);
	
	    HtmlForm form = page.getFormByName("claim");
	    HtmlTextArea textArea = (HtmlTextArea) last(form.selectNodes(".//textarea"));
	    textArea.setText(claimText);
	    
	    form.submit((HtmlButton) last(form.selectNodes(".//button")));
	
	    ClaimBuildAction action = build.getAction(ClaimBuildAction.class);
		return action;
	}

	private HtmlPage whenNavigatingToClaimPageAndClicking(String claimElement)
			throws Exception, IOException, SAXException {
		WebClient wc = new WebClient();
	    wc.login("user1", "user1");
	    HtmlPage page = wc.goTo("/job/x/" + build.getNumber());
		((HtmlAnchor) page.getElementById(claimElement)).click();
		return page;
	}
}
