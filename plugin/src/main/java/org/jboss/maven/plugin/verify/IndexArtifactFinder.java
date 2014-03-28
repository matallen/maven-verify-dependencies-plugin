package org.jboss.maven.plugin.verify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

public class IndexArtifactFinder implements ArtifactFinder{
  private final PlexusContainer plexusContainer;
  private final Indexer indexer;
  private final IndexUpdater indexUpdater;
  private final Wagon httpWagon;
  private final String repository; // "http://repo1.maven.org/maven2"
  private final boolean isDebug;
  
  public IndexArtifactFinder(boolean isDebug, String repository){
//  public IndexArtifactFinder(String repository) throws PlexusContainerException, ComponentLookupException{
    
    if (isDebug) System.out.println("IndexArtifactFinder: Initialising...");
    
    try{
      this.plexusContainer = new DefaultPlexusContainer();
      this.isDebug=isDebug;
      // lookup the indexer components from plexus
      System.out.println("plexusContainer = "+ plexusContainer);
//      plexusContainer.getLogger().debug("A PLEXUS CONTAINER LOG MESSAGE");
//      plexusContainer.initialize();
//      System.out.println("initialised plexus container ? what did that do?");
//      plexusContainer.start();
//      System.out.println("started plexus container ? what did that do?");
      
//      try{System.out.println(plexusContainer.lookupMap(MavenProjectBuilder.ROLE));}catch(Exception e){e.printStackTrace();}
      this.indexer = plexusContainer.lookup(Indexer.class);
      this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
      this.httpWagon = plexusContainer.lookup(Wagon.class, "http");
      
//      this.indexer = (Indexer)plexusContainer.lookup(Indexer.class.getName());
//      this.indexUpdater = (IndexerUpdater)plexusContainer.lookup(IndexUpdater.class.getName());
//      this.indexUpdater = null;
//      this.httpWagon = (Wagon)plexusContainer.lookup(Wagon.class.getName(), "http");
//      BasicComponentConfigurator
//      ComponentConfigurator
      
//      this.indexer = (Indexer) plexusContainer.lookup( Indexer.class.getName() );
//      
//      this.indexUpdater = (IndexUpdater) plexusContainer.lookup( IndexUpdater.class.getName() );
//      // lookup wagon used to remotely fetch index
//      this.httpWagon = (Wagon) plexusContainer.lookup( Wagon.class.getName(), "http" );
      this.repository=repository;
      if (isDebug) System.out.println("IndexArtifactFinder: Initialised");
    }catch(Exception e){
      throw new RuntimeException("Unable to initialise IndexArtifactFinder", e);
    }
  }
  
  public Artifact find(String artifactId, String version) {
//  public Artifact find(String artifactId, String version) throws IOException, ComponentLookupException{
//    return null;
    try{
      File localCache = new File("target/central-cache");
      File indexDir = new File("target/central-index");
      
      List<IndexCreator> indexers = new ArrayList<IndexCreator>();
      indexers.add((IndexCreator)plexusContainer.lookup(IndexCreator.class.getName(), "min"));
      indexers.add((IndexCreator)plexusContainer.lookup(IndexCreator.class.getName(), "jarContent"));
      indexers.add((IndexCreator)plexusContainer.lookup(IndexCreator.class.getName(), "maven-plugin"));
      
      
      IndexingContext centralContext = indexer.createIndexingContext("central-context", "central", localCache, indexDir, repository, null, true, true, indexers);
      
      
    // downloading/updating index
    
      System.out.println( " == Updating Index ==" );
      System.out.println( "This might take a while on first run, so please be patient!" );
      TransferListener listener = new AbstractTransferListener() {
        public void transferStarted(TransferEvent transferEvent) {   System.out.print("  Downloading " + transferEvent.getResource().getName()); }
        public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {}
        public void transferCompleted(TransferEvent transferEvent) { System.out.println(" - Done"); }
      };
      ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);
     
      Date centralContextCurrentTimestamp = centralContext.getTimestamp();
      IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
      IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
      if (updateResult.isFullUpdate()) {
        System.out.println("Full update happened!");
      } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
        System.out.println("No update needed, index is up to date!");
      } else {
        System.out.println("Incremental update happened, change covered " + centralContextCurrentTimestamp + " - " + updateResult.getTimestamp() + " period.");
      }
      
      System.out.println(" == Using index ==");
      BooleanQuery bq = new BooleanQuery();
      bq.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("maven-plugin")), Occur.MUST);
      bq.add(indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression("org.apache.maven.plugins")), Occur.MUST);
      GroupedSearchResponse response = indexer.searchGrouped( new GroupedSearchRequest( bq, new GAGrouping() ) );
     
      for (Map.Entry<String, ArtifactInfoGroup> entry : response.getResults().entrySet()) {
        ArtifactInfo ai = entry.getValue().getArtifactInfos().iterator().next();
        System.out.println("* Plugin " + ai.artifactId);
        System.out.println("  Latest version:  " + ai.version);
//        System.out.println(StringUtils.isBlank(ai.description) ? "No description in plugin's POM." : StringUtils.abbreviate(ai.description, 60));
        System.out.println();
      }
//      
//      // search for artifact in index
////      final GenericVersionScheme versionScheme = new GenericVersionScheme();
////      final String versionString = "1.5.0";
////      final Version version = versionScheme.parseVersion(versionString);
//     
////      // construct the query for known GA
////      final Query groupIdQ = indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression("org.sonatype.nexus"));
////      final Query artifactIdQ = indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression("nexus-api"));
////      final BooleanQuery query = new BooleanQuery();
////      query.add(groupIdQ, Occur.MUST);
////      query.add(artifactIdQ, Occur.MUST);
////   
////      // we want "jar" artifacts only
////      query.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("jar")), Occur.MUST);
////      // we want main artifacts only (no classifier)
////      // Note: this below is unfinished API, needs fixing
////      query.add(indexer.constructQuery(MAVEN.CLASSIFIER, new SourcedSearchExpression(Field.NOT_PRESENT)), Occur.MUST_NOT);
//      
//      String groupId="";
////      String version="";
//      return new Artifact(groupId, artifactId, version);
    }catch(Exception e){
      
    }
    throw new RuntimeException("Maven Index Artifact searching has not been implemented yet.");
  }
}
