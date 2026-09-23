package com.litukang.quickcode;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 设置 / 说明页。
 * 当前版本采用「零配置」设计：不需要用户名密码，也不需要保存任何账号信息，
 * 只做跳转。本页仅用于展示各 App 的安装状态，便于排查问题。
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF11131A);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, dp(28), p, dp(28));
        scroll.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(h("设备状态", 22, 0xFFF2F4F8, true));

        addRow(root, "淘宝", "com.taobao.taobao");
        addRow(root, "拼多多", "com.xunmeng.pinduoduo");
        addRow(root, "菜鸟", "com.cainiao.wireless");
        addRow(root, "微信", "com.tencent.mm");
        addRow(root, "支付宝", "com.eg.android.AlipayGphone");
        addRow(root, "极兔速递", "com.jtexpress.jt");

        TextView tip = h("以上为检测结果。若显示「未安装」而实际已安装，"
                + "可能是系统限制，重新安装 App 或重启手机可解决。\n\n"
                + "【跳不过去怎么办】\n"
                + "vivo / OriginOS 对「App 之间互相跳转」管得很严，默认会拦截。\n"
                + "请到：设置 → 应用与权限 → 权限管理，\n"
                + "搜「关联启动」和「后台弹出界面」，把「秒码」都设为允许，\n"
                + "再回到本 App 重试。", 12, 0xFF6B7280, false);
        tip.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(20);
        root.addView(tip, lp);

        setContentView(scroll);
    }

    private void addRow(LinearLayout root, String name, String pkg) {
        boolean ok = JumpHelper.isInstalled(this, pkg);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView n = h(name, 15, 0xFFF2F4F8, false);
        row.addView(n, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView s = h(ok ? "已安装" : "未安装", 15, ok ? 0xFF2A9D5C : 0xFFE02E24, true);
        row.addView(s);

        // 点一下整行复制包名，方便排查
        row.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("pkg", pkg));
                JumpHelper.toast(this, "已复制：" + pkg);
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        root.addView(row, lp);
    }

    private TextView h(String t, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(color);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (bold) {
            v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return v;
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
