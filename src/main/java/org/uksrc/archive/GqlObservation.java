package org.uksrc.archive;

import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Type;
import org.ivoa.dm.caom2.SimpleObservation;

@Type("Observation") // This is the name users will see in GraphQL
@Name("ObsData")
public class GqlObservation extends SimpleObservation {

    public GqlObservation(SimpleObservation entity) {
        // Use the copy constructor provided by VO-DML
        super(entity);
    }

    // No need to map properties; GraphQL will find all
    // public getters inherited from the parents automatically.
}
