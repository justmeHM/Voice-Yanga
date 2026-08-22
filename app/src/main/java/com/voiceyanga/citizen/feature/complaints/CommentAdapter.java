package com.voiceyanga.citizen.feature.complaints;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.databinding.ItemCommentBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentBinding binding;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        public CommentViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Comment comment) {
            binding.tvAuthor.setText(comment.getAuthorName());
            binding.tvContent.setText(comment.getContent());
            binding.tvDate.setText(dateFormat.format(new Date(comment.getCreatedAt())));
            binding.tvOfficialBadge.setVisibility(comment.isOfficial() ? View.VISIBLE : View.GONE);
            
            if (comment.isOfficial()) {
                binding.llCommentRoot.setBackgroundColor(0xFFF0FDF4); // Subtle green for official updates
            } else {
                binding.llCommentRoot.setBackgroundColor(0xFFFFFFFF);
            }
        }
    }
}