package com.jd.blockchain.tools.cli;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;

/**
 * @description: JD Chain command line tools
 * @author: imuge
 * @date: 2021/7/23
 **/
@CommandLine.Command(name = "jdchain-cli", mixinStandardHelpOptions = true, version = "1.0",
        description = "JDChain Cli is a convenient tool to manage jdchain keys, " +
                "sign and send transactions to jdchain network, " +
                "query data from jdchain network.",
        commandListHeading = "%nCommands:%n%nThe most commonly used commands are:%n",
        footer = "%nSee 'jdchain-cli help <command>' to read about a specific subcommand or concept.",
        subcommands = {
                Keys.class,
                CA.class,
                Tx.class,
                Query.class,
                Participant.class,
                CommandLine.HelpCommand.class
        })
public class JDChainCli implements Runnable {

    @CommandLine.Option(names = "--home", defaultValue = "../", description = "Set the home directory.", scope = CommandLine.ScopeType.INHERIT)
    File path;

    @CommandLine.Option(names = "--pretty", defaultValue = "false", description = "Pretty json print", scope = CommandLine.ScopeType.INHERIT)
    boolean pretty;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);
        System.exit(new CommandLine(new JDChainCli()).execute(args));
    }

    public String getHomePath() {
        try {
            return path.getCanonicalPath();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}
