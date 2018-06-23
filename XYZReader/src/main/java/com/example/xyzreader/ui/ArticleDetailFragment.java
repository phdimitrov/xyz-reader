package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Detail activity used in the {@link ArticleDetailActivity} view pager
 */
public class ArticleDetailFragment extends Fragment implements
        AppBarLayout.OnOffsetChangedListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";

    //animation speed for the collapse/expanded title fading
    //we are using custom container to show title and subtitle for expanded and handling the fading
    private static final int ALPHA_ANIMATION_SPEED = 200;
    private static final float COLLAPSE_RATIO = 0.55f;
    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private FloatingActionButton mShareFab;
    private Toolbar mToolbar;

    //text might be very long (large size) and placing it in single TextView is not efficient
    //using recycler view to show text paragraph by paragraph
    private ParagraphAdapter mParagraphAdapter;
    private RecyclerView mBodyRecyclerView;

    //collapsed and expanded titles are textview we are centering or aligning on keyline
    private TextView mExpandedTitle;
    private TextView mExpandedSubtitle;
    private TextView mCollapsedTitle;
    private ViewGroup mExpandedContainer;
    private AppBarLayout mAppBarLayout;
    private ImageView mPhotoView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mAppBarLayout = mRootView.findViewById(R.id.app_bar);
        mAppBarLayout.addOnOffsetChangedListener(this);

        mExpandedContainer = mAppBarLayout.findViewById(R.id.expanded_container);
        mExpandedTitle = mExpandedContainer.findViewById(R.id.expanded_bar_title);
        mExpandedSubtitle = mExpandedContainer.findViewById(R.id.expanded_bar_subtitle);

        mToolbar = mRootView.findViewById(R.id.scrolling_toolbar);
        mCollapsedTitle = mToolbar.findViewById(R.id.collapsed_bar_title);
        startAlphaAnimation(mCollapsedTitle, 0, View.INVISIBLE);

        mPhotoView = mRootView.findViewById(R.id.photo);

        mShareFab = mRootView.findViewById(R.id.share_fab);
        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity activity = getActivity();
                if (activity != null && mCursor != null) {
                    String title = mCursor.getString(ArticleLoader.Query.TITLE);
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(activity)
                            .setType("text/plain")
                            .setText(title)
                            .getIntent(), getString(R.string.action_share)));
                }
            }
        });

        mBodyRecyclerView = mRootView.findViewById(R.id.recycler_view);

        boolean isShown = getUserVisibleHint();
        if (isShown) {
            //update the activity toolbar with current fragment's toolbar
            getActivityCast().updateToolbar(mToolbar);
        }

        bindViews();
        return mRootView;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        //we are using custom container to show title and subtitle for expanded and handling the fading
        int maxScroll = appBarLayout.getTotalScrollRange();
        float ratio = (float) Math.abs(verticalOffset) / (float) maxScroll;

        handleAlphaOnTitle(ratio);
        handleToolbarTitleVisibility(ratio);
    }

    private void handleToolbarTitleVisibility(float ratio) {
        if (ratio >= COLLAPSE_RATIO) {

            if (!mIsTheTitleVisible) {
                startAlphaAnimation(mCollapsedTitle, ALPHA_ANIMATION_SPEED, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(mCollapsedTitle, ALPHA_ANIMATION_SPEED, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float ratio) {
        if (ratio >= COLLAPSE_RATIO) {
            if (mIsTheTitleContainerVisible) {
                startAlphaAnimation(mExpandedContainer, ALPHA_ANIMATION_SPEED, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(mExpandedContainer, ALPHA_ANIMATION_SPEED, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mParagraphAdapter = new ParagraphAdapter();
        mBodyRecyclerView.setLayoutManager(new LinearLayoutManager(mRootView.getContext(), LinearLayoutManager.VERTICAL, false));
        mBodyRecyclerView.setAdapter(mParagraphAdapter);

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            mShareFab.setVisibility(View.VISIBLE);
            mCollapsedTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            mExpandedTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mExpandedSubtitle.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            } else {
                // If date is before 1902, just show the string
                mExpandedSubtitle.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            ImageLoader loader = ImageLoaderHelper.getInstance(getActivityCast()).getImageLoader();
            loader.get(photoUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {
                        mPhotoView.setImageBitmap(bitmap);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {
                }
            });

            //process html on background as it is very slow for large texts
            String body = mCursor.getString(ArticleLoader.Query.BODY);
            ProgressBar bodyViewLoader = mRootView.findViewById(R.id.article_body_loader);
            new BodyAsyncTask(mBodyRecyclerView, bodyViewLoader).execute(body);

        } else {
            mRootView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //update the activity toolbar with current fragment's toolbar after swipe
        if (isVisibleToUser && getActivityCast() != null) {
            getActivityCast().updateToolbar(mToolbar);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        } else {
            bindViews();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
