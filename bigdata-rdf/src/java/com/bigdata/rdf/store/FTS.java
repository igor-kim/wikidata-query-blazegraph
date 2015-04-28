/*

Copyright (C) SYSTAP, LLC 2006-2015.  All rights reserved.

Contact:
     SYSTAP, LLC
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@systap.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
/*
 * Created on Apr 12, 2008
 */

package com.bigdata.rdf.store;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;


/**
 * A vocabulary for the bigdata external full text search facility. The
 * interface is generic in the sense that we could add generic full text
 * search services, by just appending {@link EndpointType}s and adding
 * implementations for them. For the beginning, we start with a Solr index.
 * 
 *  
 * The FTS search may be used to combine text search and graph search, 
 * just like the {@link BDS} - the basic difference is that this search is
 * going against an *external* service, whereas {@link BDS} is querying the
 * internal fulltext index (which is kept in synch along the lines).
 * 
 * Low-latency, user facing search applications may be created by slicing the
 * external full text search results and feeding them incrementally into
 * SPARQL queries. This approach allows the application to manage the cost
 * of the SPARQL query by bounding the input. If necessary, additional results
 * can be feed into the query. 
 * 
 * Example:
 * 
 * <pre>
 * PREFIX fts: <http://www.bigdata.com/rdf/fts#>
 * SELECT ?res ?score ?snippet WHERE {
 *   ?res fts:search "blue !red".
 *   ?res fts:endpoint  "http://my.external.solr.endpoint:5656" .
 *   ?res fts:endpointType  "Solr" .
 *   ?res fts:params "defType=dismax&bf=uses^50" .
 *   ?res fts:targetType  "URI" .
 *   ?res fts:score ?score .
 *   ?res fts:snippet ?snippet . 
 * }
 * </pre>
 * 
 * The query returns the result matches (converted to URIs), including the
 * score and sample snippets for the matches.
 * 
 * Here's another example using a SERVICE keyword directly: 
 * 
 * <pre>
 * PREFIX fts: <http://www.bigdata.com/rdf/fts#>
 * SELECT *
 * WHERE {
 *   ?res rdfs:label ?label .
 *   SERVICE <http://www.bigdata.com/rdf/fts#search> {
 *     ?res fts:search "blue !red".
 *     ?res fts:endpoint  "http://my.external.solr.endpoint:5656" .
 *   }
 *   hint:Prior hint:runLast "true" .
 * }
 * 
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface FTS {

   /**
    * The namespace used for magic search predicates.
    */
   final String NAMESPACE = "http://www.bigdata.com/rdf/fts#";

   /**
    * The type of the FTS. For now, only Solr is implemented/supported.
    */
   public static enum EndpointType {
      SOLR
   }

   /**
    * Target type for extracted entities, determining whether they are
    * parsed into a literal or interpreted as a URI.
    */
   public static enum TargetType {
      URI,
      LITERAL
   }
       
   /**
    * The name of a magic predicate recognized in SPARQL queries when it occurs
    * in statement patterns such as:
    * 
    * <pre>
    * 
    * ( s?, fts:search, &quot;scale-out RDF triplestore&quot; )
    * 
    * </pre>
    * 
    * The value MUST be bound and MUST be a literal, it points to the Solr
    * search string.
    * 
    * <p>
    * The subject MUST NOT be bound. 
    * <p>
    * 
    * This expression will evaluate to a set of bindings for the subject
    * position corresponding to the indexed literals matching any of the terms
    * obtained when the literal was tokenized.
    * 
    * <p>
    * Note: The context position should be unbound when using statement
    * identifiers.
    */
   final URI SEARCH = new URIImpl(NAMESPACE + "search");
    
   /**
    * Magic predicate used to specify the Solr endpoint to be queried. If not
    * provided, the default endpoint as specified in the configuration is used.
    * <p>
    * 
    * <pre>
    * 
    * select ?s
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:endpoint "http://my.solr.endpoint:1012/solrIndex/" .
    * }
    * 
    * </pre>
    * 
    * The endpoint must be provided as a literal, including protocol, IP or
    * hostname, and port to be queried.
    */
   final URI ENDPOINT = new URIImpl(NAMESPACE + "endpoint");
      
   
   /**
    * Magic predicate used to specify the endpoint type, such as a Solr
    * endpoint or any other external full text search service. 
    * 
    * <pre>
    * 
    * select ?s
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:endpoint &quot;http://my.solr.endpoint:1012/solrIndex/&quot; .
    *   ?s fts:endpointType &quot;SOLR&quot; .
    * }
    * 
    * </pre>
    * 
    * The endpoint must be provided as a literal, according to the 
    * {@link EndpointType} enum values.
    */
   final URI ENDPOINT_TYPE = new URIImpl(NAMESPACE + "endpointType");
   
   
   final EndpointType DEFAULT_ENDPOINT_TYPE = EndpointType.SOLR;
   
   
   /**
    * Magic predicate used to specify full text search parameters to be
    * applied when executing the search. 
    * 
    * <pre>
    * 
    * select ?s
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:params &quot;defType=dismax&bf=uses^5&quot; .
    * }
    * 
    * </pre>
    * 
    * The params need to be a correct string according to Solr specifications
    * and it must be provided as a literal.
    */
   final URI PARAMS = new URIImpl(NAMESPACE + "params");
    
   /**
    * Magic predicate used to specify the type of the values stored in the Solr
    * field or fields from which data is extracted (the latter one being
    * specified as part of the PARAMS predicate above. If there are multiple
    * output fields, all of the will be included and the type specified refers
    * to all of them. Default is URI, which converts the field into a URI; if
    * conversion fails, the value is ignored. 
    * <p>
    * 
    * <pre>
    * 
    * select ?s
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:targetType &quot;LITERAL&quot; .
    * }
    * 
    * </pre>
    * 
    * Allowed values are "URI" and "LITERAL"; if none of these values is
    * provided, the {@value #DEFAULT_TARGET_TYPE} will be used.
    */   
   final URI TARGET_TYPE = new URIImpl(NAMESPACE + "targetType");
   
   public static final TargetType DEFAULT_TARGET_TYPE = TargetType.URI;

   /**
    * Magic predicate used to query for free text search metadata to set a
    * deadline in milliseconds on the full text index search (
    * {@value #DEFAULT_TIMEOUT}). Use in conjunction with {@link #SEARCH} as
    * follows:
    * <p>
    * 
    * <pre>
    * 
    * select ?s
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:timeout &quot;5000&quot; .
    * }
    * 
    * </pre>
    * <p>
    * 
    * Timeout specified in milliseconds, as literal. If not specified or not
    * a valid integer, the {@value #DEFAULT_TIMEOUT} is used.
    */
   final URI TIMEOUT = new URIImpl(NAMESPACE + "timeout");

   /**
    * The default timeout for a free text search (milliseconds).
    */
   final long DEFAULT_TIMEOUT = Long.MAX_VALUE;
   
   /**
    * Magic predicate to indicate the output variable in which the score
    * of matches will be saved. 
    * 
    * <pre>
    * 
    * select ?s ?score
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:score ?score .
    * }
    * 
    * </pre>
    * 
    * The referenced variable must not be used somewhere else in the scope.
    * It will be bound to an xsd:double typed literal indicating the score
    * for the match.
    */
   final URI SCORE = new URIImpl(NAMESPACE + "score");

   /**
    * Magic predicate to indicate the output variable in which a sample
    * snippet for matches will be saved. 
    * 
    * <pre>
    * 
    * select ?s ?snippet
    * where {
    *   ?s fts:search &quot;scale-out RDF triplestore&quot; .
    *   ?s fts:snippet ?snippet .
    * }
    * 
    * </pre>
    * 
    * The referenced variable must not be used somewhere else in the scope.
    * It will be bound to an untyped (text) literal.
    */
   final URI SNIPPET = new URIImpl(NAMESPACE + "snippet");

}