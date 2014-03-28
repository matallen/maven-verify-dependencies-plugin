package org.jboss.maven.plugin.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxArtifactFinder extends DefaultHandler implements ArtifactFinder {
  private StringBuffer sb=new StringBuffer();
  private Map<String, Artifact> result=new HashMap<String, Artifact>();
  private Artifact _new=null;
  private final String[] repositories;
  private Map<String, Artifact> parsedArtifacts;
  private boolean isDebug;
  
  public SaxArtifactFinder(){repositories=null;};
  public SaxArtifactFinder(boolean isDebug, String repositories){
    this.repositories=repositories.split(",");
    this.isDebug=isDebug;
  }
  
  public Artifact find(String artifactId, String version) throws Exception {
    
    parsedArtifacts=new HashMap<String, Artifact>();
    for(String r:repositories){
      InputStream is=this.getClass().getClassLoader().getResource(r).openStream();
      String repoContent=IOUtils.toString(is);
      parsedArtifacts.putAll(SaxArtifactFinder.parse(repoContent));
    }
    
    if (isDebug){
      System.out.println("SaxArtifactFinder: parsed artifacts.size= "+parsedArtifacts.size());
      System.out.println("SaxArtifactFinder: returning '"+artifactId+version+"' key = "+ parsedArtifacts.get(artifactId+version));
    }
    
    return parsedArtifacts.get(artifactId+version);
  }
  
  public static Map<String, Artifact> parse(String xml) throws Exception{
    SAXParser parser=new SAXParserFactoryImpl().newSAXParser();
    SaxArtifactFinder h=new SaxArtifactFinder();
    parser.parse(new ByteArrayInputStream(xml.getBytes()), h);
    return h.getResult();
  }
  
  private Map<String,Artifact> getResult(){
    return result;
  }
  
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (qName.equals("dependency")){
      _new=new Artifact();
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("dependencies".equals(qName)){
      return;
    }
    if (qName.equals("dependency")){
      result.put(_new.getArtifactId()+_new.getVersion(), _new);
      _new=null;
    }else if (qName.equals("groupId")){
      _new.setGroupId(sb.toString().trim());
      sb.setLength(0);
    }else if (qName.equals("artifactId")){
      _new.setArtifactId(sb.toString().trim());
      sb.setLength(0);
    }else if (qName.equals("version")){
      _new.setVersion(sb.toString().trim());
      sb.setLength(0);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    for (int i = start; i < start+length; i++) {
      sb.append(ch[i]);
    }
  }
}
