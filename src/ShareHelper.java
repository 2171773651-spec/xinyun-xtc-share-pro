import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.xtc.shareapi.share.communication.SendMessageToXTC;
import com.xtc.shareapi.share.manager.ShareMessageManager;
import com.xtc.shareapi.share.shareobject.XTCImageObject;
import com.xtc.shareapi.share.shareobject.XTCShareMessage;
import com.xtc.shareapi.share.shareobject.XTCVideoObject;
import com.xtc.shareapi.share.sharescene.Chat;
import com.xtc.shareapi.share.sharescene.Moment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 芯云小天才分享Pro 新增功能辅助类：
 * 1. 图片页：选择图片并分享
 * 2. 视频页：选择视频并分享
 * 3. 设置页：保存应用名称、设置应用图标、设置介绍图、还原默认配置
 */
public final class ShareHelper {

    public static final int ACTION_NONE = 0;
    public static final int ACTION_SHARE_IMAGE = 1;
    public static final int ACTION_APP_ICON = 2;
    public static final int ACTION_INTRO = 3;
    public static final int ACTION_SHARE_VIDEO = 4;

    public static final int REQ_IMAGE = 2001;   // 与现有背景选择共用请求码（用 pendingAction 区分）
    public static final int REQ_VIDEO = 3002;

    private static final String PREFS = "xtc_share_config";
    private static final String KEY_NAME = "app_name";

    private static int pendingAction = ACTION_NONE;

    private ShareHelper() {
    }

    // ==================== 选择结果路由 ====================
    public static boolean handleActivityResult(Activity act, int req, int res, Intent data) {
        if (pendingAction == ACTION_NONE) {
            return false; // 交给原有背景处理逻辑
        }
        int action = pendingAction;
        pendingAction = ACTION_NONE;
        try {
            if (res != Activity.RESULT_OK || data == null) {
                return true; // 用户取消了选择，吞掉本次结果，避免误改背景
            }
            if (action == ACTION_SHARE_IMAGE && req == REQ_IMAGE) {
                saveImage(act, data, "share");
                toast(act, "图片已选择");
            } else if (action == ACTION_APP_ICON && req == REQ_IMAGE) {
                saveImage(act, data, "icon");
                toast(act, "应用图标已设置");
            } else if (action == ACTION_INTRO && req == REQ_IMAGE) {
                saveImage(act, data, "intro");
                toast(act, "介绍图已设置");
            } else if (action == ACTION_SHARE_VIDEO && req == REQ_VIDEO) {
                saveVideo(act, data);
                toast(act, "视频已选择");
            } else {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    // ==================== 选择器 ====================
    public static void pickImage(Activity act, int action) {
        pendingAction = action;
        try {
            s2.f(act);
        } catch (Throwable t) {
            pendingAction = ACTION_NONE;
        }
    }

    public static void pickVideo(Activity act) {
        pendingAction = ACTION_SHARE_VIDEO;
        try {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.GET_CONTENT");
            intent.setType("video/*");
            intent.putExtra("com.xtc.camera.LEFT_BUTTON_TEXT", "取消");
            intent.putExtra("com.xtc.camera.RIGHT_BUTTON_TEXT", "确定");
            act.startActivityForResult(intent, REQ_VIDEO);
        } catch (Throwable t) {
            pendingAction = ACTION_NONE;
        }
    }

    // ==================== 分享 ====================
    /** scene: 1=微聊(Chat) 2=好友圈(Moment) */
    public static void shareImage(Activity act, int scene) {
        try {
            File f = pick(act, "share");
            if (f == null || !f.exists()) {
                toast(act, "请先选择图片");
                return;
            }
            ShareMessageManager m = new ShareMessageManager(act);
            m.setAppName(appName(act));
            m.setAppIcon(iconBitmap(act));
            XTCImageObject obj = new XTCImageObject(f.getAbsolutePath());
            obj.setDescription(appName(act));
            XTCShareMessage msg = new XTCShareMessage();
            msg.setShareObject(obj);
            msg.setDescription(appName(act));
            Bitmap thumb = scaled(act, f, 200);
            if (thumb != null) {
                msg.setThumbImage(thumb);
            }
            SendMessageToXTC.Request req = new SendMessageToXTC.Request();
            applyScene(req, scene);
            req.setMessage(msg);
            m.sendRequestToXTC(req, "123456");
        } catch (Throwable t) {
            toast(act, "分享失败");
        }
    }

    /** scene: 1=微聊(Chat) 2=好友圈(Moment) */
    public static void shareVideo(Activity act, int scene) {
        try {
            File v = pick(act, "video");
            if (v == null || !v.exists()) {
                toast(act, "请先选择视频");
                return;
            }
            File th = new File(act.getFilesDir(), "video_thumb.jpg");
            ShareMessageManager m = new ShareMessageManager(act);
            m.setAppName(appName(act));
            m.setAppIcon(iconBitmap(act));
            XTCVideoObject obj = new XTCVideoObject();
            obj.setVideoPath(v.getAbsolutePath());
            obj.setThumbnailPath(th.exists() ? th.getAbsolutePath() : v.getAbsolutePath());
            obj.setExtInfo("");
            obj.setStartActivity(act.getClass().getName());
            obj.setVideoDownloadUrl("file://" + v.getAbsolutePath());
            obj.setThumbnailDownloadUrl("file://" + (th.exists() ? th.getAbsolutePath() : v.getAbsolutePath()));
            obj.setVideoKey("");
            obj.setThumbnailKey("");
            obj.setSourceKey("");
            XTCShareMessage msg = new XTCShareMessage();
            msg.setShareObject(obj);
            msg.setDescription(appName(act));
            Bitmap thumb = scaled(act, th.exists() ? th : v, 200);
            if (thumb != null) {
                msg.setThumbImage(thumb);
            }
            SendMessageToXTC.Request req = new SendMessageToXTC.Request();
            applyScene(req, scene);
            req.setMessage(msg);
            m.sendRequestToXTC(req, "123456");
        } catch (Throwable t) {
            toast(act, "分享失败");
        }
    }

    private static void applyScene(SendMessageToXTC.Request req, int scene) {
        if (scene == 1) {
            Chat chat = new Chat();
            chat.setSelectActionMode(2);
            chat.setFriendType(4369);
            req.setScene(chat);
            req.setFlag(1);
            req.setSendMode(2);
        } else {
            req.setScene(new Moment());
            req.setFlag(0);
        }
    }

    // ==================== 设置项 ====================
    public static String appName(Context c) {
        try {
            String s = c.getSharedPreferences(PREFS, 0).getString(KEY_NAME, null);
            if (s != null && s.trim().length() > 0) {
                return s.trim();
            }
        } catch (Throwable t) {
            // ignore
        }
        try {
            return c.getApplicationInfo().loadLabel(c.getPackageManager()).toString();
        } catch (Throwable t) {
            return "芯云小天才分享Pro";
        }
    }

    public static void saveAppName(Activity act, EditText et) {
        try {
            String s = et != null ? et.getText().toString().trim() : "";
            if (s.length() == 0) {
                toast(act, "请输入应用名称");
                return;
            }
            act.getSharedPreferences(PREFS, 0).edit().putString(KEY_NAME, s).apply();
            toast(act, "应用名称已保存");
        } catch (Throwable t) {
            // ignore
        }
    }

    public static Bitmap iconBitmap(Context c) {
        try {
            File f = new File(c.getFilesDir(), "icon.png");
            if (!f.exists()) {
                f = new File(c.getFilesDir(), "icon.jpg");
            }
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) {
                    return b;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return BitmapFactory.decodeResource(c.getResources(), 0x7f07007f /* index */);
    }

    public static Bitmap introBitmap(Context c) {
        try {
            File f = new File(c.getFilesDir(), "intro.png");
            if (!f.exists()) {
                f = new File(c.getFilesDir(), "intro.jpg");
            }
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) {
                    return b;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return BitmapFactory.decodeResource(c.getResources(), 0x7f070063 /* f5corecloud */);
    }

    public static void restoreDefaults(Activity act) {
        try {
            act.getSharedPreferences(PREFS, 0).edit().clear().apply();
            String[] names = {"index.jpg", "index.png", "icon.png", "icon.jpg",
                    "intro.png", "intro.jpg", "share.png", "share.jpg",
                    "video.mp4", "video.avi", "video.mkv", "video.mov", "video_thumb.jpg"};
            for (String n : names) {
                File f = new File(act.getFilesDir(), n);
                if (f.exists()) {
                    f.delete();
                }
            }
            View v = act.findViewById(0x7f0801d8 /* set52 */);
            if (v instanceof EditText) {
                ((EditText) v).setText(appName(act));
            }
            s2.a(act);
            toast(act, "已还原默认配置");
        } catch (Throwable t) {
            // ignore
        }
    }

    // ==================== 内部工具 ====================
    private static File pick(Context c, String base) {
        File f = new File(c.getFilesDir(), base + ".png");
        if (!f.exists()) {
            f = new File(c.getFilesDir(), base + ".jpg");
        }
        if (!f.exists() && "video".equals(base)) {
            String[] exts = {".mp4", ".avi", ".mkv", ".mov"};
            for (String e : exts) {
                f = new File(c.getFilesDir(), "video" + e);
                if (f.exists()) {
                    break;
                }
            }
        }
        return f.exists() ? f : null;
    }

    private static void saveImage(Activity act, Intent data, String base) throws Exception {
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        String ext = ".jpg";
        String mime = act.getContentResolver().getType(uri);
        if (mime != null && mime.toLowerCase().contains("png")) {
            ext = ".png";
        }
        File out = new File(act.getFilesDir(), base + ext);
        InputStream in = act.getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[4096];
        while (true) {
            int n = in.read(buf);
            if (n <= 0) {
                break;
            }
            fos.write(buf, 0, n);
        }
        fos.flush();
        fos.close();
        in.close();
        File other = new File(act.getFilesDir(), base + (".png".equals(ext) ? ".jpg" : ".png"));
        if (other.exists()) {
            other.delete();
        }
    }

    private static void saveVideo(Activity act, Intent data) throws Exception {
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        String ext = ".mp4";
        String mime = act.getContentResolver().getType(uri);
        if (mime != null) {
            String m = mime.toLowerCase();
            if (m.contains("avi")) {
                ext = ".avi";
            } else if (m.contains("mkv")) {
                ext = ".mkv";
            } else if (m.contains("mov") || m.contains("quicktime")) {
                ext = ".mov";
            }
        }
        File out = new File(act.getFilesDir(), "video" + ext);
        InputStream in = act.getContentResolver().openInputStream(uri);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[8192];
        while (true) {
            int n = in.read(buf);
            if (n <= 0) {
                break;
            }
            fos.write(buf, 0, n);
        }
        fos.flush();
        fos.close();
        in.close();
        String[] exts = {".mp4", ".avi", ".mkv", ".mov"};
        for (String e : exts) {
            if (!e.equals(ext)) {
                File o = new File(act.getFilesDir(), "video" + e);
                if (o.exists()) {
                    o.delete();
                }
            }
        }
        File tf = new File(act.getFilesDir(), "video_thumb.jpg");
        if (tf.exists()) {
            tf.delete();
        }
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(act, uri);
            Bitmap bm = mmr.getFrameAtTime();
            if (bm != null) {
                FileOutputStream tfos = new FileOutputStream(tf);
                bm.compress(Bitmap.CompressFormat.JPEG, 85, tfos);
                tfos.flush();
                tfos.close();
            }
            mmr.release();
        } catch (Throwable t) {
            // 缩略图生成失败不影响主流程
        }
    }

    private static Bitmap scaled(Context c, File f, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            int w = o.outWidth;
            int h = o.outHeight;
            while (w / (s * 2) > max && h / (s * 2) > max) {
                s *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void toast(Context c, String s) {
        try {
            Toast.makeText(c, s, 0).show();
        } catch (Throwable t) {
            // ignore
        }
    }
}
