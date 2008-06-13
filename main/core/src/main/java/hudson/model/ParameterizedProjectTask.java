package hudson.model;

import hudson.model.Queue.Executable;

import java.io.IOException;
import java.util.List;

/**
 * A task representing a project that should be built with a certain set of
 * parameter values
 */
public class ParameterizedProjectTask implements Queue.Task {

	private final AbstractProject<?, ?> project;
	private final List<ParameterValue> parameters;

	public ParameterizedProjectTask(AbstractProject<?, ?> project,
			List<ParameterValue> parameters) {
		this.project = project;
		this.parameters = parameters;
	}

	@Override
	public void checkAbortPermission() {
		project.checkAbortPermission();
	}

	@Override
	public Executable createExecutable() throws IOException {
		AbstractBuild<?, ?> build = project.createExecutable();
		build.addAction(new ParametersAction(parameters, build));

		return build;
	}

	@Override
	public Label getAssignedLabel() {
		return project.getAssignedLabel();
	}

	@Override
	public long getEstimatedDuration() {
		return project.getEstimatedDuration();
	}

	@Override
	public String getFullDisplayName() {
		return project.getFullDisplayName();
	}

	@Override
	public Node getLastBuiltOn() {
		return project.getLastBuiltOn();
	}

	@Override
	public String getName() {
		return project.getName();
	}

	@Override
	public String getWhyBlocked() {
		return project.getWhyBlocked();
	}

	@Override
	public boolean hasAbortPermission() {
		return project.hasAbortPermission();
	}

	@Override
	public boolean isBuildBlocked() {
		return project.isBuildBlocked();
	}

	@Override
	public String getDisplayName() {
		return project.getDisplayName();
	}

	@Override
	public ResourceList getResourceList() {
		return project.getResourceList();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizedProjectTask other = (ParameterizedProjectTask) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters)) {
			return false;
		}
		if (project != other.project) {
			return false;
		}
		return true;
	}
}
