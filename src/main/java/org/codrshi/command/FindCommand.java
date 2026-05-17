package org.codrshi.command;

import org.codrshi.service.QueryService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "find"
)
public class FindCommand implements Callable<Integer> {

    // TODO: set limit to query length
    @Parameters(index = "0")
    private String query;

    @Option(names = "--limit", defaultValue = "5")
    private int limit;

    private final QueryService queryService;

    public FindCommand(){
        queryService = new QueryService();
    }

    @Override
    public Integer call() {
        List<List<String>> result = queryService.search(query, limit);

        System.out.printf("%-50s\t\t%-30s\n", "file", "last modified");
        System.out.print("-".repeat(85));

        result.forEach(file -> {
            String truncatedFileName = file.getFirst();
            int length = truncatedFileName.length();

            if(length > 50)
                truncatedFileName = "..." +  truncatedFileName.substring((length - 50) + 3);
            System.out.printf("\n%-50s\t\t%-30s", truncatedFileName, file.get(1));
        });

        return 0;
    }
}
