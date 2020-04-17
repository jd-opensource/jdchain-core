package my.demo.maven.plugins.contract;

import org.codehaus.plexus.archiver.jar.JarArchiver;

public class ContractArchiver extends JarArchiver {
	
	public ContractArchiver(String type) {
		archiveType = type;
	}
	
}
