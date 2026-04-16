package kr.ac.hansung.cse.service;

import kr.ac.hansung.cse.model.Category;
import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.repository.CategoryRepository;
import kr.ac.hansung.cse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Category resolveCategory(String categoryName) {
        return findCategory(categoryName).orElse(null);
    }

    public Optional<Category> findCategory(String categoryName) {
        String normalizedCategoryName = normalizeCategoryName(categoryName);
        if (normalizedCategoryName == null) {
            return Optional.empty();
        }
        return categoryRepository.findByName(normalizedCategoryName);
    }

    public String normalizeCategoryName(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        String normalizedCategoryName = categoryName.trim();
        return normalizedCategoryName.isEmpty() ? null : normalizedCategoryName;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        validatePrice(product.getPrice());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Product product) {
        validatePrice(product.getPrice());
        return productRepository.update(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(id);
    }

    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }

    public List<Product> searchByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    private void validatePrice(BigDecimal price) {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
    }
}
