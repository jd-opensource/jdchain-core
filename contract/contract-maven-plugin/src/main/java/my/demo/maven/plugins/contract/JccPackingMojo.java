package my.demo.maven.plugins.contract;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "pack", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JccPackingMojo extends AbstractContractMojo {

	/**
	 * Directory containing the classes and resource files that should be packaged
	 * into the JAR.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	@Override
	protected File getClassesDirectory() {
		return classesDirectory;
	}

	@Override
	protected String getClassifier() {
		return "contract";
	}

	@Override
	protected String getType() {
		// java chain code / java contract code;
		return "jcc";
	}

}
