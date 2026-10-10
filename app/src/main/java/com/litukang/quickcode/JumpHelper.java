package com.litukang.quickcode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/**
 * 跳转核心 v1.3
 *
 * 老板的硬性要求：
 *   1. 兔喜生活用「微信扫一扫」扫驿站墙上的码，直接进小程序
 *   2. 其他（淘宝/拼多多/菜鸟）直接打开对应 App
 *   3. 任何情况下都不落浏览器、不落应用市场
 *
 * 关键点：
 *   - 微信扫一扫的直达 scheme 是 weixin://dl/scan（社区公认、多年稳定）。
 *   - 用 setPackage 把它变成「显式跳转」，成功率远高于浏览器里的隐式跳转。
 *   - 微信打不开的常见原因是 vivo/OriginOS 的「关联启动」限制，代码无法绕过，
 *     需要用户在系统设置里给「秒码」开「关联启动」+「后台弹出界面」。
 */
public final class JumpHelper {

    private JumpHelper() {
    }

    /** 目标 App 包名（固定不变）。 */
    public static final String PKG_TAOBAO = "com.taobao.taobao";
    public static final String PKG_PINDUODUO = "com.xunmeng.pinduoduo";
    public static final String PKG_CAINIAO = "com.cainiao.wireless";
    public static final String PKG_WECHAT = "com.tencent.mm";
    public static final String PKG_ALIPAY = "com.eg.android.AlipayGphone";

    /** 微信的启动 Activity（用于显式兜底）。 */
    private static final String WECHAT_LAUNCHER = "com.tencent.mm.ui.LauncherUI";

    public static class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    /**
     * 直接拉起某个已安装的 App。
     * 先走官方 getLaunchIntentForPackage；对微信再补一个显式 Component 兜底。
     */
    public static Result launchApp(Context ctx, String pkg, String appName) {
        if (!isInstalled(ctx, pkg)) {
            return new Result(false, appName + " 没装，请先到应用商店安装");
        }

        // 方法1：官方启动 Intent
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return new Result(true, "已打开 " + appName);
            }
        } catch (Exception ignored) {
            // 继续尝试下一个方法
        }

        // 方法2：微信专属显式 Component 兜底
        if (PKG_WECHAT.equals(pkg)) {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setComponent(new ComponentName(PKG_WECHAT, WECHAT_LAUNCHER));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return new Result(true, "已打开微信");
            } catch (Exception ignored) {
                // 继续往下
            }
        }

        return new Result(false,
                appName + " 被系统拦了。请到 设置→应用与权限→权限管理，"
                        + "给「秒码」打开「关联启动」和「后台弹出界面」");
    }

    /**
     * 兔喜生活的核心入口：调起微信扫一扫。
     * 用户扫驿站墙上的小程序码，微信直接进「兔喜生活」小程序。
     */
    public static Result openWechatScan(Context ctx) {
        if (!isInstalled(ctx, PKG_WECHAT)) {
            return new Result(false, "微信没装，请先安装微信");
        }

        // 依次尝试两个扫一扫 scheme，都失败再退回「打开微信」
        String[] schemes = {"weixin://dl/scan", "weixin://scanqrcode"};
        for (String s : schemes) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                i.setPackage(PKG_WECHAT);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return new Result(true, "已打开微信扫一扫，请扫驿站墙上的码");
            } catch (Exception ignored) {
                // 换下一个
            }
        }
        return launchApp(ctx, PKG_WECHAT, "微信");
    }

    /** 判断某个包名是否已安装。 */
    public static boolean isInstalled(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 跳转系统「无障碍」设置页，用于开启秒码一键出码服务。 */
    public static void openAccessibilitySettings(Context ctx) {
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            toast(ctx, "无法打开无障碍设置页，请到系统设置手动开启");
        }
    }

    public static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
}
