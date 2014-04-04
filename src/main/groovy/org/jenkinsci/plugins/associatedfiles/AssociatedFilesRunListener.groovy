package org.jenkinsci.plugins.associatedfiles
import java.util.logging.Logger

import hudson.Extension
import hudson.model.*
import hudson.model.listeners.RunListener
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig
import hudson.plugins.parameterizedtrigger.TriggerBuilder
import jenkins.model.Jenkins

@Extension
public class AssociatedFilesRunListener extends RunListener<AbstractBuild> {
  private final Logger log = Logger.getLogger(AssociatedFilesRunListener.class.getName());

  @Override
  public void onFinalized(AbstractBuild build) {
    AbstractProject origProj = build.getProject()
    if (origProj instanceof FreeStyleProject) {
      FreeStyleProject proj = (FreeStyleProject) origProj

      def afa = build.getActions().any { it.getClass().toString().equals(AssociatedFilesAction.class.toString()) }
      if (afa && proj.getBuilders().any { it.getClass().toString().equals(TriggerBuilder.class.toString()) }) {
        if (build.getResult().equals(Result.SUCCESS)) {
          def previousGoodBuild = build.getPreviousSuccessfulBuild()

          if (previousGoodBuild != null) {
            // If the previous build isn't kept forever, look at its children.
            if (!previousGoodBuild.isKeepLog()) {
              // Don't keep the children forever any more.
              getDownstreamBuilds(previousGoodBuild).each { b ->
                //log.warning("Un-marking previous child build ${b.getProject().name} #${b.number} as keep forever")
                b.keepLog(false)
              }
            }
          }

          // Now keep the children of *this* build forever
          getDownstreamBuilds(build).each { b ->
            //log.warning("Marking child build ${b.getProject().name} #${b.number} as keep forever")
            b.keepLog(true)
          }
        }
      }
    }
  }


  public void onDeleted(AbstractBuild build) {
    AssociatedFilesAction afa = build.getAction(AssociatedFilesAction.class)
    
    if (afa == null)
      return
    
    log.warning("Processing files/dirs to delete - raw version is ${afa.buildAssociatedFilesList}")
    afa.getBuildAssociatedFilesList().each { afName ->
      
      log.warning("Checking associated file ${afName}")
      def afFile = new File(afName)
      
      if (afFile.isDirectory()) {
          log.warning("Deleting directory ${afName}")
          if (!afFile.deleteDir()) {
            log.warning("Could not delete directory ${afName}")
          }
      }
      else if (afFile.isFile()) {
        log.warning("Deleting file ${afName}")
        if (!afFile.delete()) {
          log.warning("Could not delete file ${afName}")
        }
      }
    }
  }


  /**
   * Given a build, figures out what projects are downstream of it, and then finds any
   * builds of those downstream projects which have the original build as their upstream cause.
   */
  def getDownstreamBuilds(origBuild) {
    AbstractProject origProj = origBuild.getProject();

    //def dep = Jenkins.instance.getDependencyGraph();
    //println "origPRoj: ${dep.backward.keySet()}";

    def depBuilds = [];

    if (origProj.class == FreeStyleProject.class) {
      origProj.getBuilders().findAll { it.getClass().toString().equals(TriggerBuilder.class.toString()) }.each { t ->
        t.getConfigs().findAll { it.getClass().toString().equals(BlockableBuildTriggerConfig.class.toString()) }.each { c ->
          c.getProjects().split(/\,\s*/).collect { Jenkins.instance.getItem(it) }.each { pp ->
            pp.getBuilds().findAll { it.getCause(Cause.UpstreamCause.class)?.upstreamProject.equals(origProj.getName()) &&
                    it.getCause(Cause.UpstreamCause.class)?.upstreamBuild.equals(origBuild.getNumber()) }.each {
              depBuilds << it;
            }
          }
        }
      }
    }

    return depBuilds;
  }
}
