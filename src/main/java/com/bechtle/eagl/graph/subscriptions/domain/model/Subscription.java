package com.bechtle.eagl.graph.subscriptions.domain.model;

import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import com.bechtle.eagl.graph.domain.model.extensions.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public record Subscription(IRI iri, String label, String key, boolean persistent) {

    public static final String NAMESPACE = "http://av360.io/schema/subscriptions/";
    public static final String PREFIX = "sub";

    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public static final IRI TYPE = LocalIRI.from(NAMESPACE, "Subscription");

    public static final IRI ACTIVE_FEATURE = LocalIRI.from(NAMESPACE, "hasFeature");
    public static final IRI HAS_API_KEY = LocalIRI.from(NAMESPACE, "hasApiKey");
    public static final IRI HAS_KEY = DC.IDENTIFIER;
    public static final IRI HAS_LABEL = RDFS.LABEL;
    public static final IRI IS_PERSISTENT = LocalIRI.from(NAMESPACE, "isPersistent");



}

