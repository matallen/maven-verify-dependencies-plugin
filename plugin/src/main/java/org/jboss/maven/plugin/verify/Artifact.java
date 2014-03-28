package org.jboss.maven.plugin.verify;

public class Artifact {
  private String groupId;
  private String artifactId;
  private String version;
  private long size;

  public String toString(){
    return "Artifact[groupId="+groupId+", artifactId="+artifactId+", version="+version+", size="+size+"]";
  }
  public Artifact(){}
  
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Artifact(String groupId, String artifactId, String version) {
    super();
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

}
