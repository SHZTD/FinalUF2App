package com.example.socialappuf2;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {
    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Client client;
    Account account;
    String userId;
    PostsAdapter adapter;

    AppViewModel appViewModel;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle
            savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);
        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        appViewModel = new
                ViewModelProvider(requireActivity()).get(AppViewModel.class);

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() ->
                {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName().toString());
                    emailTextView.setText(result.getEmail().toString());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts(); // < – Pedir los posts tras obtener el usuario
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);

    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, deleteImageView;
        TextView authorTextView, contentTextView, numLikesTextView, timeTextView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            deleteImageView = itemView.findViewById(R.id.deleteImageView);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int
                viewType) {
            return new
                    PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop()
                        .into(holder.authorPhotoImageView);
            }
            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            // seccion para eliminar posts
            String uId = post.get("uid").toString();

            if (uId.equals(userId)) {
                holder.deleteImageView.setVisibility(View.VISIBLE);
                holder.deleteImageView.setOnClickListener(view -> {
                    eliminarPost(post.get("$id").toString());
                });
            } else {
                holder.deleteImageView.setVisibility(View.GONE);
            }

            //Fecha y Hora
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Calendar calendar = Calendar.getInstance();
            if (post.get("time") != null)
                calendar.setTimeInMillis((long) post.get("time"));
            else
                calendar.setTimeInMillis(0);

            holder.timeTextView.setText(formatter.format(calendar.getTime()));


            // Miniatura de media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into
                            (holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = likes;
                if (nuevosLikes.contains(userId))
                    nuevosLikes.remove(userId);
                else nuevosLikes.add(userId);

                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);
                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(), // documentId
                            data, // data (optional)
                            new ArrayList<>(), // permissions (optional)
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }
                                System.out.println("Likes actualizados:" +
                                        result.toString());
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }

    void obtenerPosts() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    Arrays.asList(Query.Companion.orderDesc("time"),
                            Query.Companion.limit(50)),    // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: "
                                    + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        System.out.println(result.toString());
                        mainHandler.post(() -> adapter.establecerLista(result));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    // para eliminar el post
    void eliminarPost(String postId) {
        Databases databases = new Databases(client);
        databases.deleteDocument(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                postId,
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        Snackbar.make(requireView(), "Error al eliminar el post: " + error.toString(), Snackbar.LENGTH_LONG).show();
                    } else {
                        obtenerPosts(); // Refrescar la lista de posts
                    }
                })
        );
    }

}