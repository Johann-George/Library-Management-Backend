package com.johann.models;

public class BookCategory extends ModelBase {
	
    private String category = "";
    private String subCategory = "";

    // Constructors
    public BookCategory() {
    }

    // Getters and Setters for properties
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
}

