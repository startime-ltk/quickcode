package com.litukang.quickcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 主界面 v1.4
 *
 * 第一按钮 = 兔喜生活（微信扫一扫，扫驿站墙上的码直接进小程序）。
 * 其余按钮 = 直接打开对应 App。
 * v1.4 新增：无障碍「一键出码」入口，自动点「我的」→「身份码/取件码」。
 */
public class MainActivity extends Activity {

    private static final int BG = 0xFF11131A;
    private static final int TEXT_MAIN = 0xFFF2F4F8;
    private static final int TEXT_SUB = 0xFF8B93A7;
    private static final int SECTION = 0xFF6B7280;

    private static final int C_TUXI = 0xFF07C160;
    private static final int C_CAINIAO = 0xFF1677FF;
    private static final int C_TAOBAO = 0xFFFF6A00;
    private static final int C_PDD = 0xFFE02E24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(24), pad, dp(30));
        scroll.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---------- 标题 ----------
        TextView title = new TextView(this);
        title.setText("秒码");
        title.setTextColor(TEXT_MAIN);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("点一下，扫码或打开对应 App");
        sub.setTextColor(TEXT_SUB);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        root.addView(sub, subLp);

        // ---------- vivo 权限提示条 ----------
        root.addView(tipBar("打不开微信的看这里：设置 → 应用与权限 → 权限管理 → 搜「关联启动」和「后台弹出界面」，把「秒码」允许。", dp(18)));

        // ==========================================================
        // 签收码 / 取件码
        // ==========================================================
        root.addView(section("签收码 · 取件码", dp(22)));

        root.addView(bigButton("兔喜生活 · 签收码", "点一下打开微信扫一扫，扫驿站墙上的码直接进小程序", C_TUXI, dp(12),
                v -> doJump(JumpHelper.openWechatScan(this))));

        root.addView(bigButton("菜鸟", "打开菜鸟 App，里面有原生取件码", C_CAINIAO, dp(12),
                v -> doLaunch(JumpHelper.PKG_CAINIAO, "菜鸟")));

        root.addView(bigButton("淘宝", "打开淘宝 App（身份码在「菜鸟驿站」小程序里）", C_TAOBAO, dp(12),
                v -> doLaunch(JumpHelper.PKG_TAOBAO, "淘宝")));

        root.addView(bigButton("拼多多", "打开拼多多 App（身份码在「我的」页）", C_PDD, dp(12),
                v -> doLaunch(JumpHelper.PKG_PINDUODUO, "拼多多")));

        // ==========================================================
        // 一键出码 · 无障碍
        // ==========================================================
        root.addView(section("一键出码 · 无障碍", dp(22)));

        root.addView(bigButton("一键出码", "开启无障碍后自动点「我的」→「身份码/取件码」", 0xFF8B5CF6, dp(12),
                v -> startAccessibilityFlow()));

        // ---------- 设备自检入口 ----------
        TextView check = new TextView(this);
        check.setText("设备自检 · 查看各 App 是否已装  ›");
        check.setTextColor(0xFF9AA3B5);
        check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        check.setGravity(Gravity.CENTER);
        check.setPadding(0, dp(20), 0, dp(20));
        check.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(check, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---------- 底部说明 ----------
        TextView note = new TextView(this);
        note.setText("说明\n"
                + "· 兔喜生活是小程序，微信禁止外部直接进小程序内部，唯一合规入口就是「扫一扫」扫驿站墙上的码。\n"
                + "· 淘宝/拼多多/菜鸟只负责「打开 App」，身份码是登录态动态码、藏在 App 深处且无对外直达入口，打开后还需自己点一下。\n"
                + "· 「一键出码」会自动点开「我的」→「身份码/取件码」，需先在系统设置开启秒码无障碍服务（见设备自检页）。\n"
                + "· 点了完全没反应，优先检查上方 vivo 权限提示。");
        note.setTextColor(0xFF6B7280);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        note.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(10);
        root.addView(note, noteLp);

        setContentView(scroll);
    }

    /** 直接拉起 App：成功轻提示，失败弹窗说清楚原因。 */
    private void doLaunch(String pkg, String name) {
        JumpHelper.Result r = JumpHelper.launchApp(this, pkg, name);
        show(r);
    }

    private void doJump(JumpHelper.Result r) {
        show(r);
    }

    /** 无障碍一键出码入口：检查服务已开启 → 选择目标 App → 设置任务并拉起。 */
    private void startAccessibilityFlow() {
        if (!QuickCodeAccessibilityService.isEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("需要先开启无障碍服务")
                    .setMessage("一键出码依赖系统无障碍服务自动点击。\n\n"
                            + "请点击「去开启」进入系统设置，找到「秒码一键出码」并打开开关，"
                            + "再回到本页重试。")
                    .setPositiveButton("去开启", (d, w) ->
                            JumpHelper.openAccessibilitySettings(this))
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        final String[] names = {"淘宝", "拼多多", "菜鸟"};
        final String[] pkgs = {JumpHelper.PKG_TAOBAO,
                JumpHelper.PKG_PINDUODUO, JumpHelper.PKG_CAINIAO};
        new AlertDialog.Builder(this)
                .setTitle("选择要自动出码的 App")
                .setItems(names, (d, which) -> {
                    QuickCodeAccessibilityService.requestTask(pkgs[which]);
                    doLaunch(pkgs[which], names[which]);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void show(JumpHelper.Result r) {
        if (r.ok) {
            JumpHelper.toast(this, r.message);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("没跳成功")
                    .setMessage(r.message)
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    /** 顶部提示条（深色卡片，浅色文字）。 */
    private TextView tipBar(String text, int topMargin) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xFFF0D58A);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        v.setLineSpacing(dp(2), 1f);
        v.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(0xFF2E2A1C);
        g.setCornerRadius(dp(12));
        g.setStroke(dp(1), 0xFF4A3F1A);
        v.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        v.setLayoutParams(lp);
        return v;
    }

    /** 分组标题。 */
    private TextView section(String text, int topMargin) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(SECTION);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        lp.leftMargin = dp(4);
        v.setLayoutParams(lp);
        return v;
    }

    /** 大号圆角按钮卡片。 */
    private Button bigButton(String text, String subtitle, int color, int topMargin, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text + "\n" + subtitle);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(22), dp(16), dp(22), dp(16));
        b.setLineSpacing(dp(4), 1f);
        b.setBackground(makeRounded(color));
        b.setOnClickListener(l);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        b.setLayoutParams(lp);
        return b;
    }

    private android.graphics.drawable.Drawable makeRounded(int color) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(16));
        return g;
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
