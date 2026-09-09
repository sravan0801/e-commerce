package com.example.storefront.catalog;

import com.example.storefront.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public PageResponse<ProductView> products(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(catalog.browse(category, pageable), ProductView::from);
    }

    @GetMapping("/products/{id}")
    public ProductView product(@PathVariable Long id) {
        return ProductView.from(catalog.requireById(id));
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return catalog.listCategories().stream().map(CategoryView::from).toList();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody CreateProductRequest body) {
        Product saved = catalog.addProduct(body.sku(), body.name(), body.description(),
                body.priceCents(), body.currency(), body.categorySlug(), body.stockQuantity());
        return ProductView.from(saved);
    }

    // --- DTOs -------------------------------------------------------------

    public record ProductView(
            Long id, String sku, String name, String description,
            long priceCents, String currency, int stockQuantity, String category, boolean active) {

        static ProductView from(Product p) {
            return new ProductView(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                    p.getPriceCents(), p.getCurrency(), p.getStockQuantity(),
                    p.getCategory() == null ? null : p.getCategory().getSlug(), p.isActive());
        }
    }

    public record CategoryView(Long id, String name, String slug) {
        static CategoryView from(Category c) {
            return new CategoryView(c.getId(), c.getName(), c.getSlug());
        }
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @PositiveOrZero long priceCents,
            @Size(min = 3, max = 3) String currency,
            @NotBlank String categorySlug,
            @PositiveOrZero int stockQuantity) {
    }
}
