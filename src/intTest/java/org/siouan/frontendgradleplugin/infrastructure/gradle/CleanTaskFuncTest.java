package org.siouan.frontendgradleplugin.infrastructure.gradle;

import static org.siouan.frontendgradleplugin.test.util.GradleBuildAssertions.assertTaskIgnored;
import static org.siouan.frontendgradleplugin.test.util.GradleBuildAssertions.assertTaskSkipped;
import static org.siouan.frontendgradleplugin.test.util.GradleBuildAssertions.assertTaskSuccess;
import static org.siouan.frontendgradleplugin.test.util.GradleBuildAssertions.assertTaskUpToDate;
import static org.siouan.frontendgradleplugin.test.util.GradleBuildFiles.createBuildFile;
import static org.siouan.frontendgradleplugin.test.util.GradleHelper.runGradle;
import static org.siouan.frontendgradleplugin.test.util.Resources.getResourcePath;
import static org.siouan.frontendgradleplugin.test.util.Resources.getResourceUrl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.gradle.api.plugins.BasePlugin;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.siouan.frontendgradleplugin.FrontendGradlePlugin;
import org.siouan.frontendgradleplugin.test.util.FrontendMapBuilder;

/**
 * Functional tests to verify the {@link CleanTask} integration in a Gradle build. Test cases uses fake Node/Yarn
 * distributions, to avoid the download overhead. The 'yarn' and 'npm' executables in these distributions simply call
 * the 'node' executable with the same arguments.
 */
class CleanTaskFuncTest {

    @TempDir
    Path projectDirectoryPath;

    @Test
    void shouldDoNothingWhenScriptIsNotDefined() throws IOException {
        Files.copy(getResourcePath("package-npm.json"), projectDirectoryPath.resolve("package.json"));
        final FrontendMapBuilder frontendMapBuilder = new FrontendMapBuilder()
            .nodeVersion("14.15.4")
            .nodeDistributionUrl(getResourceUrl("node-v14.15.4.zip"));
        createBuildFile(projectDirectoryPath, frontendMapBuilder.toMap());

        final BuildResult result = runGradle(projectDirectoryPath, FrontendGradlePlugin.CLEAN_TASK_NAME);

        assertTaskIgnored(result, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskIgnored(result, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskIgnored(result, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSkipped(result, FrontendGradlePlugin.CLEAN_TASK_NAME);
    }

    @Test
    void shouldCleanWithoutFrontendTasks() throws IOException {
        Files.copy(getResourcePath("package-npm.json"), projectDirectoryPath.resolve("package.json"));
        final FrontendMapBuilder frontendMapBuilder = new FrontendMapBuilder()
            .nodeVersion("14.15.4")
            .nodeDistributionUrl(getResourceUrl("node-v14.15.4.zip"));
        createBuildFile(projectDirectoryPath, frontendMapBuilder.toMap());

        final BuildResult result = runGradle(projectDirectoryPath, BasePlugin.CLEAN_TASK_NAME);

        assertTaskIgnored(result, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskIgnored(result, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskIgnored(result, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSkipped(result, FrontendGradlePlugin.CLEAN_TASK_NAME);
        assertTaskUpToDate(result, BasePlugin.CLEAN_TASK_NAME);
    }

    @Test
    void shouldCleanFrontendWithNpmOrYarn() throws IOException {
        Files.copy(getResourcePath("package-npm.json"), projectDirectoryPath.resolve("package.json"));
        final FrontendMapBuilder frontendMapBuilder = new FrontendMapBuilder()
            .nodeVersion("14.15.4")
            .nodeDistributionUrl(getResourceUrl("node-v14.15.4.zip"))
            .cleanScript("run clean");
        createBuildFile(projectDirectoryPath, frontendMapBuilder.toMap());

        final BuildResult result1 = runGradle(projectDirectoryPath, BasePlugin.CLEAN_TASK_NAME);

        assertTaskSuccess(result1, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskSkipped(result1, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskSuccess(result1, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSuccess(result1, FrontendGradlePlugin.CLEAN_TASK_NAME);
        assertTaskSuccess(result1, BasePlugin.CLEAN_TASK_NAME);

        final BuildResult result2 = runGradle(projectDirectoryPath, BasePlugin.CLEAN_TASK_NAME);

        assertTaskUpToDate(result2, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskSkipped(result2, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskSuccess(result2, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSuccess(result1, FrontendGradlePlugin.CLEAN_TASK_NAME);
        assertTaskUpToDate(result2, BasePlugin.CLEAN_TASK_NAME);

        Files.deleteIfExists(projectDirectoryPath.resolve("package-lock.json"));
        Files.copy(getResourcePath("package-yarn.json"), projectDirectoryPath.resolve("package.json"),
            StandardCopyOption.REPLACE_EXISTING);
        frontendMapBuilder
            .yarnEnabled(true)
            .yarnVersion("1.22.10")
            .yarnDistributionUrl(getResourceUrl("yarn-v1.22.10.tar.gz"));
        createBuildFile(projectDirectoryPath, frontendMapBuilder.toMap());

        final BuildResult result3 = runGradle(projectDirectoryPath, BasePlugin.CLEAN_TASK_NAME);

        assertTaskUpToDate(result3, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskSuccess(result3, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskSuccess(result3, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSuccess(result3, FrontendGradlePlugin.CLEAN_TASK_NAME);
        assertTaskSuccess(result3, BasePlugin.CLEAN_TASK_NAME);

        final BuildResult result4 = runGradle(projectDirectoryPath, BasePlugin.CLEAN_TASK_NAME);

        assertTaskUpToDate(result4, FrontendGradlePlugin.NODE_INSTALL_TASK_NAME);
        assertTaskUpToDate(result4, FrontendGradlePlugin.YARN_INSTALL_TASK_NAME);
        assertTaskSuccess(result4, FrontendGradlePlugin.INSTALL_TASK_NAME);
        assertTaskSuccess(result4, FrontendGradlePlugin.CLEAN_TASK_NAME);
        assertTaskUpToDate(result4, BasePlugin.CLEAN_TASK_NAME);
    }
}
