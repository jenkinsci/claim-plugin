package hudson.plugins.claim;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.util.ListBoxModel;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@WithJenkins
class DescribableTestActionTest {

    private JenkinsRule j;

    private User user0;
    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        this.j = j;
        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("user0", "user0");
        wc.login("user1", "user1");
        wc.login("user2", "user2");
        wc.login("user3", "user3");
        wc.login("user4", "user4");
        wc.close();

        user0 = getUser("user0", "T-800");
        user1 = getUser("user1", "Sarah CONNOR");
        user2 = getUser("user2", "John CONNOR");
        user3 = getUser("user3", "Kyle REESE");
        user4 = getUser("user4", "T-800");
    }

    private User getUser(String id, String fullName) {
        User user = j.jenkins.getUser(id);
        user.setFullName(fullName);
        return user;
    }

    private static class DescribableTestActionImpl extends DescribableTestAction {

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
    void assigneeListIsSortedByIdByDefault() {
        try (ACLContext ignored = ACL.as2(user1.impersonate2())) {
            DescribableTestActionImpl.DescriptorImpl descriptor = new DescribableTestActionImpl.DescriptorImpl();
            ListBoxModel items = descriptor.doFillAssigneeItems();
            Object[] users = getUsers(items);
            assertArrayEquals(
                    new String[] {user1.getId(), user0.getId(), user2.getId(), user3.getId(), user4.getId()},
                    users);
        }
    }

    @Test
    void assigneeListFullNameSortsByFullNameThenId() {
        try (ACLContext ignored = ACL.as2(user1.impersonate2())) {
            DescribableTestActionImpl.DescriptorImpl descriptor = new DescribableTestActionImpl.DescriptorImpl();
            ((ClaimConfig) j.jenkins.getDescriptor(ClaimConfig.class)).setSortUsersByFullName(true);
            ListBoxModel items = descriptor.doFillAssigneeItems();
            Object[] users = getUsers(items);
            assertArrayEquals(
                    new String[] {user1.getId(), user2.getId(), user3.getId(), user0.getId(), user4.getId()},
                    users);
        }
    }

    private String[] getUsers(ListBoxModel items) {
        return items.stream().map(o -> o.value).filter(o -> !ACL.SYSTEM_USERNAME.equals(o)).toArray(String[]::new);
    }
}
