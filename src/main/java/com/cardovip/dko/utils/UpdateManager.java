package com.cardovip.dko.utils;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;

import com.cardovip.dko.BuildConfig;
import com.cardovip.dko.R;
import com.cardovip.dko.log.log;
import com.cardovip.dko.model.AppConfig;
import com.hjq.toast.Toaster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.annotation.SuppressLint;

import com.cardovip.dko.event.DownloadEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用更新管理器
 */
public class UpdateManager {
    private Context context;
    private AppConfig appConfig;
    private boolean isDownloading = false;
    private AlertDialog updateDialog;
    private AlertDialog downloadingDialog;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private ExecutorService executorService;

    public UpdateManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        EventBus.getDefault().register(this);
    }

    /**
     * 检查更新
     * @param appConfig 服务器返回的配置信息
     */
    public void checkUpdate(AppConfig appConfig) {
        this.appConfig = appConfig;
        String currentVersion = BuildConfig.VERSION_NAME;

        // 检查是否需要更新
        if (appConfig.needUpdate(currentVersion)) {
            // 检查是否需要强制更新
            boolean forceUpdate = appConfig.needForceUpdate(currentVersion);
            showUpdateDialog(forceUpdate);
        }
    }

    /**
     * 显示更新对话框
     * @param forceUpdate 是否强制更新
     */
    private void showUpdateDialog(boolean forceUpdate) {
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("发现新版本")
                .setMessage(getUpdateMessage())
                .setPositiveButton("立即更新", (dialog, which) -> {
                    startDownload();
                });

        if (!forceUpdate && !appConfig.isForceUpdate()) {
            builder.setNegativeButton("稍后再说", null);
        }

        updateDialog = builder.create();
        updateDialog.setCancelable(!forceUpdate && !appConfig.isForceUpdate());
        updateDialog.show();
    }

    /**
     * 获取更新信息
     */
    private String getUpdateMessage() {
        StringBuilder message = new StringBuilder();
        message.append("当前版本：").append(BuildConfig.VERSION_NAME).append("\n");
        message.append("最新版本：").append(appConfig.getVersion()).append("\n\n");
        message.append("更新内容：\n").append(appConfig.getUpdateDescription());
        return message.toString();
    }

    /**
     * 开始下载
     */
    private void startDownload() {
        if (isDownloading) {
            Toaster.show("正在下载中，请稍候...");
            return;
        }

        if (TextUtils.isEmpty(appConfig.getDownloadUrl())) {
            Toaster.show("下载地址为空");
            return;
        }

        showDownloadingDialog();
        isDownloading = true;

        // 在线程中执行下载
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadApk();
                } catch (Exception e) {
                    log.e("下载失败: " + e.getMessage());
                    EventBus.getDefault().post(new DownloadEvent(DownloadEvent.STATUS_FAILED, e.getMessage(), true));
                }
            }
        });
    }

    /**
     * 执行APK下载
     */
    private void downloadApk() {
        String downloadUrl = appConfig.getDownloadUrl();
        String fileName = "app-update.apk";
        File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File apkFile = new File(downloadDir, fileName);

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.connect();

            int totalLength = conn.getContentLength();
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(apkFile);
            byte[] buffer = new byte[4096];
            int len;
            int downloadedLength = 0;

            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                downloadedLength += len;
                // 发送进度更新事件
                if (totalLength > 0) {
                    int progress = (int) ((downloadedLength * 100L) / totalLength);
                    EventBus.getDefault().post(new DownloadEvent(DownloadEvent.STATUS_PROGRESS, progress));
                }
            }

            fos.close();
            is.close();
            conn.disconnect();

            // 下载完成，发送成功事件
            EventBus.getDefault().post(new DownloadEvent(DownloadEvent.STATUS_SUCCESS, apkFile.getAbsolutePath()));
        } catch (Exception e) {
            log.e("下载失败: " + e.getMessage());
            EventBus.getDefault().post(new DownloadEvent(DownloadEvent.STATUS_FAILED, e.getMessage(), true));
        }
    }

    /**
     * 处理下载事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadEvent(DownloadEvent event) {
        switch (event.getStatus()) {
            case DownloadEvent.STATUS_PROGRESS:
                // 更新进度
                if (progressBar != null && tvProgress != null) {
                    progressBar.setProgress(event.getProgress());
                    tvProgress.setText(String.format("%d%%", event.getProgress()));
                }
                break;
            case DownloadEvent.STATUS_SUCCESS:
                // 下载成功，安装APK
                isDownloading = false;
                dismissDownloadingDialog();
                installApk(Uri.fromFile(new File(event.getFilePath())));
                break;
            case DownloadEvent.STATUS_FAILED:
                // 下载失败
                isDownloading = false;
                dismissDownloadingDialog();
                Toaster.show("下载失败：" + event.getError());
                break;
        }
    }

    /**
     * 显示下载进度对话框
     */
    private void showDownloadingDialog() {
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            return;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null);
        progressBar = view.findViewById(R.id.progress_bar);
        tvProgress = view.findViewById(R.id.tv_progress);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("下载中")
                .setView(view)
                .setCancelable(false);

        if (!appConfig.isForceUpdate()) {
            builder.setNegativeButton("取消", (dialog, which) -> {
                isDownloading = false;
                dismissDownloadingDialog();
            });
        }

        downloadingDialog = builder.create();
        downloadingDialog.show();
    }

    /**
     * 安装APK
     */
    private void installApk(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                new File(Objects.requireNonNull(uri.getPath())));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");

            context.startActivity(intent);
            
            // 如果是强制更新，安装完成后退出应用
            if (appConfig.isForceUpdate()) {
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                System.exit(0);
            }
        } catch (Exception e) {
            log.e("安装APK失败: " + e.getMessage());
            Toaster.show("安装失败，请重试");
        }
    }

    private void dismissUpdateDialog() {
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
    }

    private void dismissDownloadingDialog() {
        if (downloadingDialog != null && downloadingDialog.isShowing()) {
            downloadingDialog.dismiss();
        }
    }

    /**
     * 清理资源
     */
    public void destroy() {
        dismissUpdateDialog();
        dismissDownloadingDialog();
        isDownloading = false;
        EventBus.getDefault().unregister(this);
        executorService.shutdown();
    }
} 