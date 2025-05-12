package com.kushmakar.akashjewellers;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private List<GalleryImage> images;

    public GalleryAdapter(List<GalleryImage> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        GalleryImage image = images.get(position);

        // Load image from drawable
        holder.imageView.setImageResource(image.getImageResId());
        holder.imageName.setText(image.getName());

        // Show full-size image on click
        holder.itemView.setOnClickListener(v -> showImageDetailDialog(v, image));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    private void showImageDetailDialog(View view, GalleryImage image) {
        Dialog dialog = new Dialog(view.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_detail);

        ImageView imageView = dialog.findViewById(R.id.imageView_detail);
        TextView nameView = dialog.findViewById(R.id.textView_detailName);

        imageView.setImageResource(image.getImageResId());
        nameView.setText(image.getName());

        dialog.show();
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView imageName;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView_gallery);
            imageName = itemView.findViewById(R.id.textView_imageName);
        }
    }
}
