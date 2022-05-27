/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.tasks;

import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Incubating;
import org.gradle.api.file.Directory;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.options.OptionValues;
import org.gradle.buildinit.InsecureProtocolOption;
import org.gradle.buildinit.interrogator.Interrogator;
import org.gradle.buildinit.interrogator.model.Descriptor;
import org.gradle.buildinit.plugins.internal.BuildConverter;
import org.gradle.buildinit.plugins.internal.BuildInitializer;
import org.gradle.buildinit.plugins.internal.InitSettings;
import org.gradle.buildinit.plugins.internal.ProjectLayoutSetupRegistry;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl;
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework;
import org.gradle.buildinit.plugins.internal.modifiers.ComponentType;
import org.gradle.buildinit.plugins.internal.modifiers.Language;
import org.gradle.buildinit.plugins.internal.modifiers.ModularizationOption;
import org.gradle.internal.logging.text.TreeFormatter;
import org.gradle.work.DisableCachingByDefault;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.gradle.buildinit.plugins.internal.PackageNameBuilder.toPackageName;

/**
 * Generates a Gradle project structure.
 */
@DisableCachingByDefault(because = "Not worth caching")
public class InitBuild extends DefaultTask {
    private final Directory projectDir = getProject().getLayout().getProjectDirectory();
    private String type;
    private final Property<Boolean> splitProject = getProject().getObjects().property(Boolean.class);
    private String dsl;
    private final Property<Boolean> useIncubatingAPIs = getProject().getObjects().property(Boolean.class);
    private String testFramework;
    private String projectName;
    private String packageName;
    private final Property<InsecureProtocolOption> insecureProtocol = getProject().getObjects().property(InsecureProtocolOption.class);
    private final Property<String> template = getProject().getObjects().property(String.class);

    @Internal
    private ProjectLayoutSetupRegistry projectLayoutRegistry;

    /**
     * The desired type of project to generate, defaults to 'pom' if a 'pom.xml' is found in the project root and if no 'pom.xml' is found, it defaults to 'basic'.
     *
     * This property can be set via command-line option '--type'.
     */
    @Input
    public String getType() {
        return isNullOrEmpty(type) ? detectType() : type;
    }

    /**
     * Should the build be split into multiple subprojects?
     *
     * This property can be set via command-line option '--split-project'.
     *
     * @since 6.7
     */
    @Input
    @Optional
    @Option(option = "split-project", description = "Split functionality across multiple subprojects?")
    public Property<Boolean> getSplitProject() {
        return splitProject;
    }

    /**
     * The desired DSL of build scripts to create, defaults to 'groovy'.
     *
     * This property can be set via command-line option '--dsl'.
     *
     * @since 4.5
     */
    @Optional
    @Input
    public String getDsl() {
        return isNullOrEmpty(dsl) ? BuildInitDsl.GROOVY.getId() : dsl;
    }

    /**
     * Can the generated build use new and unstable features?
     *
     * When enabled, the generated build will use new patterns, APIs or features that
     * may be unstable between minor releases. Use this if you'd like to try out the
     * latest features of Gradle.
     *
     * By default, init will generate a build that uses stable features and behavior.
     *
     * @since 7.3
     */
    @Input
    @Optional
    @Incubating
    @Option(option = "incubating", description = "Allow the generated build to use new features and APIs")
    public Property<Boolean> getUseIncubating() {
        return useIncubatingAPIs;
    }

    /**
     * Target template URL.
     * @return The template URL.
     * @since 7.6
     */
    @Input
    @Optional
    @Incubating
    @Option(option = "template", description = "Return the template url to generate the target project from")
    public Property<String> getTemplate() {
        return template;
    }

    /**
     * The name of the generated project, defaults to the name of the directory the project is generated in.
     *
     * This property can be set via command-line option '--project-name'.
     *
     * @since 5.0
     */
    @Input
    public String getProjectName() {
        return projectName == null ? projectDir.getAsFile().getName() : projectName;
    }

    /**
     * The name of the package to use for generated source.
     *
     * This property can be set via command-line option '--package'.
     *
     * @since 5.0
     */
    @Input
    public String getPackageName() {
        return packageName == null ? "" : packageName;
    }

    /**
     * The test framework to be used in the generated project.
     *
     * This property can be set via command-line option '--test-framework'
     */
    @Nullable
    @Optional
    @Input
    public String getTestFramework() {
        return testFramework;
    }

    /**
     * How to handle insecure (http) URLs used for Maven Repositories.
     *
     * This property can be set via command-line option '--insecure-protocol'.  The default value is 'warn'.
     *
     * @since 7.3
     */
    @Input
    @Option(option = "insecure-protocol", description = "How to handle insecure URLs used for Maven Repositories.")
    @Incubating
    public Property<InsecureProtocolOption> getInsecureProtocol() {
        return insecureProtocol;
    }

    public ProjectLayoutSetupRegistry getProjectLayoutRegistry() {
        if (projectLayoutRegistry == null) {
            projectLayoutRegistry = getServices().get(ProjectLayoutSetupRegistry.class);
        }

        return projectLayoutRegistry;
    }

    @TaskAction
    public void setupProjectLayout() throws Exception {
        UserInputHandler inputHandler = getServices().get(UserInputHandler.class);
        ProjectLayoutSetupRegistry projectLayoutRegistry = getProjectLayoutRegistry();


        if (getTemplate().isPresent()) {
            String url = getTemplate().get();
            getLogger().lifecycle("Cloning git repository: " + url);
            materializeTemplate(url);
            return;
        }

        BuildInitializer initDescriptor = null;
        if (isNullOrEmpty(type)) {
            BuildConverter converter = projectLayoutRegistry.getBuildConverter();
            if (converter.canApplyToCurrentDirectory(projectDir)) {
                if (inputHandler.askYesNoQuestion("Found a " + converter.getSourceBuildDescription() + " build. Generate a Gradle build from this?", true)) {
                    initDescriptor = converter;
                }
            }
            if (initDescriptor == null) {
                ComponentType componentType = inputHandler.selectOption("Select type of project to generate", projectLayoutRegistry.getComponentTypes(), projectLayoutRegistry.getDefault().getComponentType());
                List<Language> languages = projectLayoutRegistry.getLanguagesFor(componentType);
                if (languages.size() == 1) {
                    initDescriptor = projectLayoutRegistry.get(componentType, languages.get(0));
                } else {
                    if (!languages.contains(Language.JAVA)) {
                        // Not yet implemented
                        throw new UnsupportedOperationException();
                    }
                    Language language = inputHandler.selectOption("Select implementation language", languages, Language.JAVA);
                    initDescriptor = projectLayoutRegistry.get(componentType, language);
                }
            }
        } else {
            initDescriptor = projectLayoutRegistry.get(type);
        }

        ModularizationOption modularizationOption;
        if (splitProject.isPresent()) {
            modularizationOption = splitProject.get() ? ModularizationOption.WITH_LIBRARY_PROJECTS : ModularizationOption.SINGLE_PROJECT;
        } else if (initDescriptor.getModularizationOptions().size() == 1) {
            modularizationOption = initDescriptor.getModularizationOptions().iterator().next();
        } else if (!isNullOrEmpty(type)) {
            modularizationOption = ModularizationOption.SINGLE_PROJECT;
        } else {
            modularizationOption = inputHandler.selectOption("Split functionality across multiple subprojects?",
                initDescriptor.getModularizationOptions(), ModularizationOption.SINGLE_PROJECT);
        }

        BuildInitDsl dsl;
        if (isNullOrEmpty(this.dsl)) {
            dsl = initDescriptor.getDefaultDsl();
            if (initDescriptor.getDsls().size() > 1) {
                dsl = inputHandler.selectOption("Select build script DSL", initDescriptor.getDsls(), dsl);
            }
        } else {
            dsl = BuildInitDsl.fromName(getDsl());
            if (!initDescriptor.getDsls().contains(dsl)) {
                throw new GradleException("The requested DSL '" + getDsl() + "' is not supported for '" + initDescriptor.getId() + "' build type");
            }
        }

        boolean useIncubatingAPIs;
        if (this.useIncubatingAPIs.isPresent()) {
            useIncubatingAPIs = this.useIncubatingAPIs.get();
        } else {
            useIncubatingAPIs = inputHandler.askYesNoQuestion("Generate build using new APIs and behavior (some features may change in the next minor release)?", false);
        }

        BuildInitTestFramework testFramework = null;
        if (modularizationOption == ModularizationOption.WITH_LIBRARY_PROJECTS) {
            // currently we only support JUnit5 tests for this combination
            testFramework = BuildInitTestFramework.JUNIT_JUPITER;
        } else if (isNullOrEmpty(this.testFramework)) {
            testFramework = initDescriptor.getDefaultTestFramework();
            if (initDescriptor.getTestFrameworks().size() > 1) {
                testFramework = inputHandler.selectOption("Select test framework", initDescriptor.getTestFrameworks(), testFramework);
            }
        } else {
            for (BuildInitTestFramework candidate : initDescriptor.getTestFrameworks()) {
                if (this.testFramework.equals(candidate.getId())) {
                    testFramework = candidate;
                    break;
                }
            }
            if (testFramework == null) {
                TreeFormatter formatter = new TreeFormatter();
                formatter.node("The requested test framework '" + getTestFramework() + "' is not supported for '" + initDescriptor.getId() + "' build type. Supported frameworks");
                formatter.startChildren();
                for (BuildInitTestFramework framework : initDescriptor.getTestFrameworks()) {
                    formatter.node("'" + framework.getId() + "'");
                }
                formatter.endChildren();
                throw new GradleException(formatter.toString());
            }
        }

        String projectName = this.projectName;
        if (initDescriptor.supportsProjectName()) {
            if (isNullOrEmpty(projectName)) {
                projectName = inputHandler.askQuestion("Project name", getProjectName());
            }
        } else if (!isNullOrEmpty(projectName)) {
            throw new GradleException("Project name is not supported for '" + initDescriptor.getId() + "' build type.");
        }

        String packageName = this.packageName;
        if (initDescriptor.supportsPackage()) {
            if (isNullOrEmpty(packageName)) {
                packageName = inputHandler.askQuestion("Source package", toPackageName(projectName).toLowerCase(Locale.US));
            }
        } else if (!isNullOrEmpty(packageName)) {
            throw new GradleException("Package name is not supported for '" + initDescriptor.getId() + "' build type.");
        }

        if (!isNullOrEmpty(packageName)) {
            if (!SourceVersion.isName(packageName)) {
                throw new GradleException("Package name: '" + packageName + "' is not valid - it may contain invalid characters or reserved words.");
            }
        }

        List<String> subprojectNames = initDescriptor.getComponentType().getDefaultProjectNames();
        InitSettings settings = new InitSettings(projectName, useIncubatingAPIs, subprojectNames,
            modularizationOption, dsl, packageName, testFramework, insecureProtocol.get(), projectDir);
        initDescriptor.generate(settings);

        initDescriptor.getFurtherReading(settings).ifPresent(link -> getLogger().lifecycle("Get more help with your project: {}", link));
    }

    private void materializeTemplate(String url) throws Exception {
        File localRepoDir = getProject().getLayout().getBuildDirectory().dir("tmp/gitClone").get().getAsFile();
        File targetDir = projectDir.getAsFile();
        cloneRepositoryTo(url, localRepoDir);
        Configuration configuration = loadFreemarkerConfiguration(localRepoDir);
        File optionsFile = new File(localRepoDir, "templateOptions.json");
        Map<String, Object> data = loadTemplateData(optionsFile);
        processTemplates(targetDir, localRepoDir, configuration, data);
        FileUtils.deleteDirectory(localRepoDir);
    }

    private void processTemplates(File targetDir, File localRepoDir, Configuration freemarkerConfig, Map<String, Object> data) throws IOException, TemplateException {
        for (File file : FileUtils.listFiles(localRepoDir, null, true)) {
            if (file.isFile()) {
                URI fileUri = file.toURI();
                URI baseUri = localRepoDir.toURI();
                String relativePath = baseUri.relativize(fileUri).getPath();
                if (!isIgnored(relativePath)) {
                    processTemplate(targetDir, freemarkerConfig, data, file, baseUri, localRepoDir);
                }
            }
        }
    }

    private void processTemplate(File targetDir, Configuration freemarkerConfig, Map<String, Object> data, File file, URI baseUri, File localRepoDir) throws IOException, TemplateException {
        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        boolean hasMetadata = false;
        int endLine = -1;
        if (lines.size() > 0 && lines.get(0).startsWith("<#GradleTemplate>")) {
            hasMetadata = true;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).equals("</#GradleTemplate>"))  {
                    endLine = i;
                    break;
                }
            }
        }
        if (hasMetadata && endLine == -1) {
            throw new RuntimeException("No </#GradleTemplate> tag found for <#GradleTemplate>");
        }

        HashMap<String, Object> finalData = new HashMap<>(data);
        if (hasMetadata) {
            String paramTemplateName = file.getName() + ".GradleTemplate.params.txt.template"; // TODO use relative path to avoid template caching
            File paramsTemplateFile = new File(localRepoDir, paramTemplateName); // TODO check for collisions
            if (paramsTemplateFile.exists()) {
                paramsTemplateFile.delete();
                FileUtils.touch(paramsTemplateFile);
            }
            FileUtils.writeLines(paramsTemplateFile, lines.subList(1, endLine));

            Template paramsTemplate = freemarkerConfig.getTemplate(paramTemplateName);
            StringWriter writer = new StringWriter();
            paramsTemplate.process(finalData, writer, null);
            getLogger().lifecycle(file.getName() + ": " + writer.toString());
            Properties props = new Properties();
            props.load(new StringReader(writer.toString()));
            getLogger().lifecycle("Properties for " + file.getName() + ": " + props);
            for (Object key : props.keySet()) {
                finalData.put((String) key, props.get(key));
            }
            paramsTemplateFile.delete();

            FileUtils.writeLines(file, lines.subList(endLine + 1, lines.size()));
        }

        String targetFileName = file.getName().substring(0, file.getName().length() - 9);
        URI templateUri = file.toURI();
        URI generatedFileUri = new File(file.getParentFile(), targetFileName).toURI();
        String fileName = (String) finalData.get("targetFile");
        String generatedFileRelativePath =  fileName == null ? baseUri.relativize(generatedFileUri).getPath() : fileName;
        String templateRelativePath = baseUri.relativize(templateUri).getPath();
        Template template = freemarkerConfig.getTemplate(templateRelativePath);
        File targetFile = new File(targetDir, generatedFileRelativePath);
        targetFile.getParentFile().mkdirs();
        FileUtils.touch(targetFile);
        Writer out = new OutputStreamWriter(new FileOutputStream(targetFile));
        template.process(finalData, out, null);
    }

    private static boolean isIgnored(String relativePath) {
        return relativePath.startsWith(".git") || relativePath.startsWith(".gradle") || Arrays.asList("gradlew", "gradlew.bat").contains(relativePath) || relativePath.equals("templateOptions.json");
    }

    private Map<String, Object> loadTemplateData(File optionsFile) throws IOException {
        // TODO no options file
        Descriptor descriptor = Descriptor.read(optionsFile);
        return  new Interrogator(getServices().get(UserInputHandler.class)).askQuestions(descriptor.getQuestions());
    }

    private static Configuration loadFreemarkerConfiguration(File localRepoDir) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        cfg.setDirectoryForTemplateLoading(localRepoDir);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }

    private void cloneRepositoryTo(String url, File localRepoDir) throws GitAPIException {
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(localRepoDir)
            .call();
    }

    @Option(option = "type", description = "Set the type of project to generate.")
    public void setType(String type) {
        this.type = type;
    }

    @OptionValues("type")
    public List<String> getAvailableBuildTypes() {
        return getProjectLayoutRegistry().getAllTypes();
    }

    /**
     * Set the build script DSL to be used.
     *
     * @since 4.5
     */
    @Option(option = "dsl", description = "Set the build script DSL to be used in generated scripts.")
    public void setDsl(String dsl) {
        this.dsl = dsl;
    }

    /**
     * Available build script DSLs to be used.
     *
     * @since 4.5
     */
    @OptionValues("dsl")
    public List<String> getAvailableDSLs() {
        return BuildInitDsl.listSupported();
    }

    /**
     * Set the test framework to be used.
     */
    @Option(option = "test-framework", description = "Set the test framework to be used.")
    public void setTestFramework(@Nullable String testFramework) {
        this.testFramework = testFramework;
    }

    /**
     * Available test frameworks.
     */
    @OptionValues("test-framework")
    public List<String> getAvailableTestFrameworks() {
        return BuildInitTestFramework.listSupported();
    }

    /**
     * Set the project name.
     *
     * @since 5.0
     */
    @Option(option = "project-name", description = "Set the project name.")
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Set the package name.
     *
     * @since 5.0
     */
    @Option(option = "package", description = "Set the package for source files.")
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    void setProjectLayoutRegistry(ProjectLayoutSetupRegistry projectLayoutRegistry) {
        this.projectLayoutRegistry = projectLayoutRegistry;
    }

    private String detectType() {
        ProjectLayoutSetupRegistry projectLayoutRegistry = getProjectLayoutRegistry();
        BuildConverter buildConverter = projectLayoutRegistry.getBuildConverter();
        if (buildConverter.canApplyToCurrentDirectory(projectDir)) {
            return buildConverter.getId();
        }
        return projectLayoutRegistry.getDefault().getId();
    }
}
