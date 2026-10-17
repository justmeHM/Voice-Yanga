package com.voiceyanga.citizen.feature.complaints;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.utils.HapticHelper;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.databinding.ActivityCreateComplaintBinding;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Screen for creating a new citizen complaint.
 * Refined to a single-screen Smart Composer for enterprise efficiency.
 */
@AndroidEntryPoint
public class CreateComplaintActivity extends AppCompatActivity {

    @javax.inject.Inject
    com.voiceyanga.citizen.data.repository.ComplaintRepository complaintRepository;

    @javax.inject.Inject
    com.voiceyanga.citizen.data.repository.AnalyticsRepository analyticsRepository;

    @javax.inject.Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    private ActivityCreateComplaintBinding binding;
    private ComplaintViewModel viewModel;
    private PhotoAdapter photoAdapter;
    private CategoryAdapter categoryAdapter;
    private CategoryDto selectedCategory = null;
    private LocationDto currentLocation = null;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String detectedAddress = null;
    private boolean isManualLocation = false;
    private List<CategoryDto> availableCategories;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> voicePermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private ActivityResultLauncher<Intent> videoCaptureLauncher;
    private ActivityResultLauncher<String> videoGalleryLauncher;
    private ActivityResultLauncher<String> videoPermissionLauncher;
    private android.net.Uri cameraImageUri;
    private String selectedVideoPath = null;
    private int selectedVideoDuration = 0;
    private boolean isSubmitted = false;

    public static final String[] SENSITIVE_SUB_CATEGORIES = {
            "Gender-Based Violence (GBV)",
            "Sexual harassment",
            "Human rights abuses",
            "Child labour",
            "Child abuse and neglect",
            "Violence against children"
    };

    private final android.os.Handler autoSaveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            triggerAutoSave();
            autoSaveHandler.postDelayed(this, 30000); // 30 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityCreateComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (analyticsRepository != null) {
            analyticsRepository.trackComplaintCreationStarted();
        }

        setupPermissionLaunchers();
        setupToolbar();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        
        String existingUuid = getIntent().getStringExtra("extra_complaint_uuid");
        if (existingUuid != null) {
            viewModel.loadComplaintForEdit(existingUuid);
            binding.toolbar.setTitle("Edit Report");
        } else {
            viewModel.loadDraft();
        }

        requestLocationPermissions();
        populateSummary();
        
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void setupPermissionLaunchers() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if ((fineLocationGranted != null && fineLocationGranted) || 
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                        getLastLocation();
                    } else {
                        Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        voicePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        HapticHelper.selection(binding.btnAddEvidence);
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentStarted("VOICE_NOTE");
                        }
                        viewModel.startRecording();
                    } else {
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentFailed("VOICE_NOTE", "PERMISSION");
                        }
                        Toast.makeText(this, R.string.error_microphone_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        HapticHelper.success(this);
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentStarted("PHOTO");
                        }
                        viewModel.addPhoto(uri.toString());
                    } else {
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentFailed("PHOTO", "CANCELLED");
                        }
                    }
                }
        );

        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        android.net.Uri videoUri = data != null ? data.getData() : null;
                        if (videoUri != null) {
                            handleSelectedVideo(videoUri);
                        } else {
                            Toast.makeText(this, "Failed to capture video", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        videoGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedVideo(uri);
                    }
                }
        );

        videoPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchVideoCapture();
                    } else {
                        Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        HapticHelper.success(this);
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentStarted("PHOTO");
                        }
                        viewModel.addPhoto(cameraImageUri.toString());
                    } else {
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentFailed("PHOTO", "CANCELLED");
                        }
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        if (analyticsRepository != null) {
                            analyticsRepository.trackMediaAttachmentFailed("PHOTO", "PERMISSION");
                        }
                        Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleSelectedVideo(android.net.Uri videoUri) {
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, videoUri);
            
            String durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = durationStr != null ? Long.parseLong(durationStr) : 0;
            int durationSec = (int) (durationMs / 1000);
            
            if (durationSec > 90) {
                showVideoTrimmerDialog(videoUri, durationMs);
            } else {
                processAndAttachVideo(videoUri, 0, durationMs, durationSec);
            }
        } catch (Exception e) {
            android.util.Log.e("CreateComplaint", "Failed to process video", e);
            Toast.makeText(this, "Failed to process video: " + com.voiceyanga.citizen.core.network.ErrorParser.parseThrowable(e), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }

    private void showVideoTrimmerDialog(android.net.Uri videoUri, long durationMs) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_video_trimmer);

        androidx.media3.ui.PlayerView playerView = dialog.findViewById(R.id.playerViewTrimmer);
        android.widget.TextView tvTrimDuration = dialog.findViewById(R.id.tvTrimDuration);
        com.google.android.material.slider.RangeSlider rangeSlider = dialog.findViewById(R.id.rangeSliderTrim);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmTrim);
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelTrim);

        androidx.media3.exoplayer.ExoPlayer player = new androidx.media3.exoplayer.ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setRepeatMode(androidx.media3.common.Player.REPEAT_MODE_ONE);

        androidx.media3.common.MediaItem mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        float durationSec = durationMs / 1000f;
        rangeSlider.setValueFrom(0f);
        rangeSlider.setValueTo(durationSec);
        
        float defaultEnd = Math.min(90f, durationSec);
        rangeSlider.setValues(0f, defaultEnd);

        final long[] selectedStartMs = {0};
        final long[] selectedEndMs = {(long) (defaultEnd * 1000)};

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            java.util.List<Float> values = slider.getValues();
            float startSec = values.get(0);
            float endSec = values.get(1);

            if (endSec - startSec > 90f) {
                if (fromUser) {
                    endSec = startSec + 90f;
                    if (endSec > durationSec) {
                        endSec = durationSec;
                        startSec = Math.max(0f, endSec - 90f);
                    }
                    slider.setValues(startSec, endSec);
                }
            }

            selectedStartMs[0] = (long) (startSec * 1000);
            selectedEndMs[0] = (long) (endSec * 1000);

            long clipLengthSec = (long) (endSec - startSec);
            tvTrimDuration.setText(String.format(Locale.getDefault(), "Selected Clip: %d:%02d - %d:%02d (%d:%02d)",
                    (long) startSec / 60, (long) startSec % 60,
                    (long) endSec / 60, (long) endSec % 60,
                    clipLengthSec / 60, clipLengthSec % 60));

            player.seekTo(selectedStartMs[0]);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            processAndAttachVideo(videoUri, selectedStartMs[0], selectedEndMs[0], (int) ((selectedEndMs[0] - selectedStartMs[0]) / 1000));
        });

        dialog.setOnDismissListener(d -> {
            player.stop();
            player.release();
        });

        dialog.show();
    }

    private void processAndAttachVideo(android.net.Uri videoUri, long startMs, long endMs, int clipDurationSec) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Optimizing video clip for upload...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        android.graphics.Bitmap frame = null;
        try {
            retriever.setDataSource(this, videoUri);
            frame = retriever.getFrameAtTime(startMs > 0 ? startMs * 1000 : 1000000);
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }

        final android.graphics.Bitmap thumbnailFrame = frame;

        com.voiceyanga.citizen.core.utils.VideoCompressor.compressVideo(this, videoUri, startMs, endMs, new com.voiceyanga.citizen.core.utils.VideoCompressor.CompressCallback() {
            @Override
            public void onSuccess(java.io.File compressedFile) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();

                selectedVideoPath = compressedFile.getAbsolutePath();
                selectedVideoDuration = clipDurationSec;

                binding.cardVideoNote.setVisibility(View.VISIBLE);
                if (thumbnailFrame != null) {
                    binding.ivVideoThumbnail.setImageBitmap(thumbnailFrame);
                }
                binding.tvVideoDuration.setText(String.format(Locale.getDefault(), "Video • %d:%02d", clipDurationSec / 60, clipDurationSec % 60));

                binding.btnDeleteVideo.setOnClickListener(v -> {
                    selectedVideoPath = null;
                    selectedVideoDuration = 0;
                    binding.cardVideoNote.setVisibility(View.GONE);
                });

                Toast.makeText(CreateComplaintActivity.this, "Video attached & optimized", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();
                Toast.makeText(CreateComplaintActivity.this, "Failed to attach video: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMediaSelectorBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.dialog_media_selector, null);
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.cardOptionPhotos).setOnClickListener(v -> {
            HapticHelper.selection(v);
            bottomSheetDialog.dismiss();
            showPhotoSourceDialog();
        });

        sheetView.findViewById(R.id.cardOptionVoice).setOnClickListener(v -> {
            HapticHelper.selection(v);
            bottomSheetDialog.dismiss();
            checkVoicePermission();
        });

        sheetView.findViewById(R.id.cardOptionVideo).setOnClickListener(v -> {
            HapticHelper.selection(v);
            bottomSheetDialog.dismiss();
            showVideoSourceDialog();
        });

        bottomSheetDialog.show();
    }

    private void checkCameraPermissionForVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchVideoCapture();
        } else {
            videoPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchVideoCapture() {
        try {
            Intent videoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            videoIntent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 90);
            videoCaptureLauncher.launch(videoIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No camera application found to record video", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch video recorder: " + com.voiceyanga.citizen.core.network.ErrorParser.parseThrowable(e), Toast.LENGTH_SHORT).show();
        }
    }

    private void showVideoSourceDialog() {
        String[] options = {"Record Video", "Choose Video from Gallery"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Video Attachment (Max 90s)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionForVideo();
                    } else {
                        videoGalleryLauncher.launch("video/*");
                    }
                })
                .show();
    }

    private void showVideoPreviewDialog(String videoPath) {
        if (videoPath == null || videoPath.trim().isEmpty()) {
            Toast.makeText(this, "No video available to preview", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_video_preview);

        androidx.media3.ui.PlayerView playerView = dialog.findViewById(R.id.playerViewPreview);
        com.google.android.material.button.MaterialButton btnClose = dialog.findViewById(R.id.btnClosePreview);

        androidx.media3.exoplayer.ExoPlayer player = new androidx.media3.exoplayer.ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        androidx.media3.common.MediaItem mediaItem;
        if (videoPath.startsWith("content://") || videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(videoPath));
        } else {
            mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(new java.io.File(videoPath)));
        }

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            player.stop();
            player.release();
        });

        dialog.show();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> handleBackPress());
    }

    private void triggerAutoSave() {
        if (binding == null || isSubmitted) return;
        
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String customCat = binding.etCustomCategory.getText().toString().trim();

        if (!title.isEmpty() || !desc.isEmpty()) {
            viewModel.saveDraft(title, desc, selectedCategory, customCat, currentLocation, detectedAddress);
            if (analyticsRepository != null) {
                analyticsRepository.trackComplaintDraftSaved();
            }
        }
    }

    private void handleBackPress() {
        if (binding == null) {
            finish();
            return;
        }

        if (isSubmitted) {
            finish();
            return;
        }
        
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String customCat = binding.etCustomCategory.getText().toString().trim();

        if (!title.isEmpty() || !desc.isEmpty()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_save_draft_title)
                    .setMessage(R.string.dialog_save_draft_message)
                    .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                        viewModel.saveDraft(title, desc, selectedCategory, customCat, currentLocation, detectedAddress);
                        finish();
                    })
                    .setNegativeButton(R.string.btn_discard, (dialog, which) -> {
                        viewModel.deleteDraft();
                        finish();
                    })
                    .setNeutralButton(R.string.btn_cancel, null)
                    .show();
        } else {
            finish();
        }
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(category -> {
            HapticHelper.selection(binding.rvCategories);
            selectCategory(category.getName());
            
            // Auto fill Title problem field if empty
            if (binding.etTitle.getText().toString().trim().isEmpty()) {
                binding.etTitle.setText(category.getName());
            }
            
            // Show details step
            if (binding.llStepDetails.getVisibility() != View.VISIBLE) {
                binding.llStepDetails.setVisibility(View.VISIBLE);
                // Smooth scroll to details
                binding.llStepDetails.post(() -> {
                    int scrollTo = binding.llStepDetails.getTop();
                    ((androidx.core.widget.NestedScrollView) binding.llStepDetails.getParent().getParent()).smoothScrollTo(0, scrollTo);
                });
            }
        });
        binding.rvCategories.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        binding.rvCategories.setAdapter(categoryAdapter);
        binding.rvCategories.setNestedScrollingEnabled(false);

        photoAdapter = new PhotoAdapter(new PhotoAdapter.OnRemoveListener() {
            @Override
            public void onRemove(String uri) {
                if (uri != null) viewModel.removePhoto(uri);
            }

            @Override
            public void onPhotoClick(String uri) {
                showPhotoPreview(uri);
            }

            @Override
            public void onLabelChanged(String uri, String label) {
                if (uri != null) viewModel.updatePhotoLabel(uri, label);
            }
        }, complaintRepository);
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupObservers() {
        viewModel.getCategories().observe(this, categories -> {
            if (categories != null && !categories.isEmpty()) {
                this.availableCategories = categories;
                categoryAdapter.setCategories(categories);
            }
        });

        viewModel.getLocations().observe(this, locations -> {
            // Keep locations referenced for spinner dialog updates
        });

        viewModel.getSubmissionSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                this.isSubmitted = true;
                com.voiceyanga.citizen.ui.common.SuccessDialogFragment dialog = new com.voiceyanga.citizen.ui.common.SuccessDialogFragment();
                dialog.setOnDismissListener(this::finish);
                dialog.show(getSupportFragmentManager(), "success");
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                binding.btnSubmit.setEnabled(true);
                
                String errLower = error.toLowerCase();
                if (errLower.contains("title")) {
                    binding.tilTitle.setError(error);
                    binding.etTitle.requestFocus();
                } else if (errLower.contains("description")) {
                    binding.tilDescription.setError(error);
                    binding.etDescription.requestFocus();
                } else if (errLower.contains("problem") || errLower.contains("category")) {
                    Toast.makeText(this, R.string.error_select_problem_type, Toast.LENGTH_LONG).show();
                }
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null && binding != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnSubmit.setEnabled(!isLoading);
            }
        });

        viewModel.getSelectedPhotos().observe(this, photos -> {
            if (photos != null) {
                photoAdapter.setPhotos(photos);
                populateSummary();
            }
        });

        viewModel.getMappedLocation().observe(this, location -> {
            if (binding == null) return;
            this.currentLocation = location;
            if (location != null) {
                if (!isManualLocation && (detectedAddress == null || detectedAddress.trim().isEmpty() || detectedAddress.contains("Unknown"))) {
                    this.detectedAddress = currentLocation.getDisplayName();
                    binding.tvDetectedLocation.setText(detectedAddress);
                }
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                binding.pbLocationLoading.setVisibility(View.GONE);
                populateSummary();
            }
        });

        viewModel.getDraft().observe(this, draft -> {
            if (draft != null && binding != null) {
                binding.etTitle.setText(draft.getTitle());
                binding.etDescription.setText(draft.getDescription());
                
                if (draft.getLocation() != null && !draft.getLocation().trim().isEmpty()) {
                    this.detectedAddress = draft.getLocation();
                    this.isManualLocation = true;
                    binding.tvDetectedLocation.setText(detectedAddress);
                    binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                    binding.pbLocationLoading.setVisibility(View.GONE);
                    viewModel.matchAndSetLocation(draft.getLocation());
                }
                if (draft.getLatitude() != 0.0) this.currentLatitude = draft.getLatitude();
                if (draft.getLongitude() != 0.0) this.currentLongitude = draft.getLongitude();

                String draftCategory = draft.getCategory();
                if (draftCategory != null) {
                    selectCategory(draftCategory);
                }
                populateSummary();
            }
        });

        viewModel.getVoiceNoteState().observe(this, state -> {
            if (state == null || binding == null) return;
            
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show();
            }
            
            binding.cardVoiceNote.setVisibility(state.isRecording || state.localFile != null ? View.VISIBLE : View.GONE);
            binding.llRecordControls.setVisibility(state.isRecording ? View.VISIBLE : View.GONE);
            binding.llPlaybackControls.setVisibility(state.localFile != null ? View.VISIBLE : View.GONE);

            // Hide description if voice note is being recorded or is present
            binding.tilDescription.setVisibility(state.isRecording || state.localFile != null ? View.GONE : View.VISIBLE);
            
            if (state.isRecording) {
                int minutes = state.elapsedSeconds / 60;
                int seconds = state.elapsedSeconds % 60;
                binding.tvRecordingTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                binding.visualizer.addAmplitude(state.amplitude);
            } else if (state.localFile != null) {
                binding.visualizer.clear();
                binding.btnPlay.setVisibility(state.isPlaying ? View.GONE : View.VISIBLE);
                binding.btnPause.setVisibility(state.isPlaying ? View.VISIBLE : View.GONE);
                
                int duration = state.durationSeconds;
                int minutes = duration / 60;
                int seconds = duration % 60;
                binding.tvVoiceDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                populateSummary();
            }
        });
    }

    private void setupListeners() {
        binding.etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    binding.tilTitle.setError(null);
                    if (selectedCategory == null) {
                        suggestCategory(s.toString());
                    }
                }
                populateSummary();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { 
                if (s != null && s.length() > 0) {
                    binding.tilDescription.setError(null);
                }
                populateSummary(); 
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnSubmit.setOnClickListener(v -> {
            if (isSubmitted) return;
            
            binding.tilTitle.setError(null);
            binding.tilDescription.setError(null);

            // Auto-stop active recording if user is currently recording
            ComplaintViewModel.VoiceNoteState vnState = viewModel.getVoiceNoteState().getValue();
            if (vnState != null && vnState.isRecording) {
                viewModel.stopRecording();
            }

            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String customCategory = binding.etCustomCategory.getText().toString().trim();
            
            binding.btnSubmit.setEnabled(false);
            if (analyticsRepository != null) {
                analyticsRepository.trackComplaintSubmissionAttempted(null);
            }
            if (sessionManager != null && detectedAddress != null && !detectedAddress.trim().isEmpty() && !detectedAddress.contains("timed out")) {
                sessionManager.saveRecentCustomLocation(detectedAddress);
            }
            boolean isAnonymous = binding.switchAnonymous.isChecked();
            if (binding.tilSubCategory.getVisibility() == View.VISIBLE && binding.actvSubCategory.getText() != null && !binding.actvSubCategory.getText().toString().trim().isEmpty()) {
                customCategory = binding.actvSubCategory.getText().toString().trim();
            }

            viewModel.submitComplaint(title, description, selectedCategory, customCategory, currentLocation, 
                    detectedAddress, currentLatitude, currentLongitude, selectedVideoPath, selectedVideoDuration, isAnonymous);
            isSubmitted = true;
        });

        binding.btnManualLocation.setOnClickListener(v -> showManualLocationDialog());
        binding.btnAddEvidence.setOnClickListener(v -> {
            HapticHelper.selection(v);
            showMediaSelectorBottomSheet();
        });
        binding.btnPlayVideoPreview.setOnClickListener(v -> {
            HapticHelper.selection(v);
            showVideoPreviewDialog(selectedVideoPath);
        });
        binding.cardReviewSummary.setOnClickListener(v -> showFullReviewDialog());

        binding.btnQuickExit.setOnClickListener(v -> {
            HapticHelper.selection(v);
            finish();
        });

        android.widget.ArrayAdapter<String> subCatAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, SENSITIVE_SUB_CATEGORIES);
        binding.actvSubCategory.setAdapter(subCatAdapter);
        binding.actvSubCategory.setOnItemClickListener((parent, view, position, id) -> {
            String subCat = SENSITIVE_SUB_CATEGORIES[position];
            selectedCategory = new CategoryDto(null, subCat);
            binding.etTitle.setText(subCat);
            populateSummary();
        });

        binding.btnStop.setOnClickListener(v -> {
            HapticHelper.selection(v);
            viewModel.stopRecording();
        });
        binding.btnPlay.setOnClickListener(v -> viewModel.playRecording());
        binding.btnPause.setOnClickListener(v -> viewModel.pauseRecording());
        binding.btnDeleteVoice.setOnClickListener(v -> {
            HapticHelper.longPress(v);
            viewModel.deleteRecording();
            populateSummary();
        });
    }

    private void checkVoicePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            HapticHelper.selection(binding.btnAddEvidence);
            viewModel.startRecording();
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void showPhotoSourceDialog() {
        String[] options = {getString(R.string.option_take_photo), getString(R.string.option_choose_gallery)};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.photos_label)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        File imageFolder = new File(getCacheDir(), "images");
        if (!imageFolder.exists()) {
            boolean ignored = imageFolder.mkdirs();
        }
        File photoFile = new File(imageFolder, "cam_" + System.currentTimeMillis() + ".jpg");
        
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this, 
                getPackageName() + ".provider", photoFile);
        
        cameraLauncher.launch(cameraImageUri);
    }

    private void showManualLocationDialog() {
        String recentLocation = sessionManager != null ? sessionManager.getRecentCustomLocation() : null;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);

        List<LocationDto> apiLocations = viewModel.getLocations().getValue();
        if (apiLocations != null && !apiLocations.isEmpty()) {
            com.google.android.material.button.MaterialButton btnBrowse = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
            btnBrowse.setText("Select Official Ward / Area");
            btnBrowse.setAllCaps(false);
            btnBrowse.setTextColor(ContextCompat.getColor(this, R.color.primary_green));
            btnBrowse.setOnClickListener(v -> {
                SearchableLocationDialog searchDialog = new SearchableLocationDialog(apiLocations, selectedLoc -> {
                    this.currentLocation = selectedLoc;
                    this.detectedAddress = selectedLoc.getDisplayName();
                    this.isManualLocation = true;
                    viewModel.setManualLocation(selectedLoc);
                    if (sessionManager != null) {
                        sessionManager.saveRecentCustomLocation(selectedLoc.getDisplayName());
                    }
                    binding.tvDetectedLocation.setText(detectedAddress);
                    binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                    populateSummary();
                });
                searchDialog.show(getSupportFragmentManager(), "searchable_location");
            });
            layout.addView(btnBrowse);
        }

        if (recentLocation != null && !recentLocation.trim().isEmpty()) {
            com.google.android.material.button.MaterialButton btnRecent = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
            btnRecent.setText("Use Recent: " + recentLocation);
            btnRecent.setAllCaps(false);
            btnRecent.setTextColor(ContextCompat.getColor(this, R.color.primary_green));
            layout.addView(btnRecent);
        }

        android.widget.EditText etLocation = new android.widget.EditText(this);
        etLocation.setHint("e.g. Chilenje, Ward 24");
        if (this.detectedAddress != null && !this.detectedAddress.contains("timed out")) {
            etLocation.setText(this.detectedAddress);
        } else if (recentLocation != null) {
            etLocation.setText(recentLocation);
        }
        layout.addView(etLocation);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_select_location)
                .setView(layout)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String manualName = etLocation.getText().toString().trim();
                    if (!manualName.isEmpty()) {
                        this.detectedAddress = manualName;
                        this.isManualLocation = true;
                        if (sessionManager != null) {
                            sessionManager.saveRecentCustomLocation(manualName);
                        }
                        viewModel.matchAndSetLocation(manualName);
                        binding.tvDetectedLocation.setText(detectedAddress);
                        binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                        populateSummary();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (layout.getChildCount() > 1 && layout.getChildAt(0) instanceof com.google.android.material.button.MaterialButton) {
            layout.getChildAt(0).setOnClickListener(v -> {
                if (recentLocation != null) {
                    etLocation.setText(recentLocation);
                    etLocation.setSelection(recentLocation.length());
                }
            });
        }

        dialog.show();
    }

    private void requestLocationPermissions() {
        if (!isGpsEnabled()) {
            Toast.makeText(this, R.string.error_gps_off, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        binding.pbLocationLoading.setVisibility(View.VISIBLE);
        binding.tvDetectedLocation.setText(R.string.status_searching_gps);
        binding.tvDetectedLocation.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> {
            requestFreshLocation();
        });
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.pbLocationLoading.setVisibility(View.GONE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .setDurationMillis(15000)
                .build();

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateLocationUI(location);
                    } else {
                        binding.pbLocationLoading.setVisibility(View.GONE);
                        Toast.makeText(CreateComplaintActivity.this, R.string.error_location_not_found, Toast.LENGTH_SHORT).show();
                        binding.tvDetectedLocation.setVisibility(View.GONE);
                    }
                }
            };
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());

        // Safety timeout to hide progress bar and notify user if GPS takes too long
        binding.tvDetectedLocation.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || binding == null) return;
            if (binding.pbLocationLoading.getVisibility() == View.VISIBLE) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                binding.pbLocationLoading.setVisibility(View.GONE);
                Toast.makeText(CreateComplaintActivity.this, "GPS taking long? Tap location to select or enter manually.", Toast.LENGTH_LONG).show();
                binding.tvDetectedLocation.setText("GPS slow. Tap to select location manually");
                binding.tvDetectedLocation.setOnClickListener(v -> showManualLocationDialog());
            }
        }, 10000);
    }

    private void updateLocationUI(Location location) {
        this.currentLatitude = location.getLatitude();
        this.currentLongitude = location.getLongitude();
        this.isManualLocation = false;
        
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                runOnUiThread(() -> {
                    if (binding == null) return;
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        String neighborhood = address.getSubLocality();
                        String city = address.getLocality();
                        String province = address.getAdminArea();
                        
                        if (neighborhood != null && !neighborhood.isEmpty() && !neighborhood.contains("+")) {
                            sb.append(neighborhood);
                            if (city != null) sb.append(", ").append(city);
                        } else if (city != null && !city.isEmpty()) {
                            sb.append(city);
                            if (province != null) sb.append(", ").append(province);
                        } else {
                            String line = address.getAddressLine(0);
                            if (line != null && line.contains("+")) {
                                String clean = line.replaceAll("^[A-Z0-9]{4}\\+[A-Z0-9]{2,3}\\s*", "");
                                sb.append(clean.isEmpty() ? "Unknown Location" : clean);
                            } else {
                                sb.append(line != null ? line : "Unknown Location");
                            }
                        }
                        
                        this.detectedAddress = sb.toString();
                        binding.tvDetectedLocation.setText(detectedAddress);
                        binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                        viewModel.mapToApiLocation(address.getLocality(), address.getSubLocality());
                    }
                    binding.pbLocationLoading.setVisibility(View.GONE);
                    populateSummary();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (binding == null) return;
                    String coords = String.format(Locale.getDefault(), "%.5f, %.5f", location.getLatitude(), location.getLongitude());
                    binding.tvDetectedLocation.setText(coords);
                    binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                    binding.pbLocationLoading.setVisibility(View.GONE);
                    populateSummary();
                });
            }
        }).start();
    }

    private void selectCategory(String categoryName) {
        boolean isOther = categoryName.equalsIgnoreCase(getString(R.string.category_other));
        boolean isSensitive = categoryName.toLowerCase().contains("sensitive") || ComplaintViewModel.isSensitiveCategory(categoryName);

        binding.tilCustomCategory.setVisibility(isOther ? View.VISIBLE : View.GONE);
        binding.tilSubCategory.setVisibility(isSensitive ? View.VISIBLE : View.GONE);
        binding.cardConfidentialBanner.setVisibility(isSensitive ? View.VISIBLE : View.GONE);
        binding.switchAnonymous.setVisibility(isSensitive ? View.VISIBLE : View.GONE);

        binding.etTitle.setText(categoryName);

        if (!isSensitive) {
            binding.actvSubCategory.setText("", false);
            binding.switchAnonymous.setChecked(false);
        }
        if (!isOther) {
            binding.etCustomCategory.setText("");
        }
        
        if (binding.llStepDetails.getVisibility() != View.VISIBLE) {
            binding.llStepDetails.setVisibility(View.VISIBLE);
        }

        if (categoryAdapter != null) {
            categoryAdapter.setSelectedCategory(categoryName);
        }

        selectedCategory = null;
        if (availableCategories != null) {
            for (CategoryDto dto : availableCategories) {
                if (dto.getName().equalsIgnoreCase(categoryName) || 
                    dto.getName().toLowerCase().contains(categoryName.toLowerCase())) {
                    selectedCategory = dto;
                    break;
                }
            }
        }
        
        if (selectedCategory == null && !isOther) {
            selectedCategory = new CategoryDto(null, categoryName);
        }
        
        populateSummary();
    }

    private void suggestCategory(String title) {
        String lowerTitle = title.toLowerCase();
        
        String suggested = null;
        if (lowerTitle.contains("water") || lowerTitle.contains("leak") || lowerTitle.contains("pipe") || lowerTitle.contains("burst")) {
            suggested = getString(R.string.category_water);
        } else if (lowerTitle.contains("sanitation") || lowerTitle.contains("sewer") || lowerTitle.contains("drainage") || lowerTitle.contains("waste") || lowerTitle.contains("garbage")) {
            suggested = getString(R.string.category_sanitation);
        } else if (lowerTitle.contains("road") || lowerTitle.contains("street") || lowerTitle.contains("pothole") || lowerTitle.contains("bridge")) {
            suggested = getString(R.string.category_roads);
        } else if (lowerTitle.contains("electric") || lowerTitle.contains("power") || lowerTitle.contains("light") || lowerTitle.contains("cable") || lowerTitle.contains("transformer")) {
            suggested = getString(R.string.category_electricity);
        } else if (lowerTitle.contains("security") || lowerTitle.contains("crime") || lowerTitle.contains("police") || lowerTitle.contains("danger") || lowerTitle.contains("safety")) {
            suggested = getString(R.string.category_security);
        }
        
        if (suggested != null) {
            selectCategory(suggested);
        }
    }

    private void populateSummary() {
        if (binding == null) return;
        
        String title = binding.etTitle.getText().toString().trim();
        binding.tvSummaryTitle.setText(title.isEmpty() ? "---" : title);
        binding.tvSummaryCategory.setText(String.format(getString(R.string.label_category_format), 
                selectedCategory != null ? selectedCategory.getName() : getString(R.string.default_category)));
        
        List<String> photos = viewModel.getSelectedPhotos().getValue();
        int photoCount = photos != null ? photos.size() : 0;
        ComplaintViewModel.VoiceNoteState vnState = viewModel.getVoiceNoteState().getValue();
        boolean hasVoice = vnState != null && vnState.localFile != null;
        
        String mediaText = String.format(Locale.getDefault(), "Evidence: %d Photos • %s", 
                photoCount, hasVoice ? getString(R.string.label_voice_note_present) : getString(R.string.label_voice_note_absent));
        binding.tvSummaryMedia.setText(mediaText);
        
        binding.tvSummaryLocation.setText(String.format(getString(R.string.label_location_format), 
                detectedAddress != null ? detectedAddress : getString(R.string.status_searching_gps)));
    }

    private void showPhotoPreview(String uri) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.shimmer_complaint_detail); // Using a placeholder or creating a simple layout
        
        // Better: create a quick layout for full screen image
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(android.graphics.Color.BLACK);
        
        com.bumptech.glide.Glide.with(this)
                .load(uri)
                .listener(com.voiceyanga.citizen.core.utils.PhotoLogger.listener())
                .into(iv);
        
        dialog.setContentView(iv);
        iv.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showFullReviewDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_report_review_bottom_sheet, null);
        dialog.setContentView(view);
        
        android.widget.TextView tvTitle = view.findViewById(R.id.tvReviewTitle);
        android.widget.TextView tvCategory = view.findViewById(R.id.tvReviewCategory);
        android.widget.TextView tvDescription = view.findViewById(R.id.tvReviewDescription);
        android.widget.TextView tvLocation = view.findViewById(R.id.tvReviewLocation);
        
        tvTitle.setText(binding.etTitle.getText().toString().trim());
        tvCategory.setText(selectedCategory != null ? selectedCategory.getName().toUpperCase() : "GENERAL");
        tvDescription.setText(binding.etDescription.getText().toString().trim());
        tvLocation.setText(detectedAddress != null ? detectedAddress : "No location detected");
        
        // Photos
        List<String> photos = viewModel.getSelectedPhotos().getValue();
        if (photos != null && !photos.isEmpty()) {
            androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvReviewPhotos);
            PhotoAdapter previewAdapter = new PhotoAdapter(null, complaintRepository); // No removal in preview
            previewAdapter.setPhotos(photos);
            rv.setAdapter(previewAdapter);
        } else {
            view.findViewById(R.id.tvMediaLabel).setVisibility(View.GONE);
            view.findViewById(R.id.rvReviewPhotos).setVisibility(View.GONE);
        }

        // Voice Note
        ComplaintViewModel.VoiceNoteState vnState = viewModel.getVoiceNoteState().getValue();
        if (vnState != null && vnState.localFile != null) {
            view.findViewById(R.id.cardReviewVoice).setVisibility(View.VISIBLE);
            android.widget.TextView tvDuration = view.findViewById(R.id.tvReviewVoiceDuration);
            int duration = vnState.durationSeconds;
            tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", duration / 60, duration % 60));
        }
        
        view.findViewById(R.id.btnCloseReview).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        triggerAutoSave();
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);
    }
}
