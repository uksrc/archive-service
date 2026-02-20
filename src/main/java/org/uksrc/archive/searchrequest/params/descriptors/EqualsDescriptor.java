package org.uksrc.archive.searchrequest.params.descriptors;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.uksrc.archive.searchrequest.query.QueryContext;

public class EqualsDescriptor<T> implements PredicateDescriptor {

    private final String path;
    private final T value;

    public EqualsDescriptor(String path, T value) {
        this.path = path;
        this.value = value;
    }

    @Override
    public Predicate toPredicate(QueryContext<?> context) {
        Path<?> field = resolvePath(context.root(), path);
        return context.criteriaBuilder().equal(field, value);
    }

    private Path<?> resolvePath(Root<?> root, String path) {
        String[] parts = path.split("\\.");
        Path<?> p = root;
        for (String part : parts) {
            p = p.get(part);
        }
        return p;
    }
}
