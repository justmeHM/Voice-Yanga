package com.voiceyanga.citizen.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.voiceyanga.citizen.R;

public class SuccessDialogFragment extends DialogFragment {

    public interface OnDismissListener {
        void onDismissed();
    }

    private OnDismissListener listener;

    public void setOnDismissListener(OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Material_Light_Dialog_MinWidth);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        View icon = view.findViewById(R.id.ivSuccessIcon);
        if (icon != null) {
            android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.scale_in);
            icon.startAnimation(anim);
        }

        // Auto-dismiss after animation (approx 2s)
        view.postDelayed(() -> {
            if (isAdded()) {
                dismiss();
                if (listener != null) listener.onDismissed();
            }
        }, 2500);
    }
}
