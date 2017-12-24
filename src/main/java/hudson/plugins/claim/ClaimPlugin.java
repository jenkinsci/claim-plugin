package hudson.plugins.claim;

import hudson.Plugin;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;

public class ClaimPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        super.start();
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-sm",
                        "plugin/claim/images/16x16/claim.png",
                        Icon.ICON_SMALL_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-md",
                        "plugin/claim/images/24x24/claim.png",
                        Icon.ICON_MEDIUM_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-lg",
                        "plugin/claim/images/32x32/claim.png",
                        Icon.ICON_LARGE_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-xlg",
                        "plugin/claim/images/48x48/claim.png",
                        Icon.ICON_XLARGE_STYLE));
    }
}
