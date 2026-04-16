package kr.ac.hansung.cse.controller;

import jakarta.validation.Valid;
import kr.ac.hansung.cse.exception.ProductNotFoundException;
import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.model.ProductForm;
import kr.ac.hansung.cse.service.CategoryService;
import kr.ac.hansung.cse.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}")
    public String showProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        model.addAttribute("product", product);
        return "productView";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        populateCategories(model);
        return "productForm";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm productForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateCategories(model);
            return "productForm";
        }

        String normalizedCategoryName = productService.normalizeCategoryName(productForm.getCategory());
        productForm.setCategory(normalizedCategoryName);
        if (normalizedCategoryName != null && productService.findCategory(normalizedCategoryName).isEmpty()) {
            bindingResult.rejectValue("category", "invalid", "존재하지 않는 카테고리입니다.");
            populateCategories(model);
            return "productForm";
        }

        Product product = productForm.toEntity();
        product.setCategory(productService.resolveCategory(normalizedCategoryName));
        Product savedProduct = productService.createProduct(product);

        redirectAttributes.addFlashAttribute("successMessage",
                "'" + savedProduct.getName() + "' 상품이 성공적으로 등록되었습니다.");
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        model.addAttribute("productForm", ProductForm.from(product));
        populateCategories(model);
        return "productEditForm";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") ProductForm productForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateCategories(model);
            return "productEditForm";
        }

        String normalizedCategoryName = productService.normalizeCategoryName(productForm.getCategory());
        productForm.setCategory(normalizedCategoryName);
        if (normalizedCategoryName != null && productService.findCategory(normalizedCategoryName).isEmpty()) {
            bindingResult.rejectValue("category", "invalid", "존재하지 않는 카테고리입니다.");
            populateCategories(model);
            return "productEditForm";
        }

        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(productForm.getName());
        product.setCategory(productService.resolveCategory(normalizedCategoryName));
        product.setPrice(productForm.getPrice());
        product.setDescription(productForm.getDescription());

        productService.updateProduct(product);

        redirectAttributes.addFlashAttribute("successMessage",
                "'" + product.getName() + "' 상품 정보가 수정되었습니다.");
        return "redirect:/products/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        String productName = product.getName();
        productService.deleteProduct(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "'" + productName + "' 상품이 삭제되었습니다.");
        return "redirect:/products";
    }

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               Model model) {
        List<Product> products;
        if (keyword != null && !keyword.isBlank() && categoryId != null) {
            products = productService.searchByNameAndCategory(keyword, categoryId);
        } else if (keyword != null && !keyword.isBlank()) {
            products = productService.searchByName(keyword);
        } else if (categoryId != null) {
            products = productService.searchByCategory(categoryId);
        } else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "productList";
    }

    private void populateCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
    }
}
