package hudson.plugins.mavendeploymentlinker;

import hudson.model.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.kohsuke.stapler.export.Exported;

public class MavenDeploymentLinkerAction implements Action {
    private static class ArtifactVersion {
        private static final String SNAPSHOT_PATTERN = ".*-SNAPSHOT.*";
        private static final Pattern p = Pattern.compile(SNAPSHOT_PATTERN);
        
        private ArtifactVersion(String url) {
            this.url = url;
            checkRelease();
        }
        private final String url;
        private boolean snapshot;

        private void checkRelease() {
            snapshot = p.matcher(url).matches();
        }
        public boolean isSnapshot() {
            return snapshot;
        }
        public String getText() {
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("\n<li>");
            textBuilder.append("<a href=\"" + url + "\">");
            textBuilder.append(url.substring(url.lastIndexOf('/') + 1, url.length()));
            textBuilder.append("</a>");
            textBuilder.append("</li>\n");
            return textBuilder.toString();
        }
    }
    
    private List<ArtifactVersion> deployments = new ArrayList<ArtifactVersion>();
    
    private transient String text;

    private boolean snapshot = false;
    
    public boolean isSnapshot() {
        return snapshot;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "";
    }

    @Exported
    public String getText() {
        if (text == null) {
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("<ul>");
            for (ArtifactVersion artifact : deployments) {
                textBuilder.append(artifact.getText());
            }
            textBuilder.append("</ul>");
            text = textBuilder.toString();
        }
        return text;
    }

    public void addDeployment(String url) {
        ArtifactVersion artifactVersion = new ArtifactVersion(url);
        if (artifactVersion.isSnapshot()) {
            snapshot = true;
        }
        deployments.add(artifactVersion);
    }

}
