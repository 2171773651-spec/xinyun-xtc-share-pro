import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 新增功能长按监听器（与设置页“分享应用”长按逻辑一致）：
 * 1=长按分享图片到微聊 2=长按分享视频到微聊
 */
public final class n4 implements View.OnLongClickListener {

    public final int a;
    public final AppCompatActivity b;

    public n4(AppCompatActivity activity, int which) {
        this.a = which;
        this.b = activity;
    }

    @Override
    public boolean onLongClick(View view) {
        int i = this.a;
        AppCompatActivity act = this.b;
        if (i == 1) {
            ShareHelper.shareImage(act, 1);
        } else if (i == 2) {
            ShareHelper.shareVideo(act, 1);
        }
        return true;
    }
}
