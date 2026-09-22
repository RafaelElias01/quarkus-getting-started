package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import jakarta.ws.rs.QueryParam;

@Path("/products")
public class ProductResource {

    private final List<Product> products = List.of(
            new Product(1L, "Product 1", "Category 1", 10.0),
            new Product(2L, "Product 2", "Category 2", 20.0),
            new Product(3L, "Product 3", "Category 3", 300.0)
    );

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Product> getProducts(@QueryParam("category") String category, @QueryParam("minPrice") Double minPrice) {
        if (category == null && minPrice == null) {
            return products;
        }
        return products.stream()
                .filter(product -> category == null || product.category().equals(category))
                .filter(product -> minPrice == null || product.price() >= minPrice)
                .toList();
    }
}
