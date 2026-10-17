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

    public interface OnCommentActionListener {
        void onEditComment(Comment comment);
        void onDeleteComment(Comment comment);
    }

    private List<Comment> comments = new ArrayList<>();
    private OnCommentActionListener listener;
    private String currentUserName;
    private String currentUserEmail;

    public CommentAdapter() {}

    public CommentAdapter(OnCommentActionListener listener, String currentUserName, String currentUserEmail) {
        this.listener = listener;
        this.currentUserName = currentUserName;
        this.currentUserEmail = currentUserEmail;
    }

    public void setCommentActionListener(OnCommentActionListener listener, String currentUserName, String currentUserEmail) {
        this.listener = listener;
        this.currentUserName = currentUserName;
        this.currentUserEmail = currentUserEmail;
    }

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

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentBinding binding;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        public CommentViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Comment comment) {
            binding.tvAuthor.setText(comment.getAuthorName());
            
            String content = comment.getContent();
            if (content == null || content.trim().isEmpty()) {
                content = "(No content)";
            }
            binding.tvContent.setText(content);
            binding.tvContent.setVisibility(View.VISIBLE);
            
            binding.tvDate.setText(dateFormat.format(new Date(comment.getCreatedAt())));
            binding.tvOfficialBadge.setVisibility(comment.isOfficial() ? View.VISIBLE : View.GONE);
            
            if (comment.isOfficial()) {
                binding.llCommentRoot.setBackgroundColor(0x1A10B981);
            } else {
                binding.llCommentRoot.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            boolean isOwned = false;
            String author = comment.getAuthorName();
            if (author != null) {
                if (currentUserName != null && author.equalsIgnoreCase(currentUserName)) isOwned = true;
                if (currentUserEmail != null && author.equalsIgnoreCase(currentUserEmail)) isOwned = true;
            }

            if (isOwned && listener != null) {
                binding.llCommentActions.setVisibility(View.VISIBLE);
                binding.btnEditComment.setOnClickListener(v -> listener.onEditComment(comment));
                binding.btnDeleteComment.setOnClickListener(v -> listener.onDeleteComment(comment));
            } else {
                binding.llCommentActions.setVisibility(View.GONE);
            }
        }
    }
}
