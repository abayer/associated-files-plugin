package org.jenkinsci.plugins.associatedfiles

import hudson.model.Action
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class AssociatedFilesAction implements Action {
  String buildAssociatedFiles
  
  public AssociatedFilesAction(String buildAssociatedFiles) {
    this.buildAssociatedFiles = buildAssociatedFiles
  }
  
  public String getIconFileName() {
    return null
  }
  
  public String getDisplayName() {
    return null
  }
  
  public String getUrlName() {
    return "associatedFiles"
  }
  
  @Exported(visibility=2)
  public getBuildAssociatedFilesList() {
    return buildAssociatedFiles.split(',').collect { it.trim() }
  }
}
