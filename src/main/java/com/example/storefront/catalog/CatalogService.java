package com.example.storefront.catalog;

import com.example.storefront.common.DomainExceptions.BusinessRuleException;
import com.example.storefront.common.DomainExceptions.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read access to the catalogue for everyone; writes are ADMIN-only. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public Page<Product> browse(String categorySlug, Pageable pageable) {
        return categorySlug == null || categorySlug.isBlank()
                ? products.findByActiveTrue(pageable)
                : products.findByActiveTrueAndCategory_Slug(categorySlug, pageable);
    }

    public Product requireById(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found."));
    }

    public List<Category> listCategories() {
        return categories.findAll();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Product addProduct(String sku, String name, String description,
                              long priceCents, String currency, String categorySlug, int stockQuantity) {
        if (products.existsBySku(sku)) {
            throw new BusinessRuleException("SKU " + sku + " already exists.");
        }
        Category category = categories.findBySlug(categorySlug)
                .orElseThrow(() -> new NotFoundException("Category '" + categorySlug + "' not found."));
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPriceCents(priceCents);
        product.setCurrency(currency == null ? "USD" : currency);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setActive(true);
        return products.save(product);
    }
}
