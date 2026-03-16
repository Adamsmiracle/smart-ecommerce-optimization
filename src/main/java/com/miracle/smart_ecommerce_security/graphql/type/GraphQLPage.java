package com.miracle.smart_ecommerce_security.graphql.type;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Concrete page wrapper for GraphQL resolvers.
 *
 * Spring GraphQL uses reflection to map return types to schema types.
 * Returning a raw Spring {@code Page<T>} (interface + generics) causes
 * "Error fetching schema" because type erasure prevents Spring GraphQL
 * from resolving the concrete element type for the {@code content} field.
 *
 * This class is concrete, non-generic at the field level, and its field
 * names match the *Page types in schema.graphqls exactly:
 *   content, totalElements, totalPages, size, number,
 *   first, last, numberOfElements, empty
 */
public class GraphQLPage<T> {

    private final List<T>  content;
    private final long     totalElements;
    private final int      totalPages;
    private final int      size;
    private final int      number;
    private final boolean  first;
    private final boolean  last;
    private final int      numberOfElements;
    private final boolean  empty;

    private GraphQLPage(Page<T> page) {
        this.content          = page.getContent();
        this.totalElements    = page.getTotalElements();
        this.totalPages       = page.getTotalPages();
        this.size             = page.getSize();
        this.number           = page.getNumber();
        this.first            = page.isFirst();
        this.last             = page.isLast();
        this.numberOfElements = page.getNumberOfElements();
        this.empty            = page.isEmpty();
    }

    public static <T> GraphQLPage<T> of(Page<T> page) {
        return new GraphQLPage<>(page);
    }

    public List<T>  getContent()          { return content; }
    public long     getTotalElements()    { return totalElements; }
    public int      getTotalPages()       { return totalPages; }
    public int      getSize()             { return size; }
    public int      getNumber()           { return number; }
    public boolean  isFirst()             { return first; }
    public boolean  isLast()              { return last; }
    public int      getNumberOfElements() { return numberOfElements; }
    public boolean  isEmpty()             { return empty; }
}

