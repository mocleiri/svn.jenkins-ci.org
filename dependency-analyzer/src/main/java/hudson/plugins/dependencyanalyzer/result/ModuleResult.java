package hudson.plugins.dependencyanalyzer.result;

import hudson.maven.ModuleName;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Dependency analyze result for a module.
 *
 * @author Vincent Sellier
 * @author Etienne Jouvin
 *
 */
public class ModuleResult implements Serializable {

	private static final long serialVersionUID = -6461651211214230477L;

	private Map<DependencyProblemType, List<String>> dependencyProblems;
	private String displayName;
	private ModuleName moduleName;

	/**
	 * @return the dependencyProblems.
	 */
	public Map<DependencyProblemType, List<String>> getDependencyProblems() {
		return this.dependencyProblems;
	}

	/**
	 * @return the displayName.
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * @return the moduleName.
	 */
	public ModuleName getModuleName() {
		return this.moduleName;
	}

	/**
	 * @return Used dependencies, but not declared, list.
	 */
	public List<String> getUndeclaredDependencies() {
		return this.dependencyProblems.get(DependencyProblemType.UNDECLARED);
	}

	/**
	 * @return The number for used dependencies but not declared.
	 */
	public Integer getUndeclaredDependenciesCount() {
		List<String> dependencies = this.getUndeclaredDependencies();

		return dependencies == null ? 0 : dependencies.size();
	}

	/**
	 * @return Declared dependencies, but not used, list.
	 */
	public List<String> getUnusedDependencies() {
		return this.dependencyProblems.get(DependencyProblemType.UNUSED);
	}

	/**
	 * @return The number for declared dependencies but not used.
	 */
	public Integer getUnusedDependenciesCount() {
		List<String> dependencies = this.getUnusedDependencies();

		return dependencies == null ? 0 : dependencies.size();
	}

	/**
	 * @param dependencyProblems the dependencyProblems to set.
	 */
	public void setDependencyProblems(Map<DependencyProblemType, List<String>> dependencyProblems) {
		this.dependencyProblems = dependencyProblems;
	}

	/**
	 * @param displayName the displayName to set.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @param moduleName the moduleName to set.
	 */
	public void setModuleName(ModuleName moduleName) {
		this.moduleName = moduleName;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
