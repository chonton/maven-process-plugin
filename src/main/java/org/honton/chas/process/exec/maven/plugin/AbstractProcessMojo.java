package org.honton.chas.process.exec.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Stack;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractProcessMojo extends AbstractMojo {
  @Component
  protected MavenProject project;

  @Parameter(property = "exec.arguments")
  protected String[] arguments;

  @Parameter(property = "exec.environment")
  protected Map<String, String> environment;

  @Parameter(property = "exec.workingDir")
  protected String workingDir;

  @Parameter(property = "exec.name")
  protected String name;

  @Parameter(defaultValue = "${exec.healthCheckUrl}")
  protected HealthCheckUrl healthCheckUrl;

  public void setHealthCheckUrl(String url) throws MalformedURLException {
    this.healthCheckUrl = new HealthCheckUrl();
    this.healthCheckUrl.setUrl(new URL(url));
  }

  @Parameter(property = "exec.healthCheckValidateSsl", defaultValue = "true")
  protected boolean healthCheckValidateSsl;

  @Parameter(property = "exec.waitAfterLaunch", required = false, defaultValue = "30")
  protected int waitAfterLaunch;

  @Parameter(defaultValue = "false", property = "exec.waitForInterrupt")
  protected boolean waitForInterrupt;

  @Parameter(required = false, property = "exec.processLogFile")
  protected String processLogFile;

  @Parameter(defaultValue = "false", property = "exec.skip")
  protected boolean skip;

  protected static File ensureDirectory(File dir) {
    if (!dir.mkdirs() && !dir.isDirectory()) {
      throw new RuntimeException("couldn't create directories: " + dir);
    }
    return dir;
  }

  protected void sleepUntilInterrupted() throws IOException {
    getLog().info("Hit ENTER on the console to continue the build.");

    for (; ; ) {
      int ch = System.in.read();
      if (ch == -1 || ch == '\n') {
        break;
      }
    }
  }

  public AbstractProcessMojo() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        Stack<ExecProcess> processesStack = CrossMojoState.getProcesses(getPluginContext());
        // Let us stop any services that are still running
        if (!processesStack.empty()) {
          internalStopProcesses();
        }
      }
    });
  }

  protected void internalStopProcesses() {
    getLog().info("Stopping all processes ...");
    Stack<ExecProcess> processesStack = CrossMojoState.getProcesses(getPluginContext());
    while (!processesStack.isEmpty()) {
      ExecProcess execProcess = processesStack.pop();
      if (execProcess != null) {
        getLog().info("Stopping process: " + execProcess.getName());
        execProcess.destroy();
        execProcess.waitFor();
        getLog().info("Stopped process: " + execProcess.getName());
      }
    }
  }
}