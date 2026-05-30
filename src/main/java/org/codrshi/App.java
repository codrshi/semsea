package org.codrshi;

import org.codrshi.command.RootCommand;
import org.codrshi.repository.DbManager;
import picocli.CommandLine;

public class App 
{
    static void main( String[] args )
    {
        DbManager.init();

        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
