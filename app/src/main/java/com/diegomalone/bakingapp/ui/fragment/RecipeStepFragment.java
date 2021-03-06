package com.diegomalone.bakingapp.ui.fragment;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.diegomalone.bakingapp.R;
import com.diegomalone.bakingapp.model.Recipe;
import com.diegomalone.bakingapp.model.Step;
import com.diegomalone.bakingapp.ui.events.PreviousNextClickListener;
import com.diegomalone.bakingapp.utils.ModelUtils;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by malone on 12/02/18.
 */

public class RecipeStepFragment extends Fragment {

    public static final String RECIPE_KEY = "recipeKey";
    public static final String STEP_KEY = "stepKey";
    public static final String STEP_LIST_KEY = "stepListKey";

    public static final String VIDEO_POSITION = "videoPosition";
    public static final String VIDEO_PLAY_STATUS = "videoPlayStatus";

    private Recipe recipe;
    private Step step;
    private ArrayList<Step> stepList = new ArrayList<>();

    private TextView stepTextView;
    private View nextStepContainer, previousStepContainer;
    private ImageView stepImageView;

    private SimpleExoPlayer exoPlayer;
    private SimpleExoPlayerView playerView;
    private boolean playWhenReady = true;
    private Long playerPosition = 0L;

    private PreviousNextClickListener clickCallback;

    public static RecipeStepFragment newInstance(Recipe recipe, Step step) {
        RecipeStepFragment fragment = new RecipeStepFragment();

        ArrayList<Step> stepList = new ArrayList<>();
        stepList.addAll(recipe.getStepList());

        Bundle bundle = new Bundle();
        bundle.putParcelable(RECIPE_KEY, recipe);
        bundle.putParcelable(STEP_KEY, step);
        bundle.putParcelableArrayList(STEP_LIST_KEY, stepList);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            clickCallback = (PreviousNextClickListener) getContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + PreviousNextClickListener.class.getSimpleName());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(view);

        Bundle args = getArguments();

        if (args != null) {
            recipe = args.getParcelable(RECIPE_KEY);
            step = args.getParcelable(STEP_KEY);
            stepList = args.getParcelableArrayList(STEP_LIST_KEY);
        }

        if (savedInstanceState != null) {
            playWhenReady = savedInstanceState.getBoolean(VIDEO_PLAY_STATUS);
            playerPosition = savedInstanceState.getLong(VIDEO_POSITION);
        }

        setTitle();

        initViews(view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (exoPlayer != null) {
            outState.putBoolean(VIDEO_PLAY_STATUS, exoPlayer.getPlayWhenReady());
            outState.putLong(VIDEO_POSITION, exoPlayer.getCurrentPosition());
        }

        outState.putParcelable(RECIPE_KEY, recipe);
        outState.putParcelable(STEP_KEY, step);
        outState.putParcelableArrayList(STEP_LIST_KEY, stepList);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || exoPlayer == null)) {
            initializePlayer();
        }
    }

    private void initializePlayer() {
        if (step.getVideoURL() != null && !step.getVideoURL().isEmpty()) {
            Uri mediaUri = Uri.parse(step.getVideoURL());

            if (exoPlayer == null) {
                // Create an instance of the ExoPlayer.
                TrackSelector trackSelector = new DefaultTrackSelector();
                LoadControl loadControl = new DefaultLoadControl();
                exoPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
                playerView.setPlayer(exoPlayer);

                // Prepare the MediaSource.
                String userAgent = Util.getUserAgent(getContext(), "BakingApp");
                MediaSource mediaSource = new ExtractorMediaSource(mediaUri,
                        new DefaultDataSourceFactory(getContext(), userAgent),
                        new DefaultExtractorsFactory(),
                        null, null);
                exoPlayer.prepare(mediaSource);
                exoPlayer.setPlayWhenReady(playWhenReady);

                if (playerPosition != 0) exoPlayer.seekTo(playerPosition);
            }
        } else {
            playerView.setVisibility(GONE);
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void initViews(View baseView) {
        stepTextView = baseView.findViewById(R.id.stepDescriptionTextView);
        playerView = baseView.findViewById(R.id.videoPlayer);
        nextStepContainer = baseView.findViewById(R.id.nextStepContainer);
        previousStepContainer = baseView.findViewById(R.id.previousStepContainer);
        stepImageView = baseView.findViewById(R.id.stepImage);

        stepTextView.setText(step.getDescription());
        playerView.setDefaultArtwork(BitmapFactory.decodeResource(getResources(), R.drawable.ic_recipe_video_place_holder));

        initializePlayer();

        if (getContext() != null && step.getThumbnailURL() != null && !step.getThumbnailURL().isEmpty()) {
            stepImageView.setVisibility(VISIBLE);

            Glide.with(getContext())
                    .load(step.getThumbnailURL())
                    .load(step.getThumbnailURL())
                    .into(stepImageView);
        } else {
            stepImageView.setVisibility(GONE);
        }

        if (ModelUtils.hasNextStep(stepList, step)) {
            nextStepContainer.setOnClickListener(view ->
                    clickCallback.showStep(ModelUtils.getNextStep(stepList, step))
            );
        } else {
            nextStepContainer.setVisibility(GONE);
        }

        if (ModelUtils.hasPreviousStep(stepList, step)) {
            previousStepContainer.setOnClickListener(view ->
                    clickCallback.showStep(ModelUtils.getPreviousStep(stepList, step))
            );
        } else {
            previousStepContainer.setVisibility(GONE);
        }
    }

    private void setTitle() {
        if (getActivity() != null) {
            getActivity().setTitle(recipe.getName());
        }
    }
}
