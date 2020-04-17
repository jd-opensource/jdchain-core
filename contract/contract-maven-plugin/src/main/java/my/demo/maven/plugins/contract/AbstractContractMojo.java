package my.demo.maven.plugins.contract;

import java.io.File;
import java.util.Map;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Base class for creating a contract package from project classes.
 * 
 * Reference {@link AbstractJarMojo}
 *
 */
public abstract class AbstractContractMojo extends AbstractMojo {

	private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html" };

	private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

//	@Parameter
//	private String[] includes;
//
//	@Parameter
//	private String[] excludes;

	@Parameter(property = "packDependencies", defaultValue = "true")
	private boolean packDependencies;

	@Parameter(property = "copyDependencies", defaultValue = "false")
	private boolean copyDependencies;

	@Parameter(property = "flag")
	private String flag;

	/**
	 * Directory containing the generated JAR.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;

	/**
	 * Name of the generated JAR.
	 */
	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String finalName;

	/**
	 * The {@link {MavenProject}.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The {@link MavenSession}.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	/**
	 * The archive configuration to use. See
	 * <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
	 * Archiver Reference</a>.
	 * 
	 * <br>
	 * Create an inner default archive configuration for the contract package.
	 */
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 *
	 */
	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * Timestamp for reproducible output archive entries, either formatted as ISO
	 * 8601 <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds
	 * since the epoch (like <a href=
	 * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
	 *
	 * @since 3.2.0
	 */
	@Parameter(defaultValue = "${project.build.outputTimestamp}")
	private String outputTimestamp;

	/**
	 * Return the specific output directory to serve as the root for the archive.
	 * 
	 * @return get classes directory.
	 */
	protected abstract File getClassesDirectory();

	/**
	 * @return the {@link #project}
	 */
	protected final MavenProject getProject() {
		return project;
	}

	protected abstract String getClassifier();

	protected abstract String getType();

	/**
	 * Returns the contract file to generate, based on an optional classifier.
	 *
	 * @param basedir         the output directory
	 * @param resultFinalName the name of the ear file
	 * @param classifier      an optional classifier
	 * @return the file to generate
	 */
	protected File getContractFile(File basedir, String resultFinalName, String classifier) {
		if (basedir == null) {
			throw new IllegalArgumentException("basedir is not allowed to be null");
		}
		if (resultFinalName == null) {
			throw new IllegalArgumentException("finalName is not allowed to be null");
		}

		StringBuilder fileName = new StringBuilder(resultFinalName);

		fileName.append("." + getType());

		return new File(basedir, fileName.toString());
	}

	/**
	 * Generates the CONTRACT archive.
	 * 
	 * @return The instance of File for the created archive file.
	 * @throws MojoExecutionException in case of an error.
	 */
	public File createArchive() throws MojoExecutionException {
		File contractFile = getContractFile(outputDirectory, finalName, getClassifier());

		MavenArchiver archiver = new MavenArchiver();
		archiver.setCreatedBy("JD Chain Contract Plugin", "com.jd.blockchain.maven.plugins", "contract-maven-plugin");

//		// reuse the jar archiver for the contract type;
//		archiver.setArchiver((JarArchiver) archivers.get("jar"));
		archiver.setArchiver(new ContractArchiver(getType()));

		archiver.setOutputFile(contractFile);

		// configure for Reproducible Builds based on outputTimestamp value
		archiver.configureReproducible(outputTimestamp);

		archive.setAddMavenDescriptor(false);
		archive.setForced(true);

		try {
			File contentDirectory = getClassesDirectory();
			if (!contentDirectory.exists()) {
//				getLog().warn("CONTRACT will be empty - no content was marked for inclusion!");
				throw new MojoExecutionException(
						"The [" + getType() + "] package is empty! -- No content was marked for inclusion!");
			} else {
				archiver.getArchiver().addDirectory(contentDirectory, getIncludes(), getExcludes());
			}

			archiver.createArchive(session, project, archive);

			return contractFile;
		} catch (Exception e) {
			throw new MojoExecutionException("Error occurred while assembling CONTRACT! --" + e.getMessage(), e);
		}
	}

	/**
	 * Generates the CONTRACT.
	 * 
	 * @throws MojoExecutionException in case of an error.
	 *
	 */
	public void execute() throws MojoExecutionException {
		// abort if empty;
		if (!getClassesDirectory().exists() || getClassesDirectory().list().length < 1) {
			throw new MojoExecutionException("The " + getType() + " is empty! -- No content was marked for inclusion!");
		}

		File contractFile = createArchive();

		// The CONTRACT package is actually an classifier from a JAR package.
		// So, attach the contract file to the maven project;
		projectHelper.attachArtifact(getProject(), getType(), getClassifier(), contractFile);

		getLog().info("Generated " + getType() + ": " + contractFile.getAbsolutePath());

		getLog().info(String.format("\r\npackageDependencies=%s \r\ncopyDependencies=%s \r\nflag=%s",
				packDependencies, copyDependencies, flag));
	}

	/**
	 * @return true in case where the classifier is not {@code null} and contains
	 *         something else than white spaces.
	 */
	protected boolean hasClassifier() {
		boolean result = false;
		if (getClassifier() != null && getClassifier().trim().length() > 0) {
			result = true;
		}

		return result;
	}

	private String[] getIncludes() {
//		if (includes != null && includes.length > 0) {
//			return includes;
//		}
		return DEFAULT_INCLUDES;
	}

	private String[] getExcludes() {
//		if (excludes != null && excludes.length > 0) {
//			return excludes;
//		}
		return DEFAULT_EXCLUDES;
	}
}
