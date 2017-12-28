package hudson.plugins.claim;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DescribableTestActionTest {
  @Rule
  public JenkinsRule j = new JenkinsRule();

  private User user0;
  private User user1;
  private User user2;
  private User user3;
  private User user4;

  @Before
  public void setUp() throws Exception {
    j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
    j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
    JenkinsRule.WebClient wc = j.createWebClient();
    wc.login("user0", "user0");
    wc.login("user1", "user1");
    wc.login("user2", "user2");
    wc.login("user3", "user3");
    wc.login("user4", "user4");
    wc.close();

    (user0 = j.jenkins.getUser("user0")).setFullName("T-800");
    (user1 = j.jenkins.getUser("user1")).setFullName("Sarah CONNOR");
    (user2 = j.jenkins.getUser("user2")).setFullName("John CONNOR");
    (user3 = j.jenkins.getUser("user3")).setFullName("Kyle REESE");
    (user4 = j.jenkins.getUser("user4")).setFullName("T-800");
  }

  private static class DescribableTestActionImpl extends DescribableTestAction
  {
    @Override
    public String getIconFileName() {
      return null;
    }

    @Override
    public String getDisplayName() {
      return null;
    }

    @Override
    public String getUrlName() {
      return null;
    }
  }

  @Test
  public void assignee_list_is_sorted_by_id_by_default() {
      ACL.impersonate(user1.impersonate(), () -> {
          DescribableTestActionImpl.DescriptorImpl descriptor = new DescribableTestActionImpl.DescriptorImpl();
          ListBoxModel items = descriptor.doFillAssigneeItems();
          Object[] users = getUsers(items);
          assertArrayEquals(new String[] { user1.getId(), user0.getId(), user2.getId(), user3.getId(),
                  user4.getId()}, users);
      });
  }

  @Test
  public void assignee_list_full_name_sorts_by_fullName_then_id() {
      ACL.impersonate(user1.impersonate(), () -> {
          DescribableTestActionImpl.DescriptorImpl descriptor = new DescribableTestActionImpl.DescriptorImpl();
          ((ClaimConfig)j.jenkins.getDescriptor(ClaimConfig.class)).setSortUsersByFullName(true);
          ListBoxModel items = descriptor.doFillAssigneeItems();
          Object[] users = getUsers(items);
          assertArrayEquals(new String[] { user1.getId(), user2.getId(), user3.getId(), user0.getId(),
                  user4.getId()}, users);
      });
  }

  private String[] getUsers(ListBoxModel items) {
    return items.stream().map(o -> o.value).filter(o -> !ACL.SYSTEM_USERNAME.equals(o)).toArray(String[]::new);
  }
}
