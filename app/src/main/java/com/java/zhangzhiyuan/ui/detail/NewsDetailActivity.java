package com.java.zhangzhiyuan.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.Point;
import android.view.Display;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.java.zhangzhiyuan.BuildConfig;
import com.java.zhangzhiyuan.R;
import com.java.zhangzhiyuan.adapter.ImageSliderAdapter;
import com.java.zhangzhiyuan.databinding.ActivityNewsDetailBinding;
import com.java.zhangzhiyuan.db.AppDatabase;
import com.java.zhangzhiyuan.model.FavoriteRecord;
import com.java.zhangzhiyuan.model.HistoryRecord;
import com.java.zhangzhiyuan.model.NewsItem;
import com.java.zhangzhiyuan.model.NoteRecord;
import com.java.zhangzhiyuan.model.Summary;
import com.java.zhangzhiyuan.model.ZhipuRequest;
import com.java.zhangzhiyuan.model.ZhipuResponse;
import com.java.zhangzhiyuan.network.RetrofitClient;
import com.java.zhangzhiyuan.util.JwtTokenGenerator;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class NewsDetailActivity extends AppCompatActivity {

    private static final String TAG = "NewsDetailActivity";
    private static final String API_KEY = BuildConfig.ZHIPU_API_KEY;
    //视图绑定对象，用于访问 activity_news_detail.xml 中的所有UI组件。
    private ActivityNewsDetailBinding binding;

    private ExoPlayer player;
    private String currentVideoUrl;
    private boolean playWhenReady = true;
    private long playbackPosition = 0;

    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final List<String> BLOCKED_IMAGE_DOMAINS = Arrays.asList("n.sinaimg.cn", "imgpai.thepaper.cn", "p1.ifengimg.com", "finance.people.com.cn","ll.anhuinews.com","static.cnbetacdn.com","pic.enorth.com.cn","cssn.cn","static.statickksmg.com","pic.anhuinews.com","kjt.fujian.gov.cn");


    private boolean isFavorited = false;
    private NewsItem currentNewsItem;
    private final Gson gson = new Gson();

    private boolean isNoteAreaVisible = false;

    interface SummaryCallback { void onSuccess(String summary); void onFailure(String error); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        db = AppDatabase.getDatabase(this);

        NewsItem newsItem = (NewsItem) getIntent().getSerializableExtra("news_item");
        if (newsItem != null) {
            this.currentNewsItem = newsItem;
            populateViews(newsItem);
            handleImageGallery(newsItem);
            this.currentVideoUrl = newsItem.getVideo();


//            用于调试，测试播放器能否正常播放
//            if (this.currentVideoUrl != null && !this.currentVideoUrl.isEmpty()) {
//                this.currentVideoUrl = "http://vjs.zencdn.net/v/oceans.mp4";
//            }

            loadSummary(newsItem.getNewsID(), newsItem.getContent());
            recordHistory(newsItem);
            //将这条新闻记录到用户的浏览历史中。
            checkIfFavorited(newsItem.getNewsID());
            setupNoteFeature();
        }
    }
    //处理顶部菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //: 加载menu_news_detail.xml 文件，将其中的菜单项“渲染”到menu对象中。
        getMenuInflater().inflate(R.menu.menu_news_detail, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable.mutate();
                DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.black));
            }
        }
        //显示空心或者实心收藏
        updateFavoriteIcon(menu.findItem(R.id.action_favorite));
        return true;
    }
    //获取被点击菜单项的id
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        //如果是返回按钮
        if (itemId == android.R.id.home) {
            finish();//关闭当前activity
            return true;
        } else if (itemId == R.id.action_note) {
            //显示或隐藏笔记框
            toggleNoteArea();
            return true;
        } else if (itemId == R.id.action_favorite) {
            toggleFavorite(item);
            return true;
        } else if (itemId == R.id.action_share) {
            shareNews();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareNews() {
        if (currentNewsItem == null) return;
        //创建一个用于“发送”数据的Intent。ACTION_SEND是安卓系统内置的一个标准动作。
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String summaryText = binding.newsDetailSummary.getText().toString();
        String shareBody = "【来自SkyNews的分享】\n标题：" + currentNewsItem.getTitle() + "\n\n摘要：" + summaryText;
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享新闻：" + currentNewsItem.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        //启动一个系统选择器
        startActivity(Intent.createChooser(shareIntent, "分享到"));
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //不显示默认的标题
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            //获取返回箭头的图标资源
            final Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_baseline_arrow_back_24);
            if (upArrow != null) {
                upArrow.setColorFilter(ContextCompat.getColor(this, R.color.black), android.graphics.PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }
    }
    //将NewsItem对象中的文本数据填充到对应的UI组件中。
    private void populateViews(NewsItem newsItem) {
        binding.newsDetailTitle.setText(newsItem.getTitle());
        binding.newsDetailSource.setText(newsItem.getPublisher());
        binding.newsDetailTime.setText(newsItem.getPublishTime());

        String originalContent = newsItem.getContent();
        if (originalContent != null && !originalContent.isEmpty()) {
            //处理换行
            String[] paragraphs = originalContent.split("\\r?\\n");
            StringBuilder formattedContent = new StringBuilder();
            // 遍历每一个段落
            for (String paragraph : paragraphs) {
                String trimmedParagraph = paragraph.trim();
                if (!trimmedParagraph.isEmpty()) {
                    if (!trimmedParagraph.startsWith("\u3000\u3000")) {
                        formattedContent.append("\u3000\u3000");
                    }
                    formattedContent.append(trimmedParagraph).append("\n\n");
                }
            }
            binding.newsDetailContent.setText(formattedContent.toString().trim());
        } else {
            binding.newsDetailContent.setText("");
        }
    }


    // 在 NewsDetailActivity.java 文件中

    private void handleImageGallery(NewsItem newsItem) {
        String rawImageUrls = newsItem.getRawImageUrls();
        // 步骤 1: 从原始字符串解析出初始的图片URL列表
        List<String> initialImageUrls = new ArrayList<>();

        if (rawImageUrls != null && !rawImageUrls.trim().isEmpty()) {
            // 去除掉所有的方括号 []
            String processedImageUrls = rawImageUrls.replace("[", "").replace("]", "").trim();

            if (!processedImageUrls.isEmpty()) {
                // 按逗号分割URL
                String[] urlArray = processedImageUrls.split(",");
                for (String url : urlArray) {
                    String trimmedUrl = url.trim();
                    if (!trimmedUrl.isEmpty() && !initialImageUrls.contains(trimmedUrl)) {
                        initialImageUrls.add(trimmedUrl); // 存储有效的URL
                    }
                }
            }
        }

        // 步骤 2: 过滤掉被屏蔽的域名，生成最终干净的URL列表
        final List<String> cleanedImageUrls = new ArrayList<>();
        for (String url : initialImageUrls) {
            boolean isBlocked = false;
            for (String blockedDomain : BLOCKED_IMAGE_DOMAINS) {
                if (url.contains(blockedDomain)) {
                    isBlocked = true;
                    break; // 发现是屏蔽域名，停止检查
                }
            }
            if (!isBlocked) {
                cleanedImageUrls.add(url); // 只有有效URL才添加
            }
        }

        // 如果过滤后列表为空，则隐藏所有图片相关视图并返回
        if (cleanedImageUrls.isEmpty()) {
            binding.imageSliderPager.setVisibility(View.GONE);
            binding.imageSliderIndicatorContainer.setVisibility(View.GONE);
            binding.verticalImageContainer.setVisibility(View.GONE);
            return;
        }

        // 步骤 3: 异步获取所有干净图片的尺寸
        final List<Point> imageDimensions = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger counter = new AtomicInteger(cleanedImageUrls.size());

        for (String url : cleanedImageUrls) { // <-- 使用的是过滤后的 cleanedImageUrls
            Glide.with(this)
                    .asDrawable()
                    .load(url)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            imageDimensions.add(new Point(resource.getIntrinsicWidth(), resource.getIntrinsicHeight()));
                            if (counter.decrementAndGet() == 0) {
                                // 所有图片都处理完毕
                                runOnUiThread(() -> processAndDisplayImages(cleanedImageUrls, imageDimensions));
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (counter.decrementAndGet() == 0) {
                                // 所有图片都处理完毕
                                runOnUiThread(() -> processAndDisplayImages(cleanedImageUrls, imageDimensions));
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // 此方法必须被重写
                        }
                    });
        }
    }

    // vvv--- 在 NewsDetailActivity 中添加这个全新的方法 ---vvv
    private void processAndDisplayImages(List<String> urls, List<Point> dimensions) {
        if (urls.isEmpty() || dimensions.isEmpty()) {
            return; // 如果没有任何一张图片成功加载，则不显示
        }

        // 3. 判断所有图片的宽高比是否一致
        boolean allRatiosAreSimilar = true;
        // 使用第一张图片的宽高比作为基准
        double firstRatio = (double) dimensions.get(0).x / dimensions.get(0).y;

        for (int i = 1; i < dimensions.size(); i++) {
            double currentRatio = (double) dimensions.get(i).x / dimensions.get(i).y;
            // 允许有5%的误差
            if (Math.abs(firstRatio - currentRatio) > 0.05) {
                allRatiosAreSimilar = false;
                break;
            }
        }

        if (allRatiosAreSimilar) {
            // --- 场景一：宽高比一致，使用画廊模式 ---
            binding.verticalImageContainer.setVisibility(View.GONE);
            binding.imageSliderPager.setVisibility(View.VISIBLE);

            // 动态计算画廊高度
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenWidth = size.x;
            // 根据屏幕宽度和图片宽高比，计算画廊应有的高度
            int galleryHeight = (int) (screenWidth / firstRatio);

            ViewGroup.LayoutParams params = binding.imageSliderPager.getLayoutParams();
            params.height = galleryHeight;
            binding.imageSliderPager.setLayoutParams(params);

            // 设置适配器
            ImageSliderAdapter adapter = new ImageSliderAdapter(urls);
            binding.imageSliderPager.setAdapter(adapter);

            // 设置指示器（如果图片多于一张）
            if (urls.size() > 1) {
                binding.imageSliderIndicatorContainer.setVisibility(View.VISIBLE);
                createIndicators(urls.size());
                binding.imageSliderPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        updateIndicatorState(position);
                    }
                });
                updateIndicatorState(0);
            } else {
                binding.imageSliderIndicatorContainer.setVisibility(View.GONE);
            }

        } else {
            // --- 场景二：宽高比不一致，使用垂直平铺模式 ---
            binding.imageSliderPager.setVisibility(View.GONE);
            binding.imageSliderIndicatorContainer.setVisibility(View.GONE);
            binding.verticalImageContainer.setVisibility(View.VISIBLE);
            binding.verticalImageContainer.removeAllViews(); // 清空容器

            for (String url : urls) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 16); // 给图片之间加点间距
                imageView.setLayoutParams(params);

                // 这个属性是关键，它能让ImageView在保持图片原始宽高比的同时，调整自己的边界
                imageView.setAdjustViewBounds(true);

                Glide.with(this).load(url).into(imageView);
                binding.verticalImageContainer.addView(imageView);
            }
        }
    }


    //当用户点进详情页时，这个方法会立即将这条新闻记入浏览历史
    private void recordHistory(NewsItem newsItem) {
        executorService.execute(() -> {
            if (newsItem.getUrl() == null) return; // 如果URL为空，则不记录
            //使用gson库将整个NewsItem对象转换成一个JSON格式的字符串
            String newsItemJson = gson.toJson(newsItem);
            // 使用 URL 作为主键来记录历史
            HistoryRecord record = new HistoryRecord(newsItem.getUrl(), newsItemJson, System.currentTimeMillis());
            db.historyDao().insert(record);
        });
    }

    private void checkIfFavorited(String newsId) {
        executorService.execute(() -> {
            FavoriteRecord record = db.favoriteDao().getFavoriteById(newsId);
            isFavorited = (record != null);
            runOnUiThread(this::invalidateOptionsMenu);
        });
    }

    private void toggleFavorite(MenuItem item) {
        isFavorited = !isFavorited;
        updateFavoriteIcon(item);
        executorService.execute(() -> {
            String newsItemJson = gson.toJson(currentNewsItem);
            if (isFavorited) {
                FavoriteRecord record = new FavoriteRecord(currentNewsItem.getNewsID(), newsItemJson, System.currentTimeMillis());
                db.favoriteDao().insert(record);
            } else {
                FavoriteRecord recordToDelete = new FavoriteRecord();
                recordToDelete.newsId = currentNewsItem.getNewsID();
                db.favoriteDao().delete(recordToDelete);
            }
        });
    }

    private void updateFavoriteIcon(MenuItem item) {
        if (item == null) return;
        if (isFavorited) {
            item.setIcon(R.drawable.ic_baseline_star_24);
        } else {
            item.setIcon(R.drawable.ic_baseline_star_border_24);
        }
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon.mutate();
            DrawableCompat.setTint(icon, ContextCompat.getColor(this, R.color.black));
        }
    }

    private void initializePlayer() {
        if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
            try {
                DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
                player = new ExoPlayer.Builder(this)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build();
                binding.playerView.setPlayer(player);
                binding.playerView.setVisibility(View.VISIBLE);
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(currentVideoUrl));
                player.setMediaItem(mediaItem);
                player.setPlayWhenReady(playWhenReady);
                player.seekTo(playbackPosition);
                player.prepare();
            } catch (Exception e) {
                binding.playerView.setVisibility(View.GONE);
                Toast.makeText(this, "视频播放失败", Toast.LENGTH_LONG).show();
            }
        } else {
            binding.playerView.setVisibility(View.GONE);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            player.release();
            player = null;
            if(binding != null) {
                binding.playerView.setPlayer(null);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
            initializePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player == null) {
            initializePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
            releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
            releasePlayer();
    }

    private void createIndicators(int count) {
        if (binding == null) return;
        binding.imageSliderIndicatorContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView indicator = new ImageView(this);
            indicator.setImageResource(R.drawable.tab_selector);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            indicator.setLayoutParams(params);
            binding.imageSliderIndicatorContainer.addView(indicator);
        }
    }

    private void updateIndicatorState(int position) {
        if (binding == null) return;
        for (int i = 0; i < binding.imageSliderIndicatorContainer.getChildCount(); i++) {
            View child = binding.imageSliderIndicatorContainer.getChildAt(i);
            if (child instanceof ImageView) {
                child.setSelected(i == position);
            }
        }
    }

    private void loadSummary(final String newsId, final String newsContent) {
        if (binding == null) return;
        binding.newsDetailSummary.setText("摘要生成中，请稍候...");
        executorService.execute(() -> {
            if (binding == null) return;
            Summary summary = db.summaryDao().getSummaryById(newsId);
            if (summary != null) {
                if (binding != null) {
                    runOnUiThread(() -> binding.newsDetailSummary.setText(summary.summaryText));
                }
            } else {
                fetchSummaryFromGlmApi(newsContent, new SummaryCallback() {
                    @Override
                    public void onSuccess(String generatedSummary) {
                        if (binding == null) return;
                        String indentedSummary = "\u3000\u3000" + generatedSummary.replace("【GLM模拟摘要】", "").trim();
                        Summary newSummary = new Summary();
                        newSummary.newsId = newsId;
                        newSummary.summaryText = indentedSummary;
                        executorService.execute(() -> db.summaryDao().insert(newSummary));
                        runOnUiThread(() -> binding.newsDetailSummary.setText(indentedSummary));
                    }
                    @Override
                    public void onFailure(String error) {
                        if (binding != null) {
                            runOnUiThread(() -> binding.newsDetailSummary.setText(error));
                        }
                    }
                });
            }
        });
    }

    private void fetchSummaryFromGlmApi(final String content, final SummaryCallback callback) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            callback.onFailure("摘要生成失败：未配置智谱 API Key");
            return;
        }
        String token = JwtTokenGenerator.generateToken(API_KEY, 3600 * 1000);
        //1. 构造ChatMessage，包含角色和我们给AI的指令
        ZhipuRequest.ChatMessage message = new ZhipuRequest.ChatMessage("user", "请为以下新闻内容生成一段100到150字左右的摘要，请直接返回摘要内容，不要包含“好的”、“当然”等多余的词语：\n\n" + content);
        //2. 构造ZhipuRequest，指定模型和对话内容
        ZhipuRequest requestBody = new ZhipuRequest("glm-4", Collections.singletonList(message));
        RetrofitClient.getApiService().getChatSummary("Bearer " + token, requestBody).enqueue(new Callback<ZhipuResponse>() {
            @Override
            public void onResponse(@NonNull Call<ZhipuResponse> call, @NonNull Response<ZhipuResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().choices.isEmpty()) {
                    try {
                        callback.onSuccess(response.body().choices.get(0).message.content);
                    } catch (Exception e) {
                        callback.onFailure("摘要解析失败");
                    }
                } else {
                    callback.onFailure("摘要生成失败，响应码: " + (response.body() != null ? response.code() : "N/A"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<ZhipuResponse> call, @NonNull Throwable t) {
                callback.onFailure("摘要生成失败：网络异常");
            }
        });
    }


    private void setupNoteFeature() {
        executorService.execute(() -> {
            NoteRecord note = db.noteDao().getNoteByNewsId(currentNewsItem.getNewsID());
            if (note != null && note.noteContent != null) {
                runOnUiThread(() -> binding.etNote.setText(note.noteContent));
            }
        });
        binding.btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void toggleNoteArea() {
        isNoteAreaVisible = !isNoteAreaVisible;
        if (isNoteAreaVisible) {
            binding.noteContainer.setVisibility(View.VISIBLE);
            binding.etNote.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.etNote, InputMethodManager.SHOW_IMPLICIT);
        } else {
            binding.noteContainer.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.etNote.getWindowToken(), 0);
        }
    }

    private void saveNote() {
        String content = binding.etNote.getText().toString();
        if (content.trim().isEmpty()) {
            Toast.makeText(this, "笔记内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> {
            NoteRecord record = new NoteRecord();
            record.newsId = currentNewsItem.getNewsID();
            record.newsItemJson = gson.toJson(currentNewsItem);
            record.noteContent = content;
            record.updateTime = System.currentTimeMillis();
            db.noteDao().insert(record);
            runOnUiThread(() -> {
                Toast.makeText(NewsDetailActivity.this, "笔记已保存", Toast.LENGTH_SHORT).show();
                toggleNoteArea();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
