package com.kushmakar.akashjewellers;

public class GalleryImage {
    private String id;
    private String name;
    private int imageResId;

    public GalleryImage(String id, String name, int imageResId) {
        this.id = id;
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}
