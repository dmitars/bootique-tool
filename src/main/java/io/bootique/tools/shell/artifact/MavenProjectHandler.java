package io.bootique.tools.shell.artifact;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import com.google.inject.Inject;
import io.bootique.command.CommandOutcome;
import io.bootique.tools.shell.Shell;
import io.bootique.tools.shell.template.Properties;
import io.bootique.tools.shell.template.processor.JavaPackageProcessor;
import io.bootique.tools.shell.template.processor.MavenProcessor;
import io.bootique.tools.shell.template.source.SourceSet;
import io.bootique.tools.shell.template.source.SourceTemplateFilter;

public class MavenProjectHandler extends ArtifactHandler {

    private static final String DEFAULT_VERSION = "1.0-SNAPSHOT";

    @Inject
    private NameParser nameParser;

    @Inject
    private Shell shell;

    public MavenProjectHandler() {
        {
            SourceSet sourceSet = new SourceSet();
            sourceSet.setIncludes(new SourceTemplateFilter("**/*.java"));
            sourceSet.setProcessors(new JavaPackageProcessor());
            sourceSets.add(sourceSet);
        }

        {
            SourceSet sourceSet = new SourceSet();
            sourceSet.setIncludes(new SourceTemplateFilter("pom.xml"));
            sourceSet.setProcessors(new MavenProcessor());
            sourceSets.add(sourceSet);
        }
    }

    @Override
    public CommandOutcome validate(String name) {
        NameParser.ValidationResult validationResult = nameParser.validate(name);
        if(!validationResult.isValid()) {
            return CommandOutcome.failed(-1, validationResult.getMessage());
        }
        return CommandOutcome.succeeded();
    }

    @Override
    public CommandOutcome handle(String name) {
        NameParser.NameComponents components = nameParser.parse(name);

        Path outputRoot = Paths.get(System.getProperty("user.dir")).resolve(components.getName());
        if(Files.exists(outputRoot)) {
            return CommandOutcome.failed(-1, "Directory '" + components.getName() + "' already exists");
        }

        Properties properties = Properties.builder()
                .with("java.package", components.getJavaPackage())
                .with("maven.groupId", components.getJavaPackage())
                .with("maven.artifactId", components.getName())
                .with("maven.version", DEFAULT_VERSION)
                .with("project.name", components.getName())
                .build();

        shell.println("@|green   <|@ Generating new project @|bold " + components.getName() + "|@ ...");
        processTemplates(outputRoot, properties);
        shell.println("@|green   <|@ done.");

        return CommandOutcome.succeeded();
    }

    @Override
    protected Collection<String> getTemplateNames() {
        return Arrays.asList(
                "src/main/java/example/Application.java",
                "src/test/java/example/ApplicationTest.java",
                "pom.xml"
        );
    }

    @Override
    protected String getTemplateBase() {
        return "templates/maven-project/";
    }
}
