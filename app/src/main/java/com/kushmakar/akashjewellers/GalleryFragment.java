package com.kushmakar.akashjewellers;



import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Sample gallery images data
        List<GalleryImage> images = new ArrayList<>();
        images.add(new GalleryImage("1", "Machine Chain", R.drawable.machine_chain_1));
        images.add(new GalleryImage("2", "Machine Chain 2", R.drawable.machine_chain_2));
        images.add(new GalleryImage("3", "Machine Chain 3", R.drawable.machine_chain_3));
        images.add(new GalleryImage("4", "Chain ", R.drawable.chain));
        images.add(new GalleryImage("5", "Chain 2", R.drawable.chain_2));
        images.add(new GalleryImage("6", "Chain 3", R.drawable.chain_3));
        images.add(new GalleryImage("8", "Chain 4", R.drawable.chain_4));
        images.add(new GalleryImage("7", "HandMade chain", R.drawable.handmade_chain_1));


        // Set up RecyclerView with GridLayoutManager (2 columns)
        recyclerView = view.findViewById(R.id.recyclerView_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(new GalleryAdapter(images));
    }
}