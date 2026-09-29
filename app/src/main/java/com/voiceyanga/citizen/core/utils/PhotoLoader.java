package com.voiceyanga.citizen.core.utils;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;

public class PhotoLoader {

    public static void loadPhoto(Complaint complaint, String uri, String mediaId, ComplaintRepository repo, ImageView iv, int placeholderRes) {
        // Always reset alpha to handle recycled views
        iv.setAlpha(1.0f);

        if (uri == null || uri.isEmpty()) {
            iv.setImageResource(placeholderRes);
            PhotoLogger.logPlaceholder();
            return;
        }

        String resolvedUrl = MediaUtils.resolvePhotoUrl(uri);
        if (resolvedUrl == null || resolvedUrl.isEmpty()) {
            iv.setImageResource(placeholderRes);
            PhotoLogger.logPlaceholder();
            return;
        }

        Object model;
        if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
            model = new GlideUrl(resolvedUrl);
        } else {
            model = resolvedUrl;
        }

        Glide.with(iv.getContext())
                .load(model)
                .placeholder(placeholderRes)
                .error(R.drawable.vote_icon)
                .listener(PhotoLogger.listener(complaint != null ? complaint.getServerId() : "N/A", mediaId != null ? mediaId : "N/A"))
                .into(iv);
    }
}
