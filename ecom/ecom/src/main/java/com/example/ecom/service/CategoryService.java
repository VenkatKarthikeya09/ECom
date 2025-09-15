package com.example.ecom.service;

import com.example.ecom.model.Category;
import com.example.ecom.repository.CategoryRepository;
import com.example.ecom.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Integer categoryId) {
        return categoryRepository.findById(categoryId);
    }

    public Category createCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return categoryRepository.save(c);
    }

    public Optional<Category> updateCategory(Integer id, String name) {
        return categoryRepository.findById(id).map(c -> {
            c.setName(name);
            return categoryRepository.save(c);
        });
    }

    public boolean deleteCategory(Integer id) {
        long hasProducts = productRepository.countByCategory_CategoryId(id);
        if (hasProducts > 0) {
            return false;
        }
        categoryRepository.deleteById(id);
        return true;
    }
}