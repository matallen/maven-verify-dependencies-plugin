package org.jboss.maven.plugin.verify;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal run
 * @author mallen@redhat.com
 */
public class Verify extends AbstractMojo {
  /** @parameter expression="${run.debug}" */
  private String debug="false";
  /** @parameter expression="${run.artifactBinary}" */
  private String artifactBinary=null;
  /** @parameter expression="${run.targetContainerVersion}" */
  private String targetContainerVersion=null;
  /** @parameter expression="${run.repository}" */
  private String repository;
  /** @parameter expression="${run.artifactVersionRegExp}" */
  private String artifactVersionRegExp;
  
  private boolean isDebug(){ return "true".equalsIgnoreCase(debug); }
  
  public enum Config{XML,HTTP,INDEX};
  private Config config;

//  @Override
//  public void execute() throws MojoExecutionException{
  public void execute() throws MojoExecutionException, MojoFailureException {
    
    if (isDebug()){
      System.out.println("Verify: Starting...");
      System.out.println("Verify: repository = "+ repository);
    }
    
    if (repository.toLowerCase().endsWith("xml")){
      config=Config.XML;
    }else if (repository.toLowerCase().startsWith("http")){ // including https
      config=Config.INDEX;
    }
    
    if (isDebug()) System.out.println("Verify: config = "+ config);
    if (isDebug()) System.out.println("Verify: artifactVersionRegExp = "+ artifactVersionRegExp);
    
    ZipFile includedBinaryArchive=null;
    Pattern includedBinaryArchiveReader=Pattern.compile(artifactVersionRegExp);
    try{
      includedBinaryArchive=new ZipFile(new File("target/"+artifactBinary));
      long totalInBytes=0;
      
      if (isDebug()) System.out.println("Verify: includedBinaryArchive = "+ includedBinaryArchive.getName());
      
      StringBuffer sb=new StringBuffer();
      
      ArtifactFinder finder=null;
      switch (config){
        case INDEX: finder=new IndexArtifactFinder(isDebug(), repository); break;
        case XML:   finder=new SaxArtifactFinder(isDebug(), repository);   break;
      }
      
      if (finder==null)
        throw new RuntimeException("must set config to specify artifact library");
      
      if (isDebug()) System.out.println("Verify: finder = "+ finder.getClass().getSimpleName());
      
      for (Enumeration list = includedBinaryArchive.entries(); list.hasMoreElements(); ) {
        java.util.zip.ZipEntry ze=(java.util.zip.ZipEntry)list.nextElement();
        
        
        if (ze.getName().toLowerCase().matches(artifactVersionRegExp)){
          if (isDebug()) System.out.println("Verify: found ["+ze.getName()+"]");
//        if (ze.getName().toLowerCase().matches(".+\\..ar$")){
          
          Matcher m=includedBinaryArchiveReader.matcher(new File(ze.getName()).getName());
          if (m.find()){
            String artifactId=m.group(1);
            String version=m.group(2);
            if (isDebug()) System.out.println("Verify: detected (artifactId="+artifactId+", groupId="+version+") from file ("+ze.getName()+")");
            
            Artifact replacementArtifact=finder.find(artifactId, targetContainerVersion);
            
//            if (replacementArtifact!=null && !version.equals(replacementArtifact.getVersion())){
            if (replacementArtifact!=null){
              totalInBytes+=ze.getCompressedSize();
              replacementArtifact.setSize(ze.getCompressedSize());
              sb.append("<dependency>\n");
              sb.append("  <groupId>"+replacementArtifact.getGroupId()+"</groupId>\n");
              sb.append("  <artifactId>"+replacementArtifact.getArtifactId()+"</artifactId>\n");
              sb.append("  <version>"+replacementArtifact.getVersion()+"</version>\n");
              sb.append("  <scope>provided</scope> <!--  made 'provided' scope to save "+(ze.getCompressedSize()/1000)+"kBytes-->\n");
              sb.append("</dependency>\n");
            }
          }
          
        }
      }
      if (totalInBytes>0){
        System.out.println("XXXXXXXXXXXXXXXXXXX");
        System.out.println(sb.toString().trim());
        System.out.println("Make the above changes to save "+(totalInBytes/1000)+" kBytes, identify platform incompatibilities, and reduce deployment times!!");
        System.out.println("XXXXXXXXXXXXXXXXXXX");
      }
    }catch(Exception e){
      throw new MojoExecutionException(e.getMessage(), e);
    }finally{
      try{
        if (null!=includedBinaryArchive) includedBinaryArchive.close();
      }catch(IOException e){
        throw new MojoExecutionException(e.getMessage(), e);
      }
    }
  }
  
}
