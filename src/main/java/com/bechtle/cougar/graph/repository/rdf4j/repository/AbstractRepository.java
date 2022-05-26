package com.bechtle.cougar.graph.repository.rdf4j.repository;

import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.cougar.graph.domain.model.vocabulary.Transactions;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import com.bechtle.cougar.graph.repository.behaviours.RepositoryBehaviour;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j(topic = "cougar.graph.repository")
public class AbstractRepository implements RepositoryBehaviour {

    private final RepositoryConfiguration.RepositoryType repositoryType;
    private RepositoryConfiguration repositoryConfiguration;

    public AbstractRepository(RepositoryConfiguration.RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
    }


    @Autowired
    private void setConfig(RepositoryConfiguration repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }


    public Mono<Void> store(Model model) {
        return this.getRepository().flatMap(repository -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                try {
                    Resource[] contexts = model.contexts().toArray(new Resource[model.contexts().size()]);
                    connection.add(model, contexts);
                    connection.commit();
                    log.debug("Model was stored in repository '{}'", repository.toString());
                    return Mono.empty();
                } catch (Exception e) {
                    connection.rollback();
                    return Mono.error(e);
                }
            } catch (Exception e) {
                log.error("Failed to initialize repository connection");
                return Mono.error(e);
            }
        });
    }


    public Flux<BindingSet> query(String query) {
        log.debug("XXXX");
        return this.getRepository().flatMapMany(repository ->
                Flux.create(c -> {
                    try (RepositoryConnection connection = repository.getConnection()) {

                        log.debug("ZZZZ");
                        TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);

                        try (TupleQueryResult result = q.evaluate()) {
                            result.stream().forEach(c::next);
                        } finally {
                            c.complete();
                        }

                    } catch (MalformedQueryException e) {
                        log.warn("Error while parsing query, reason: {}", e.getMessage());
                        c.error(e);
                    } catch (Exception e) {
                        log.error("Unknown error while running query", e);
                        c.error(e);
                    }
                }));
    }

    public Mono<TupleQueryResult> queryOld(String query) {
        return this.getRepository().map(repository -> {
            try (RepositoryConnection connection = repository.getConnection()) {

                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);

                TupleQueryResult result = q.evaluate();
                if (!result.hasNext()) {
                    log.debug("Query with no results: {}", query);
                } else {
                    log.debug("Query has results: {}", query);
                }
                return result;

            } catch (MalformedQueryException e) {
                log.warn("Error while parsing query, reason: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                throw e;
            }
        });
    }





    @Override
    public Mono<Repository> getRepository() {
        return this.repositoryConfiguration.getRepository(this.repositoryType);
    }

    @Override
    public Flux<Transaction> commit(Collection<Transaction> transactions) {
        return this.getRepository().flatMapMany(repository -> Flux.create(c -> {

            try (RepositoryConnection connection = repository.getConnection()) {
                log.trace("(Store) Committing transaction to repository {}", getRepository().toString());

                transactions.forEach(trx -> {
                    // FIXME: the approach based on the context works only as long as the statements in the graph are all within the global context only
                    // with this approach, we cannot insert a statement to a context (since it is already in GRAPH_CREATED), every st can only be in one context
                    Model insertModel = trx.getModel().filter(null, null, null, Transactions.GRAPH_CREATED);
                    Model removeModel = trx.getModel().filter(null, null, null, Transactions.GRAPH_DELETED);

                    // we have to get rid of the context
                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
                    List<Statement> insertStatements = insertModel.stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).toList();
                    List<Statement> removeStatements = removeModel.stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).toList();

                    try {
                        connection.begin();
                        connection.add(insertStatements);
                        connection.remove(removeStatements);
                        connection.commit();

                        trx.setCompleted();

                        log.trace("(Store) Transaction completed with {} inserted statements and {} removed statements.", insertModel.size(), removeModel.size());
                        c.next(trx);
                    } catch (Exception e) {
                        log.error("(Store) Failed to complete transaction .", e);
                        log.trace("(Store) Insert Statements in this transaction: \n {}", insertModel);
                        log.trace("(Store) Remove Statements in this transaction: \n {}", removeModel);

                        connection.rollback();
                        trx.setFailed(e.getMessage());
                        c.next(trx);
                    }


                });

                c.complete();
            } catch (Exception e) {
                log.error("Failed to initialize repository connection");
                c.error(e);
            }
        }));
    }
}