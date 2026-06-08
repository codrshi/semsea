package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.config.SemseaConfig;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.LinkedHashSet;
import java.util.List;

@Command(
        name = "config",
        description = "Show or update the indexing rules in semsea.json.",
        mixinStandardHelpOptions = true,
        subcommands = { ConfigCommand.Show.class, ConfigCommand.Set.class }
)
public class ConfigCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(ConfigCommand.class);

    private static final int CONTENT_WIDTH = 76;
    private static final int LEFT_INDENT   = 4;

    @Spec
    CommandSpec commandSpec;

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'config' invoked (no subcommand, defaulting to show)");
        render();
        MetricCollector.print("CONFIG_COMMAND");
        log.info("'config' completed");
    }

    @Command(
            name = "show",
            description = "Display the configured ignoredDirectories, ignoredFiles, and supportedFiles.",
            mixinStandardHelpOptions = true
    )
    static class Show implements Runnable {

        private static final Logger log = LogManager.getLogger(Show.class);

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            TerminalRenderer.init(spec.commandLine().getOut());
            log.info("'config show' invoked");
            render();
            MetricCollector.print("CONFIG_COMMAND");
            log.info("'config show' completed");
        }
    }

    @Command(
            name = "set",
            description = "Append or replace values in ignoredDirectories, ignoredFiles, or supportedFiles.",
            mixinStandardHelpOptions = true
    )
    static class Set implements Runnable {

        private static final Logger log = LogManager.getLogger(Set.class);

        @Spec
        CommandSpec spec;

        @ArgGroup(exclusive = true, multiplicity = "0..1")
        DirsArg dirsArg;

        @ArgGroup(exclusive = true, multiplicity = "0..1")
        FilesArg filesArg;

        @ArgGroup(exclusive = true, multiplicity = "0..1")
        ExtsArg extsArg;

        static class DirsArg {
            @Option(names = "--add-ignored-dirs", split = ",", arity = "1..*", paramLabel = "<dir>",
                    description = "Append directories to ignoredDirectories (comma-separated).")
            java.util.Set<String> add;
            @Option(names = "--set-ignored-dirs", split = ",", arity = "1..*", paramLabel = "<dir>",
                    description = "Replace ignoredDirectories with the given values (comma-separated).")
            java.util.Set<String> replace;
        }

        static class FilesArg {
            @Option(names = "--add-ignored-files", split = ",", arity = "1..*", paramLabel = "<file>",
                    description = "Append file names to ignoredFiles (comma-separated).")
            java.util.Set<String> add;
            @Option(names = "--set-ignored-files", split = ",", arity = "1..*", paramLabel = "<file>",
                    description = "Replace ignoredFiles with the given values (comma-separated).")
            java.util.Set<String> replace;
        }

        static class ExtsArg {
            @Option(names = "--add-supported-files", split = ",", arity = "1..*", paramLabel = "<ext>",
                    description = "Append file extensions to supportedFiles (e.g. .rs,.go).")
            java.util.Set<String> add;
            @Option(names = "--set-supported-files", split = ",", arity = "1..*", paramLabel = "<ext>",
                    description = "Replace supportedFiles with the given extensions.")
            java.util.Set<String> replace;
        }

        @Override
        public void run() {
            TerminalRenderer.init(spec.commandLine().getOut());
            log.info("'config set' invoked");

            FieldUpdate dirs = resolve("ignoredDirectories", dirsArg  == null ? null : dirsArg.add,
                                                              dirsArg  == null ? null : dirsArg.replace);
            FieldUpdate files = resolve("ignoredFiles",       filesArg == null ? null : filesArg.add,
                                                              filesArg == null ? null : filesArg.replace);
            FieldUpdate exts = resolve("supportedFiles",      extsArg  == null ? null : extsArg.add,
                                                              extsArg  == null ? null : extsArg.replace);

            if(dirs == null && files == null && exts == null) {
                throw new SemseaException(
                        "No changes specified.",
                        "Provide at least one of --add-/--set-{ignored-dirs,ignored-files,supported-files}.");
            }

            SemseaConfig before = ConfigManager.getConfig();
            java.util.Set<String> dirsBefore  = snapshot(before.getIgnoredDirectories());
            java.util.Set<String> filesBefore = snapshot(before.getIgnoredFiles());
            java.util.Set<String> extsBefore  = snapshot(before.getSupportedFiles());

            ConfigManager.updateIndexingRules(
                    dirs  == null ? null : dirs.values,  dirs  != null && dirs.replace,
                    files == null ? null : files.values, files != null && files.replace,
                    exts  == null ? null : exts.values,  exts  != null && exts.replace);

            renderResult("Ignored directories",  dirsBefore,  ConfigManager.getConfig().getIgnoredDirectories(), dirs);
            renderResult("Ignored files",        filesBefore, ConfigManager.getConfig().getIgnoredFiles(),       files);
            renderResult("Supported extensions", extsBefore,  ConfigManager.getConfig().getSupportedFiles(),     exts);

            TerminalRenderer.println();
            MetricCollector.print("CONFIG_COMMAND");
            log.info("'config set' completed");
        }

        private static FieldUpdate resolve(String label, java.util.Set<String> add, java.util.Set<String> replace) {
            java.util.Set<String> cleanedAdd     = sanitize(add);
            java.util.Set<String> cleanedReplace = sanitize(replace);
            if(cleanedAdd == null && cleanedReplace == null) return null;
            if(cleanedReplace != null) {
                log.debug("Replacing {} with {} value(s)", label, cleanedReplace.size());
                return new FieldUpdate(cleanedReplace, true);
            }
            log.debug("Appending {} value(s) to {}", cleanedAdd.size(), label);
            return new FieldUpdate(cleanedAdd, false);
        }

        private static java.util.Set<String> sanitize(java.util.Set<String> values) {
            if(values == null || values.isEmpty()) return null;
            java.util.Set<String> cleaned = new LinkedHashSet<>();
            for(String v : values) {
                if(v == null) continue;
                String trimmed = v.trim();
                if(!trimmed.isEmpty()) {
                    cleaned.add(trimmed);
                }
            }
            return cleaned.isEmpty() ? null : cleaned;
        }

        private static java.util.Set<String> snapshot(java.util.Set<String> source) {
            return source == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source);
        }

        private static void renderResult(String label,
                                         java.util.Set<String> before,
                                         java.util.Set<String> after,
                                         FieldUpdate update) {
            if(update == null) return;

            int prevCount = before.size();
            int newCount  = after == null ? 0 : after.size();

            if(update.replace) {
                TerminalRenderer.println("  %s %s: %d %s %d  %s",
                        TerminalRenderer.green("+"),
                        TerminalRenderer.bold(label),
                        prevCount,
                        TerminalRenderer.dim("->"),
                        newCount,
                        TerminalRenderer.yellow("(replaced)"));
                return;
            }

            int added = newCount - prevCount;
            if(added <= 0) {
                TerminalRenderer.println("  %s %s: %d %s %s",
                        TerminalRenderer.dim("-"),
                        TerminalRenderer.bold(label),
                        newCount,
                        TerminalRenderer.dim("(no new values; all"),
                        TerminalRenderer.dim(update.values.size() + " already present)"));
                return;
            }
            TerminalRenderer.println("  %s %s: %d %s %d  %s",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(label),
                    prevCount,
                    TerminalRenderer.dim("->"),
                    newCount,
                    TerminalRenderer.green("(+" + added + " added)"));
        }

        private record FieldUpdate(java.util.Set<String> values, boolean replace) { }
    }

    private static void render() {
        SemseaConfig config = ConfigManager.getConfig();

        TerminalRenderer.println();
        TerminalRenderer.println("  %s",
                TerminalRenderer.bold("Indexing rules"));
        TerminalRenderer.println();

        renderSection("Ignored directories", config.getIgnoredDirectories(),
                "Names of directories skipped during indexing.");

        renderSection("Ignored files", config.getIgnoredFiles(),
                "Exact file names skipped during indexing.");

        renderSection("Supported extensions", config.getSupportedFiles(),
                "File extensions considered for indexing.");

        TerminalRenderer.println("  %s",
                TerminalRenderer.dim("Edit with 'semsea config set --help'."));
        TerminalRenderer.println();
    }

    private static void renderSection(String label, java.util.Set<String> items, String hint) {
        int count = items == null ? 0 : items.size();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold(label + ":"),
                TerminalRenderer.dim("(" + count + ")"));
        TerminalRenderer.println("  %s",
                TerminalRenderer.dim(hint));
        renderGrid(items);
        TerminalRenderer.println();
    }

    private static void renderGrid(java.util.Set<String> items) {
        if(items == null || items.isEmpty()) {
            TerminalRenderer.println("%s%s",
                    " ".repeat(LEFT_INDENT),
                    TerminalRenderer.dim("(empty)"));
            return;
        }

        List<String> sorted = items.stream().sorted().toList();
        int maxLen   = sorted.stream().mapToInt(String::length).max().orElse(0);
        int gap      = 2;
        int colWidth = maxLen + gap;
        int cols     = Math.max(1, CONTENT_WIDTH / colWidth);

        StringBuilder line = new StringBuilder();
        for(int i = 0; i < sorted.size(); i++) {
            if(i % cols == 0) {
                line.append(" ".repeat(LEFT_INDENT));
            }
            line.append(String.format("%-" + colWidth + "s", sorted.get(i)));
            boolean lastInRow  = i % cols == cols - 1;
            boolean lastOfAll  = i == sorted.size() - 1;
            if(lastInRow || lastOfAll) {
                TerminalRenderer.println("%s", stripTrailing(line.toString()));
                line.setLength(0);
            }
        }
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while(end > 0 && s.charAt(end - 1) == ' ') end--;
        return s.substring(0, end);
    }
}
