package org.codrshi;

import org.codrshi.command.RootCommand;
import picocli.CommandLine;

/**
 * Hello world!
 *
 */
public class App 
{
    static void main( String[] args )
    {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
