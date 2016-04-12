package com.greenbamboo.prescholleducation.MediaFramework;

import java.io.Serializable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import com.ants360.sports.lib.util.Logger;
import com.ants360.z13.controller.CameraMainController;
import com.ants360.z13.module.Constant.RecordMode;
import com.greenbamboo.prescholleducation.MediaFramework.GMediaFramework.MessageListener;
import com.video.draw.PlayerEGLConfigChooser;
import com.video.draw.PlayerEGLContextFactory;
import com.video.draw.PlayerEGLWindowSurfaceFactory;
import com.video.draw.PlayerRenderer;

public class CameraMediaPlayer implements MessageListener, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static {
        System.loadLibrary("GMediaFramework");
    }

    private static final String TAG = CameraMediaPlayer.class.getSimpleName();

    public static final int AUDIO = 1;
    public static final int VIDEO = 2;
    public static final int BOTHAV = 3;

    public static final int STOPED = 1;
    public static final int PLAYING = 2;

    private GLSurfaceView mGLSurface = null;
    private PlayerRenderer mRenderer = null;
    private Activity mActivity;
    private String mAddress = "";
    private String mUserName = "";
    private String mPassword = "";
    private int mode = VIDEO;
    private int mStatus;

    private static CameraMediaPlayer instance;
    private OnBufferingListener mListener;
    public static final String url = "rtsp://192.168.42.1/live";
    CameraMainController mController;
    boolean init = false;

    private CameraMediaPlayer(Activity activity, CameraMainController mController) {
        super();
        this.mActivity = activity;
        this.mController = mController;
        initialize();
    }

    public static CameraMediaPlayer getInstance(Activity activity, CameraMainController mController) {
        if (instance == null) {
            instance = new CameraMediaPlayer(activity, mController);
        }
        return instance;
    }

    public void setOnBufferingListener(OnBufferingListener listener) {
        this.mListener = listener;
    }

    public void setGLSurface(GLSurfaceView glSurface) {
        this.mGLSurface = glSurface;
        this.mGLSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        this.mGLSurface.setEGLContextClientVersion(2);
        this.mGLSurface.setEGLConfigChooser(new PlayerEGLConfigChooser(8, 8, 8, 8, 0, 0));
        this.mGLSurface.setEGLWindowSurfaceFactory(new PlayerEGLWindowSurfaceFactory());
        this.mGLSurface.setEGLContextFactory(new PlayerEGLContextFactory());
        this.mGLSurface.setEGLContextClientVersion(2);
        this.mGLSurface.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);

        mRenderer = new PlayerRenderer();
        mRenderer.setScale(1.0f, 1.0f, 1.0f);

        mRenderer.setListener(new PlayerRenderer.FrameListener() {

            public void onNewFrame() {
                mGLSurface.requestRender();
            }
        });

        mGLSurface.setRenderer(mRenderer);
        mGLSurface.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGLSurface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        GMediaFramework.sharedInstance().bindRenderer(mRenderer);
    }

    private void initialize() {
        GMediaFramework.sharedInstance().install();
        GMediaFramework.sharedInstance().setMessageListener(this);
        mStatus = STOPED;
    }

    public void destroyRenderer() {
        if (mGLSurface != null) {
            mGLSurface.queueEvent(new Runnable() {

                @Override
                public void run() {
                    if (mRenderer != null) {
                        // TODO Auto-generated method stub
                        mRenderer.destroy();
                        mRenderer = null;
                    }
                }
            });
        }
    }

    public void destroy() {
        instance = null;
        stop();
        GMediaFramework.sharedInstance().unInstall();
        // if (mGLSurface != null) {
        // mGLSurface.queueEvent(new Runnable() {
        //
        // @Override
        // public void run() {
        // if (mRenderer != null) {
        // // TODO Auto-generated method stub
        // mRenderer.destroy();
        // mRenderer = null;
        // }
        // }
        // });
        // }
        destroyRenderer();
    }

    public void play() {

        if (mStatus != PLAYING) {
            Play(url);
        }
    }

    public void Play(String address) {

        boolean changeed = false;
        if (!mAddress.equals(address)) {
            changeed = true;
            mAddress = address;
        }

        switch (mStatus) {
        case STOPED:
            GMediaFramework.sharedInstance().mediaPlay(mAddress, mUserName, mPassword);
            break;
        case PLAYING:
            if (changeed) {
                GMediaFramework.sharedInstance().mediaStop();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        GMediaFramework.sharedInstance().mediaPlay(mAddress, mUserName, mPassword);
                    }
                }, 1000);

            }
            break;

        default:
            break;
        }
        mStatus = PLAYING;
        mRenderer.startRendering();
    }

    public void stop() {
        if(mRenderer!=null && mGLSurface!=null) {
            mRenderer.stopRendering();
            mGLSurface.requestRender();
        }
        if (mStatus == STOPED) {
            return;
        } else {
            GMediaFramework.sharedInstance().mediaStop();
            mStatus = STOPED;
        }
    }

    public void onPause() {
        if (mGLSurface != null) {
            mGLSurface.onPause();
        }

    }

    public void onResume() {
        if (mGLSurface != null) {
            mGLSurface.onResume();
        }
    }

    public void setSurfaceSize(int fWidth, int fHeight) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        ViewGroup.LayoutParams param = (ViewGroup.LayoutParams) mGLSurface.getLayoutParams();
        if (outMetrics.widthPixels < outMetrics.heightPixels) {
            param.width = outMetrics.widthPixels;
            param.height = (int) (outMetrics.widthPixels * fHeight / (double) fWidth);
            // if (mController.isRecording()) {
            // param.height = (int) (outMetrics.widthPixels * fHeight / (double) fWidth);
            // fHeight = param.height;
            // } else {
            // // fWidth = outMetrics.widthPixels;
            // fHeight = (int) Math.floor(fWidth / mController.getCurrentAspactRatio());
            // param.height = fHeight;
            // }
        } else {
            param.height = outMetrics.heightPixels;
            param.width = (int) (outMetrics.heightPixels * fWidth / (double) fHeight);
        }
        // if (mController.isRecording()) {
        fWidth = outMetrics.widthPixels;
        fHeight = (outMetrics.widthPixels * 3) / 4;
        // fHeight = (int) Math.floor(fWidth / mController.getCurrentAspactRatio());
        // } else {
        // fWidth = outMetrics.widthPixels;
        // fHeight = (int) Math.floor(fWidth / mController.getCurrentAspactRatio());
        // }

        // if (mController.isRecording()) {
        // fWidth = outMetrics.widthPixels;
        // fHeight = param.height;
        // } else {
        // fWidth = outMetrics.widthPixels;
        // fHeight = (int) Math.floor(fWidth / mController.getCurrentAspactRatio());
        // }
        Logger.print("debug_surface", "isRecording: " + mController.isRecording() + " fWidth: " + fWidth + " fHeight: "
                + fHeight + " param.height:" + param.height + " param.width:" + param.width);
        mGLSurface.setLayoutParams(param);
        mGLSurface.getHolder().setFixedSize(fWidth, fHeight);
        mGLSurface.postInvalidate();
        if (mListener != null) {
            mListener.onBufferingComplete();
        }
        Log.d(TAG, "setSurfaceSize");
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            int id = msg.what;
            String content = (String) msg.obj;
            Log.d(TAG, "Message: id@" + id + ", content@" + content);

            boolean fIs555 = (((id >> 30) & 1) > 0);
            int f555Value = ((id << 2) >> 2);
            boolean fVideo = (((id >> 24) & 1) > 0);
            boolean fOpen = (((id >> 16) & 1) > 0);
            boolean fSuccess = (((id >> 8) & 1) > 0);
            boolean fFirstFrame = (((id >> 31) & 1) > 0);
            int fWidth = (id << 2) >> 17;
            int fHeight = (id << 17) >> 17;

            if (!fIs555) {
                if (!fFirstFrame) {
                    if (fVideo && fOpen && fSuccess) {
                        switch (mode) {
                        case AUDIO:
                            Log.d(TAG, "just audio!");
                            break;
                        case VIDEO:
                            Log.d(TAG, "just video!");
                            break;
                        case BOTHAV:
                            Log.d(TAG, "both video and audio!");
                            break;
                        default:
                            break;
                        }
                    } else if (fVideo && !fOpen && fSuccess) {
                        Log.e(TAG, "the video can not open!");
                    }
                    Log.d(TAG, (fOpen ? "open" : "close") + " " + (fVideo ? "video" : "audio") + " "
                            + (fSuccess ? "success" : "failed"));
                    if (!fSuccess) {
                        Log.e(TAG, "ERROR >> " + (content != null ? content : "null"));
                        mStatus = STOPED;
                    }
                    Logger.print("debug_surface", "Live frame:  " + fWidth + " * " + fHeight);
                } else /* 第一帧图像 */
                {
                    // Log.d(TAG, "First video frame:  " + fWidth + " * " + fHeight);
                    Logger.print("debug_surface", "First video frame:  " + fWidth + " * " + fHeight);
                    // if (!mController.isRecording()) {
                    // init = false;
                    // setSurfaceSize(fWidth, fHeight);
                    // } else {
                    if (mController == null) {
                        return;
                    }
                    if (mController.getCurrentRecordMode() == null) {
                        return;
                    }
                    if ((mController.getCurrentRecordMode().equals(RecordMode.NORMAL) || mController
                            .getCurrentRecordMode().equals(RecordMode.PHOTO)) && mController.isPreviewEnable() && init) {
                        if (mController.isRecording() && "16:9".equals(mController.getAspactRatio())) {
                            mRenderer.setScale(1.0f, 0.75f, 1.0f);
                            needSetScale = true;
                        } else {
                            if (needSetScale) {
                                needSetScale = false;
                                mRenderer.setScale(1.0f, 1.0f, 1.0f);
                            } else {
                                setSurfaceSize(640, 480);
                            }
                        }
                    } else {
                        init = true;
                        setSurfaceSize(640, 480);
                        if (mController.isRecording() && "16:9".equals(mController.getAspactRatio())) {
                            mRenderer.setScale(1.0f, 0.75f, 1.0f);
                            needSetScale = true;
                        }
                    }
                    // }
                }
            } else /* Live555 Event */
            {
                // Log.e(TAG, "OnLive555 Event, Code@ " + f555Value);

                Logger.print("debug_surface", "OnLive555 Event, Code@ " + f555Value + "frame:  " + fWidth + " * "
                        + fHeight);
                mStatus = STOPED;
            }
        }

    };

    boolean needSetScale = false;

    @Override
    public void onMessage(int id, String content) {
        Message msg = new Message();
        msg.what = id;
        msg.obj = content;
        // mHandler.sendMessageDelayed(msg, 5000);
        mHandler.sendMessage(msg);
    }

    public interface OnBufferingListener {
        void onBufferingComplete();
    }
发生地离开房间爱睡觉分类考试
}
