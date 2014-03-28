package org.jboss.maven.plugin.verify;

public interface ArtifactFinder {
  public Artifact find(String artifactId, String version) throws Exception;
}
