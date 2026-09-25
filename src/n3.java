import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 新增功能点击监听器：
 * 1=选择图片(分享用) 2=分享图片到好友圈 4=选择视频 5=分享视频到好友圈
 * 7=设置应用图标 8=设置介绍图 9=保存应用名称 10=还原默认配置
 */
public final class n3 implements View.OnClickListener {

    public final int a;
    public final AppCompatActivity b;
    public final View c;

    public n3(AppCompatActivity activity, View root, int action) {
        this.a = action;
        this.b = activity;
        this.c = root;
    }

    @Override
    public void onClick(View view) {
        int i = this.a;
        AppCompatActivity act = this.b;
        View root = this.c;
        switch (i) {
            case 1:
                ShareHelper.pickImage(act, ShareHelper.ACTION_SHARE_IMAGE);
                break;
            case 2:
                ShareHelper.shareImage(act, 2);
                break;
            case 4:
                ShareHelper.pickVideo(act);
                break;
            case 5:
                ShareHelper.shareVideo(act, 2);
                break;
            case 7:
                ShareHelper.pickImage(act, ShareHelper.ACTION_APP_ICON);
                break;
            case 8:
                ShareHelper.pickImage(act, ShareHelper.ACTION_INTRO);
                break;
            case 9:
                EditText et = root != null ? (EditText) root.findViewById(0x7f0801d8 /* set52 */) : null;
                ShareHelper.saveAppName(act, et);
                break;
            case 10:
                ShareHelper.restoreDefaults(act);
                break;
            default:
                break;
        }
    }
}
